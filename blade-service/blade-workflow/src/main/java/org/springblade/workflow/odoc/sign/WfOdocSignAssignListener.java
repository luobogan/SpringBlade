package org.springblade.workflow.odoc.sign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealCmd;
import org.springblade.workflow.odoc.seal.OdocSealOutcome;
import org.springblade.workflow.odoc.seal.OdocSealSignerCmd;
import org.springblade.workflow.odoc.seal.OdocSealStrategyHolder;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用印「出口监听器」：节点提交后，在签章节点（定稿节点兜底）发起用印。
 *
 * <p>与版式出口监听器（{@code WfOdocLayoutAssignListener}）同范式：引擎推进由
 * {@code WfTaskServiceImpl#doApprove} 驱动，故在提交后置点调用；失败由调用方 catch 告警、不阻断审批主干。</p>
 *
 * <p>触发条件（任一命中）：</p>
 * <ol>
 *   <li>当前节点 == 流程设置「签章节点」({@code signNodeKey} 单选) 或命中 {@code signatureNodeKeys}(CSV 多选)；</li>
 *   <li>未配置签章节点时：当前节点 == 「定稿节点」({@code finalizeNodeKey}) 且 {@code sealRequired=1}（必须签章）。</li>
 * </ol>
 *
 * <p>用印方案由「公文通用设置」决定（自研 local / 第三方 esign，决策 D2 并存）：
 * local 同步盖章并落 {@code wf_odoc_seal}；esign 异步发起签署，落 {@code wf_odoc_sign_flow}
 * 并在回调/补偿完成后归档为新的公文版本。</p>
 *
 * <p>幂等：esign 以 {@code bizNum=odoc-sign-{instId}-{nodeKey}} 为发起幂等键（唯一索引）；
 * local 以「该正文已有成功签章记录」为准。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfOdocSignAssignListener {

	private final IWfOdocDocService docService;
	private final IWfOdocLayoutService layoutService;
	private final WfOdocProcessSetMapper processSetMapper;
	private final WfOdocSignFlowMapper signFlowMapper;
	private final OdocSealStrategyHolder strategyHolder;
	/** 「签完才流转」推进器（记录待推进上下文 / 幂等推进 / 拒签恢复待办） */
	private final OdocSealAdvancer advancer;

	/**
	 * 节点提交后置处理：命中签章/定稿节点则发起用印。
	 *
	 * <p><b>「签完才流转」</b>：esign 方案下发起签署后返回 {@code waiting=true}，
	 * 调用方据此<b>不推进引擎</b>，流程停在签章节点；待签署完成（回调/补偿）后
	 * 由 {@link OdocSealAdvancer} 推进。本方法同时把待推进上下文
	 * （引擎任务ID/提交人/提交变量）落到 {@code wf_odoc_sign_flow}。</p>
	 *
	 * @param operator 当前节点办理人ID（local 模式作为签章人）
	 * @param task     当前业务待办（取引擎任务ID，用于后续推进）
	 * @param vars     本次提交变量（推进时回填引擎，避免变量丢失）
	 * @return 用印触发结果
	 */
	public OdocSealTriggerResult onNodeSubmit(WfInstance inst, WfProcessNode node, Long operator,
											  WfTask task, Map<String, Object> vars) {
		OdocSealTriggerResult result = OdocSealTriggerResult.none();
		if (inst == null || node == null) {
			return result;
		}
		WfOdocProcessSet set = processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, inst.getDefId()));
		if (set == null) {
			return result;
		}
		if (!triggered(node.getNodeKey(), set)) {
			return result;
		}
		result.setTriggered(true);
		WfOdocDoc doc = docService.getDocByInstanceId(inst.getId());
		if (doc == null) {
			log.warn("用印跳过：实例无正文. instId={}", inst.getId());
			return result;
		}
		WfOdocDocVersion cur = docService.getCurrentVersion(doc.getId());
		if (cur == null) {
			log.warn("用印跳过：正文无版本. instId={}, docId={}", inst.getId(), doc.getId());
			return result;
		}
		WfOdocLayoutFile layout = latestLayout(doc.getId());
		if (layout == null) {
			log.warn("用印跳过：无版式文件（需先转 PDF/OFD 版式）. instId={}, docId={}", inst.getId(), doc.getId());
			return result;
		}
		IOdocSealStrategy strategy = strategyHolder.current();
		OdocSealCmd cmd = buildCmd(inst, node, set, doc, cur, layout, operator, strategy);
		if (cmd == null) {
			return result;
		}
		OdocSealOutcome outcome = strategy.seal(cmd);
		log.info("用印发起完成. instId={}, docId={}, mode={}, finished={}, flowId={}",
			inst.getId(), doc.getId(), outcome.getMode(), outcome.isFinished(), outcome.getEsignFlowId());
		result.setEsignFlowId(outcome.getEsignFlowId());
		result.setSignFlowId(outcome.getSignFlowId());
		result.setMessage(outcome.getMessage());
		if (IOdocSealStrategy.MODE_ESIGN.equals(strategy.mode()) && !outcome.isFinished()) {
			// 异步第三方签章：等待签署完成后由回调推进流程
			result.setWaiting(true);
			advancer.saveContext(outcome.getSignFlowId(), node.getNodeKey(), task, operator, vars);
		}
		return result;
	}


	/** 命中判定：签章节点（单选/多选）；未配置签章节点时回落到「定稿节点 + 必须签章」 */
	private boolean triggered(String nodeKey, WfOdocProcessSet set) {
		if (nodeKey == null) {
			return false;
		}
		if (hitNode(nodeKey, set.getSignNodeKey(), set.getSignatureNodeKeys())) {
			return true;
		}
		boolean mustSeal = set.getSealRequired() != null && set.getSealRequired() == 1;
		return mustSeal && set.getFinalizeNodeKey() != null && set.getFinalizeNodeKey().equals(nodeKey);
	}

	/** 组装用印命令；返回 null 表示本次无需发起 */
	private OdocSealCmd buildCmd(WfInstance inst, WfProcessNode node, WfOdocProcessSet set,
							   WfOdocDoc doc, WfOdocDocVersion cur, WfOdocLayoutFile layout,
							   Long operator, IOdocSealStrategy strategy) {
		OdocSealConfigItem cfg = OdocSealConfigItem.parse(set.getSealConfig());
		OdocSealCmd cmd = new OdocSealCmd();
		cmd.setDocId(doc.getId());
		cmd.setInstId(inst.getId());
		cmd.setLayoutFileId(layout.getId());
		cmd.setSrcVersionId(cur.getId());
		cmd.setTitle(doc.getTitle());
		cmd.setBizNum(buildBizNum(inst, node));
		cmd.setSealType(cfg.getSealType());
		cmd.setPage(cfg.getPage());
		cmd.setOffsetX(cfg.getOffsetX());
		cmd.setOffsetY(cfg.getOffsetY());

		if (IOdocSealStrategy.MODE_ESIGN.equals(strategy.mode())) {
			if (esignStarted(doc.getId())) {
				log.info("用印跳过：已有进行中/已完成的签署流程. docId={}", doc.getId());
				return null;
			}
			List<OdocSealSignerCmd> signers = toSignerCmds(cfg);
			if (signers.isEmpty()) {
				log.warn("用印跳过：第三方签章未配置签署方（流程设置 seal_config.signers）. instId={}, defId={}",
					inst.getId(), inst.getDefId());
				return null;
			}
			cmd.getSigners().addAll(signers);
			return cmd;
		}
		// local：签章人 = 当前节点办理人
		cmd.setSignerId(operator);
		return cmd;
	}

	/** 流程设置里的签署方 → 统一用印签署方 */
	private List<OdocSealSignerCmd> toSignerCmds(OdocSealConfigItem cfg) {
		List<OdocSealSignerCmd> list = new ArrayList<>();
		if (cfg.getSigners() == null) {
			return list;
		}
		for (OdocSealConfigItem.Signer s : cfg.getSigners()) {
			if (s == null) {
				continue;
			}
			OdocSealSignerCmd c = new OdocSealSignerCmd();
			c.setSignerType(s.getSignerType() == null ? 0 : s.getSignerType());
			c.setSignOrder(s.getOrder());
			c.setAccountId(s.getAccountId());
			c.setPsnAccount(s.getPsnAccount());
			c.setPsnName(s.getPsnName());
			c.setPsnIdCardNum(s.getPsnIdCardNum());
			c.setOrgName(s.getOrgName());
			c.setOrgIdCardNum(s.getOrgIdCardNum());
			c.setSealId(s.getSealId());
			c.setAutoSign(s.getAutoSign());
			list.add(c);
		}
		return list;
	}

	/** 该正文是否已有（进行中或已完成的）第三方签署流程 */
	private boolean esignStarted(Long docId) {
		return signFlowMapper.selectCount(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getDocId, docId)
			.eq(WfOdocSignFlow::getSealMode, WfOdocSignFlow.SEAL_MODE_ESIGN)
			.in(WfOdocSignFlow::getSignStatus, WfOdocSignFlow.SIGN_STATUS_PENDING,
				WfOdocSignFlow.SIGN_STATUS_RUNNING, WfOdocSignFlow.SIGN_STATUS_FINISHED)) > 0;
	}

	/**
	 * 签署轮次幂等键：{@code odoc-sign-{instId}-{nodeKey}-r{n}}。
	 *
	 * <p>同一轮内重复提交复用同一 bizNum（不重复调用厂商接口）；上一轮已终态
	 * （拒签/过期/失败/撤销）时进入下一轮，允许重新发起签署（历史轮次保留可追溯）。</p>
	 */
	private String buildBizNum(WfInstance inst, WfProcessNode node) {
		String base = "odoc-sign-" + inst.getId() + "-" + node.getNodeKey();
		List<WfOdocSignFlow> list = signFlowMapper.selectList(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getInstId, inst.getId())
			.likeRight(WfOdocSignFlow::getBizNum, base)
			.orderByAsc(WfOdocSignFlow::getId));
		if (list == null || list.isEmpty()) {
			return base + "-r1";
		}
		int maxRound = 0;
		String latestBizNum = null;
		Integer latestStatus = null;
		for (WfOdocSignFlow f : list) {
			maxRound = Math.max(maxRound, parseRound(f.getBizNum(), base));
			latestBizNum = f.getBizNum();
			latestStatus = f.getSignStatus();
		}
		if (maxRound <= 0) {
			return base + "-r1";
		}
		boolean terminal = latestStatus != null && (latestStatus == WfOdocSignFlow.SIGN_STATUS_REJECTED
			|| latestStatus == WfOdocSignFlow.SIGN_STATUS_EXPIRED
			|| latestStatus == WfOdocSignFlow.SIGN_STATUS_FAILED
			|| latestStatus == WfOdocSignFlow.SIGN_STATUS_CANCELED);
		return terminal ? base + "-r" + (maxRound + 1) : latestBizNum;
	}

	/** 解析 bizNum 末尾的轮次号（-rN），解析不到按 0 处理 */
	private int parseRound(String bizNum, String base) {
		if (bizNum == null || bizNum.length() <= base.length()) {
			return 0;
		}
		String suffix = bizNum.substring(base.length());
		if (!suffix.startsWith("-r")) {
			return 0;
		}
		try {
			return Integer.parseInt(suffix.substring(2));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** 取该正文最新的版式文件（PDF/OFD） */
	private WfOdocLayoutFile latestLayout(Long docId) {
		List<WfOdocLayoutFile> list = layoutService.list(docId);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.stream()
			.filter(lf -> lf.getFormat() != null && (lf.getFormat() == 12 || lf.getFormat() == 13))
			.max(Comparator.comparing(WfOdocLayoutFile::getId))
			.orElse(null);
	}

	/** 命中判定：nodeKey 等于单选 key，或包含在 CSV 多值里 */
	private boolean hitNode(String nodeKey, String single, String csv) {
		if (nodeKey == null) {
			return false;
		}
		if (single != null && !single.isBlank() && single.equals(nodeKey)) {
			return true;
		}
		if (csv == null || csv.isBlank()) {
			return false;
		}
		Set<String> keys = Arrays.stream(csv.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
		return keys.contains(nodeKey);
	}

}
