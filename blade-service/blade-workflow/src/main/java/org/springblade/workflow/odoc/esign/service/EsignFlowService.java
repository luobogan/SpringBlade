package org.springblade.workflow.odoc.esign.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.entity.WfOdocSignNotify;
import org.springblade.workflow.odoc.entity.WfOdocSignSigner;
import org.springblade.workflow.odoc.esign.IEsignService;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springblade.workflow.odoc.esign.dto.EsignFlowDetail;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignNotifyMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignSignerMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.sign.OdocSealAdvancer;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * e签宝签署流程编排：回调处理 + 状态同步 + 归档回写。
 *
 * <p>回调链路（官方要求「回调可能丢」→ 必须有补偿）：</p>
 * <ol>
 *   <li>验签（请求头 timestamp + URL query + body，HMAC 与 appSecret）；</li>
 *   <li>幂等落库 wf_odoc_sign_notify（唯一键 esign_flow_id + action + ts，重复投递只处理一次）；</li>
 *   <li>以厂商详情为准同步签署方与整体状态（不依赖单个回调事件的语义）；</li>
 *   <li>全部签署完成 → 取签署完成件归档为新的公文版本，回写 archive_version_id。</li>
 * </ol>
 *
 * <p>补偿任务与回调复用同一套「同步 + 归档」逻辑，保证两条路径结果一致。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsignFlowService {

	private static final DateTimeFormatter SIGN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final IEsignService esignService;
	private final EsignProperties props;
	private final WfOdocSignFlowMapper flowMapper;
	private final WfOdocSignSignerMapper signerMapper;
	private final WfOdocSignNotifyMapper notifyMapper;
	private final IWfOdocDocService docService;
	private final IOdocFileStore fileStore;
	/** 「签完才流转」推进器：签署完成后推进卡在签章节点的流程（幂等） */
	private final OdocSealAdvancer advancer;
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 处理 e签宝回调：验签 → 幂等落库 → 同步状态 → 归档回写。
	 *
	 * @param timestamp 请求头时间戳
	 * @param query     回调 URL 的 query 串（官方口径：按 key 排序后拼接 value）
	 * @param rawQuery  回调 URL 原始 query 串（验签兜底：多参数拼接口径存在歧义时再试一次）
	 * @param body      原始报文（验签依赖原文，不可被任何过滤器改写）
	 * @param signature 请求头签名
	 * @return true=已处理（含重复投递）；false=验签失败
	 */
	public boolean handleCallback(String timestamp, String query, String rawQuery, String body, String signature) {
		boolean verified = esignService.checkCallback(timestamp, query, body, signature);
		if (!verified && rawQuery != null && !rawQuery.equals(query)) {
			// 兜底：部分环境下 query 拼接口径不一致，用原始 query 再验一次
			verified = esignService.checkCallback(timestamp, rawQuery, body, signature);
		}
		JsonNode node = parse(body);
		String action = text(node, "action");
		String esignFlowId = text(node, "flowId");
		String ts = text(node, "timestamp");
		Integer signResult = intOf(text(node, "signResult"));

		if (!verified) {
			log.warn("[esign] 回调验签失败 flowId={} action={}", esignFlowId, action);
			saveNotify(esignFlowId, action, signResult, body, signature, ts, false, "验签失败");
			return false;
		}
		if (saveNotify(esignFlowId, action, signResult, body, signature, ts, true, null)) {
			log.info("[esign] 回调重复投递已忽略 flowId={} action={} ts={}", esignFlowId, action, ts);
			return true;
		}
		WfOdocSignFlow flow = findByEsignFlowId(esignFlowId);
		if (flow == null) {
			log.warn("[esign] 回调未匹配到本地签署流程 flowId={}", esignFlowId);
			markNotify(esignFlowId, action, ts, false, "未匹配到本地签署流程");
			return true;
		}
		flow.setNotifyCount(nz(flow.getNotifyCount()) + 1);
		flow.setLastNotifyTime(LocalDateTime.now());
		if (signResult != null) {
			flow.setSignResult(signResult);
		}
		flowMapper.updateById(flow);
		try {
			syncAndArchive(flow);
			markNotify(esignFlowId, action, ts, true, "已处理");
		} catch (Exception e) {
			log.warn("[esign] 回调处理异常 flowId={}", esignFlowId, e);
			markNotify(esignFlowId, action, ts, false, "处理异常：" + e.getMessage());
		}
		return true;
	}

	// ==================== 补偿 ====================

	/**
	 * 补偿扫描：把「进行中」的签署流程与厂商对齐（官方明确要求：回调可能丢，必须有补偿）。
	 *
	 * @return 本轮处理的流程数
	 */
	public int compensate(int limit) {
		List<WfOdocSignFlow> list = flowMapper.selectList(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getSignStatus, WfOdocSignFlow.SIGN_STATUS_RUNNING)
			.orderByAsc(WfOdocSignFlow::getId)
			.last("limit " + Math.max(1, limit)));
		int maxHours = props.getCompensate() == null ? 72 : props.getCompensate().getMaxHours();
		int count = 0;
		for (WfOdocSignFlow flow : list) {
			if (expired(flow, maxHours)) {
				markExpired(flow);
				continue;
			}
			try {
				syncAndArchive(flow);
				count++;
			} catch (Exception e) {
				log.warn("[esign] 补偿同步失败 flowId={}", flow.getFlowId(), e);
			}
		}
		return count + compensateAdvance(limit);
	}

	/**
	 * 补偿「已签署但流程未推进」的记录。
	 *
	 * <p>场景：回调里推进失败（引擎异常/服务重启），签署件已归档但流程仍停在签章节点。
	 * 这里按 {@code advance_status=待推进} 扫描并重试推进，保证最终一致。</p>
	 */
	public int compensateAdvance(int limit) {
		List<WfOdocSignFlow> list = flowMapper.selectList(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getSignStatus, WfOdocSignFlow.SIGN_STATUS_FINISHED)
			.and(w -> w.eq(WfOdocSignFlow::getAdvanceStatus, WfOdocSignFlow.ADVANCE_STATUS_PENDING)
				.or().isNull(WfOdocSignFlow::getAdvanceStatus))
			.orderByAsc(WfOdocSignFlow::getId)
			.last("limit " + Math.max(1, limit)));
		int count = 0;
		for (WfOdocSignFlow flow : list) {
			try {
				if (advancer.advance(flow.getId())) {
					count++;
				}
			} catch (Exception e) {
				log.warn("[esign] 补偿推进失败 flowId={}", flow.getFlowId(), e);
			}
		}
		return count;
	}

	/** 超过补偿上限（默认 72 小时）仍未完成：置为已过期并告警，不再无限补偿 */
	private boolean expired(WfOdocSignFlow flow, int maxHours) {
		if (maxHours <= 0 || flow.getCreateTime() == null) {
			return false;
		}
		long ageHours = (System.currentTimeMillis() - flow.getCreateTime().getTime()) / 3600000L;
		return ageHours > maxHours;
	}

	private void markExpired(WfOdocSignFlow flow) {
		flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_EXPIRED);
		flow.setErrorMsg("超过补偿时限（" + (props.getCompensate() == null ? 72 : props.getCompensate().getMaxHours())
			+ " 小时）仍未签署完成，已停止补偿");
		flowMapper.updateById(flow);
		// 超时终止推进：把业务待办恢复为待办，办理人可重新提交（进入新一轮签署）
		advancer.terminate(flow.getId(), "签署超时未完成，流程未流转");
		log.warn("[esign] 签署流程超时未完成，停止补偿 flowId={} docId={}", flow.getFlowId(), flow.getDocId());
	}

	/**
	 * 查厂商详情同步签署方与整体状态；全部签完则取签署件归档回写。
	 */
	public void syncAndArchive(WfOdocSignFlow flow) {
		EsignFlowDetail detail = esignService.getFlowDetail(flow.getFlowId());
		if (detail == null) {
			flow.setLastQueryTime(LocalDateTime.now());
			flowMapper.updateById(flow);
			return;
		}
		syncSigners(flow, detail);
		flow.setLastQueryTime(LocalDateTime.now());
		if (detail.isFinished()) {
			flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_FINISHED);
			flowMapper.updateById(flow);
			archive(flow);
			// 「签完才流转」：签署完成 → 推进卡在签章节点的流程（幂等；失败由补偿任务重试）
			advancer.advance(flow.getId());
		} else if (isRejected(flow)) {
			// 拒签：终止推进并把业务待办恢复为待办，办理人可重新提交（进入新一轮签署）
			flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_REJECTED);
			flow.setErrorMsg("签署方拒签，流程未流转");
			flowMapper.updateById(flow);
			advancer.terminate(flow.getId(), "签署方拒签，流程未流转");
			log.warn("[esign] 签署被拒签，流程保持停在签章节点. flowId={} docId={}", flow.getFlowId(), flow.getDocId());
		} else {
			flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_RUNNING);
			flowMapper.updateById(flow);
		}
	}

	/**
	 * 是否拒签（e签宝 signResult 口径：0=未签署、1=拒签、2=已签署）。
	 *
	 * <p>仅在厂商详情未标记「全部签完」且某个签署方拒签时成立。</p>
	 */
	private boolean isRejected(WfOdocSignFlow flow) {
		Integer sr = flow.getSignResult();
		if (sr != null && sr == 1) {
			return true;
		}
		return signerMapper.selectCount(Wrappers.<WfOdocSignSigner>lambdaQuery()
			.eq(WfOdocSignSigner::getFlowId, flow.getId())
			.eq(WfOdocSignSigner::getSignStatus, WfOdocSignSigner.SIGN_STATUS_REJECTED)) > 0;
	}

	/** 同步签署方状态（按账号ID，其次按名称匹配） */
	private void syncSigners(WfOdocSignFlow flow, EsignFlowDetail detail) {
		if (detail.getSigners() == null || detail.getSigners().isEmpty()) {
			return;
		}
		List<WfOdocSignSigner> signers = signerMapper.selectList(Wrappers.<WfOdocSignSigner>lambdaQuery()
			.eq(WfOdocSignSigner::getFlowId, flow.getId()));
		for (WfOdocSignSigner signer : signers) {
			for (EsignFlowDetail.SignerStatus status : detail.getSigners()) {
				if (!match(signer, status)) {
					continue;
				}
				if (status.isSigned()) {
					signer.setSignStatus(WfOdocSignSigner.SIGN_STATUS_SIGNED);
					if (signer.getSignTime() == null) {
						signer.setSignTime(parseSignTime(status.getSignTime()));
					}
				} else if (status.getSignResult() != null && status.getSignResult() == 1) {
					// signResult=1：拒签
					signer.setSignStatus(WfOdocSignSigner.SIGN_STATUS_REJECTED);
				}
				signerMapper.updateById(signer);
			}
		}
	}

	private boolean match(WfOdocSignSigner signer, EsignFlowDetail.SignerStatus status) {
		if (status.getAccountId() != null && status.getAccountId().equals(signer.getAccountId())) {
			return true;
		}
		return status.getAccountName() != null && signer.getAccountName() != null
			&& status.getAccountName().equals(signer.getAccountName());
	}

	// ==================== 归档 ====================

	/**
	 * 取签署完成件归档为新的公文版本，并回写 archive_version_id（幂等：已归档直接返回）。
	 */
	public void archive(WfOdocSignFlow flow) {
		if (flow.getArchiveVersionId() != null) {
			return;
		}
		byte[] bytes = esignService.downloadSignedFile(flow.getFlowId());
		boolean ofd = props.getArchive() != null && "ofd".equalsIgnoreCase(props.getArchive().getFormat());
		String fileName = "signed-" + flow.getFlowId() + (ofd ? ".ofd" : ".pdf");
		Long fileId = fileStore.store(bytes, fileName);
		Integer docFileType = ofd ? 13 : 12;
		WfOdocDocVersion version = docService.saveVersion(
			flow.getDocId(), fileId, fileName, docFileType, "电子签章归档件");
		flow.setArchiveVersionId(version.getId());
		flowMapper.updateById(flow);
		log.info("[esign] 签署完成件已归档 docId={} flowId={} versionId={}",
			flow.getDocId(), flow.getFlowId(), version.getId());
	}

	// ==================== 回调记录 ====================

	/**
	 * 落回调记录（幂等：唯一键 esign_flow_id + action + ts）。
	 *
	 * <p>幂等语义分两档：</p>
	 * <ul>
	 *   <li>已处理（processed=1）→ 判定为厂商重复投递，直接忽略；</li>
	 *   <li>上次处理失败（processed=2，如验签失败、引擎异常）→ 允许同 ts 重试：
	 *       刷新报文与签名后继续处理，保证「先失败后重投」不会被误判为重复。</li>
	 * </ul>
	 *
	 * @return true=重复投递且已处理过（应忽略）；false=本次需要处理
	 */
	private boolean saveNotify(String esignFlowId, String action, Integer signResult, String body,
							   String signature, String ts, boolean processed, String message) {
		WfOdocSignNotify notify = new WfOdocSignNotify();
		notify.setEsignFlowId(esignFlowId);
		notify.setAction(action);
		notify.setSignResult(signResult);
		notify.setBody(body);
		notify.setSig(signature);
		notify.setTs(ts);
		notify.setProcessed(processed ? WfOdocSignNotify.PROCESSED_DONE : WfOdocSignNotify.PROCESSED_FAILED);
		notify.setProcessMsg(message);
		notify.setReceiveTime(LocalDateTime.now());
		if (processed) {
			notify.setProcessTime(LocalDateTime.now());
		}
		try {
			notifyMapper.insert(notify);
			return false;
		} catch (DuplicateKeyException e) {
			WfOdocSignNotify exist = notifyMapper.selectOne(Wrappers.<WfOdocSignNotify>lambdaQuery()
				.eq(WfOdocSignNotify::getEsignFlowId, esignFlowId)
				.eq(WfOdocSignNotify::getAction, action)
				.eq(WfOdocSignNotify::getTs, ts)
				.last("limit 1"));
			if (exist == null) {
				// 极小概率：并发插入后记录已回滚 → 视为重复，避免重复处理
				return true;
			}
			Integer state = exist.getProcessed();
			if (state != null && state == WfOdocSignNotify.PROCESSED_FAILED) {
				// 上一次处理失败 → 允许重试：刷新报文/签名，置待处理，由后续 markNotify 落终态
				exist.setBody(body);
				exist.setSig(signature);
				exist.setSignResult(signResult);
				exist.setProcessMsg(message == null ? "重试投递" : message);
				exist.setReceiveTime(LocalDateTime.now());
				exist.setProcessed(processed ? WfOdocSignNotify.PROCESSED_PENDING : WfOdocSignNotify.PROCESSED_FAILED);
				notifyMapper.updateById(exist);
				log.info("[esign] 回调上次处理失败，允许重试 flowId={} action={} ts={}", esignFlowId, action, ts);
				return false;
			}
			return true;
		}
	}

	/** 更新回调记录的处理结果 */
	private void markNotify(String esignFlowId, String action, String ts, boolean ok, String message) {
		WfOdocSignNotify notify = notifyMapper.selectOne(Wrappers.<WfOdocSignNotify>lambdaQuery()
			.eq(WfOdocSignNotify::getEsignFlowId, esignFlowId)
			.eq(WfOdocSignNotify::getAction, action)
			.eq(WfOdocSignNotify::getTs, ts)
			.last("limit 1"));
		if (notify == null) {
			return;
		}
		notify.setProcessed(ok ? WfOdocSignNotify.PROCESSED_DONE : WfOdocSignNotify.PROCESSED_FAILED);
		notify.setProcessMsg(message);
		notify.setProcessTime(LocalDateTime.now());
		notifyMapper.updateById(notify);
	}

	// ==================== 内部方法 ====================

	private WfOdocSignFlow findByEsignFlowId(String esignFlowId) {
		if (esignFlowId == null || esignFlowId.isBlank()) {
			return null;
		}
		return flowMapper.selectOne(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getFlowId, esignFlowId)
			.last("limit 1"));
	}

	private JsonNode parse(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(body);
		} catch (Exception e) {
			log.warn("[esign] 回调报文解析失败：{}", e.getMessage());
			return null;
		}
	}

	private String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}

	private Integer intOf(String v) {
		if (v == null || v.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(v.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private LocalDateTime parseSignTime(String v) {
		if (v == null || v.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(v.trim(), SIGN_TIME);
		} catch (Exception e) {
			return null;
		}
	}

	private int nz(Integer v) {
		return v == null ? 0 : v;
	}

}
