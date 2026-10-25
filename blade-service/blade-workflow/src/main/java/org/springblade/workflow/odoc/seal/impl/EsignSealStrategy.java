package org.springblade.workflow.odoc.seal.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.entity.WfOdocSignSigner;
import org.springblade.workflow.odoc.esign.IEsignService;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springblade.workflow.odoc.esign.dto.EsignCreateFlowCmd;
import org.springblade.workflow.odoc.esign.dto.EsignFlowCreateResult;
import org.springblade.workflow.odoc.esign.dto.EsignSeal;
import org.springblade.workflow.odoc.mapper.WfOdocDocVersionMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignSignerMapper;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealCmd;
import org.springblade.workflow.odoc.seal.OdocSealConfig;
import org.springblade.workflow.odoc.seal.OdocSealOption;
import org.springblade.workflow.odoc.seal.OdocSealOutcome;
import org.springblade.workflow.odoc.seal.OdocSealSignerCmd;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * e签宝用印策略（esign 模式）。
 *
 * <p>流程：取待签文件 → 三步走上传（内部含就绪轮询）→ 创建签署流程 →
 * 落 {@code wf_odoc_sign_flow} / {@code wf_odoc_sign_signer} → 发起签署。</p>
 *
 * <p>与 local 模式的差异：<b>异步</b>。发起后立即返回（{@code finished=false}），
 * 签章件在全部签署方完成后由回调（{@code POST /odoc/esign/callback}）或补偿任务
 * 取回归档并回写 {@code wf_odoc_sign_flow.archive_version_id}。</p>
 *
 * <p>幂等：{@code bizNum} 唯一索引，重复发起同一 bizNum 时直接复用既有签署流程。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsignSealStrategy implements IOdocSealStrategy {

	/** 毫米 → 像素（96dpi） */
	private static final double MM_PER_INCH = 25.4;
	private static final double DPI = 96.0;

	private final IEsignService esignService;
	private final EsignProperties props;
	private final IOdocFileStore fileStore;
	private final IWfOdocLayoutService layoutService;
	private final WfOdocDocVersionMapper versionMapper;
	private final WfOdocSignFlowMapper flowMapper;
	private final WfOdocSignSignerMapper signerMapper;
	private final OdocSealConfig sealConfig;
	private final IWfOdocSettingService settingService;

	@Override
	public String mode() {
		return MODE_ESIGN;
	}

	@Override
	public boolean available() {
		return props.isEnabled()
			&& StringUtils.hasText(props.getAppId())
			&& StringUtils.hasText(props.getAppSecret());
	}

	@Override
	public List<OdocSealOption> listSeals() {
		List<OdocSealOption> list = new ArrayList<>();
		if (!available()) {
			// 未就绪（未启用或缺 app-id/app-secret）：降级为空列表，页面显示「未就绪」而不是 500
			log.warn("[esign] 印章列表不可用：e签宝未就绪（enabled={}, appId={}, appSecret={}）",
				props.isEnabled(), StringUtils.hasText(props.getAppId()), StringUtils.hasText(props.getAppSecret()));
			return list;
		}
		String orgId = sealConfig.esignOrgId(currentSetting());
		for (EsignSeal seal : esignService.listOrgOwnSeals(orgId)) {
			list.add(option(seal));
		}
		for (EsignSeal seal : esignService.listOrgAuthorizedSeals(orgId)) {
			list.add(option(seal));
		}
		return list;
	}
	// ==================== 用印 ====================

	@Override
	public OdocSealOutcome seal(OdocSealCmd cmd) {
		if (!props.isEnabled()) {
			throw new ServiceException("电子签章未启用（odoc.esign.enabled=false）");
		}
		if (!StringUtils.hasText(props.getAppId()) || !StringUtils.hasText(props.getAppSecret())) {
			// 未就绪：抛业务异常 → 提交链路的用印环节被 try/catch 吞掉，流程照常推进（优雅降级）
			throw new ServiceException("第三方电子签章未就绪（缺少 odoc.esign.app-id / app-secret）");
		}
		if (cmd.getDocId() == null) {
			throw new ServiceException("用印缺少公文正文ID");
		}
		if (cmd.getInstId() == null) {
			throw new ServiceException("用印缺少流程实例ID");
		}
		List<OdocSealSignerCmd> signers = cmd.getSigners();
		if (signers == null || signers.isEmpty()) {
			throw new ServiceException("用印缺少签署方");
		}

		// 幂等：同一 bizNum 已发起过则复用，避免重复调用厂商接口
		if (StringUtils.hasText(cmd.getBizNum())) {
			WfOdocSignFlow exist = flowMapper.selectOne(Wrappers.<WfOdocSignFlow>lambdaQuery()
				.eq(WfOdocSignFlow::getBizNum, cmd.getBizNum())
				.last("limit 1"));
			if (exist != null) {
				log.info("[odoc-seal] 复用已发起的签署流程 bizNum={} flowId={}", cmd.getBizNum(), exist.getFlowId());
				return pendingOutcome(exist);
			}
		}

		// ① 取待签文件
		byte[] content;
		String fileName;
		if (cmd.getLayoutFileId() != null) {
			content = layoutService.content(cmd.getLayoutFileId());
			fileName = layoutService.contentName(cmd.getLayoutFileId());
		} else if (cmd.getSrcVersionId() != null) {
			WfOdocDocVersion version = versionMapper.selectById(cmd.getSrcVersionId());
			if (version == null) {
				throw new ServiceException("正文版本不存在: " + cmd.getSrcVersionId());
			}
			content = fileStore.download(version.getFileId());
			fileName = version.getFileName();
		} else {
			throw new ServiceException("用印缺少待签文件（版式文件或正文版本）");
		}

		// ② 上传（内部含就绪轮询）
		String fileId = esignService.uploadFile(content, fileName);

		// ③ 创建签署流程
		EsignFlowCreateResult result = esignService.createSignFlow(buildFlowCmd(cmd, fileId, fileName));

		// ④ 落库
		WfOdocSignFlow flow = new WfOdocSignFlow();
		flow.setInstId(cmd.getInstId());
		flow.setDocId(cmd.getDocId());
		flow.setSrcVersionId(cmd.getSrcVersionId());
		flow.setEsignFileId(fileId);
		flow.setFlowId(result.getFlowId());
		flow.setBizNum(cmd.getBizNum());
		flow.setTitle(cmd.getTitle());
		flow.setSealMode(WfOdocSignFlow.SEAL_MODE_ESIGN);
		flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_PENDING);
		flow.setNotifyCount(0);
		flowMapper.insert(flow);

		for (int i = 0; i < signers.size(); i++) {
			OdocSealSignerCmd c = signers.get(i);
			WfOdocSignSigner signer = new WfOdocSignSigner();
			signer.setFlowId(flow.getId());
			signer.setSignerType(c.getSignerType());
			signer.setSignOrder(c.getSignOrder() == null ? i + 1 : c.getSignOrder());
			signer.setAccountId(c.getAccountId());
			signer.setAccountName(c.getAccountName() != null ? c.getAccountName() : c.getOrgName());
			signer.setPsnAccount(c.getPsnAccount());
			signer.setOrgIdCard(c.getOrgIdCardNum());
			signer.setAutoSign(Boolean.TRUE.equals(c.getAutoSign()) ? 1 : 0);
			signer.setSealId(resolveSealId(cmd, c));
			signer.setSignUrl(signUrlOf(result, i));
			signer.setSignStatus(WfOdocSignSigner.SIGN_STATUS_PENDING);
			signerMapper.insert(signer);
		}

		// ⑤ 发起签署
		esignService.startSignFlow(result.getFlowId());
		flow.setSignStatus(WfOdocSignFlow.SIGN_STATUS_RUNNING);
		flowMapper.updateById(flow);
		log.info("[odoc-seal] e签宝签署流程已发起 docId={} instId={} flowId={} fileId={}",
			cmd.getDocId(), cmd.getInstId(), result.getFlowId(), fileId);

		OdocSealOutcome outcome = new OdocSealOutcome();
		outcome.setMode(MODE_ESIGN);
		outcome.setSuccess(true);
		outcome.setFinished(false);
		outcome.setEsignFlowId(result.getFlowId());
		outcome.setSignFlowId(flow.getId());
		outcome.setSignUrl(signUrlOf(result, 0));
		outcome.setMessage("签署流程已发起，等待签署完成");
		return outcome;
	}

	// ==================== 内部方法 ====================

	/** 组装 e签宝创建签署流程入参 */
	private EsignCreateFlowCmd buildFlowCmd(OdocSealCmd cmd, String fileId, String fileName) {
		EsignCreateFlowCmd flowCmd = new EsignCreateFlowCmd();
		flowCmd.setDocId(cmd.getDocId());
		flowCmd.setSrcVersionId(cmd.getSrcVersionId());
		flowCmd.setTitle(cmd.getTitle() == null ? "" : cmd.getTitle());
		flowCmd.setBizNum(cmd.getBizNum());
		flowCmd.setSealMode(WfOdocSignFlow.SEAL_MODE_ESIGN);
		flowCmd.setChargeMode(props.getSign().getChargeMode());
		flowCmd.setNoticeTypes(props.getSign().getNoticeTypes());

		EsignCreateFlowCmd.Doc doc = new EsignCreateFlowCmd.Doc();
		doc.setFileId(fileId);
		doc.setFileName(fileName);
		flowCmd.getDocs().add(doc);

		for (OdocSealSignerCmd c : cmd.getSigners()) {
			EsignCreateFlowCmd.SignerCmd signer = new EsignCreateFlowCmd.SignerCmd();
			signer.setSignerType(c.getSignerType());
			signer.setSignOrder(c.getSignOrder() == null ? 1 : c.getSignOrder());
			signer.setAccountId(c.getAccountId());
			signer.setOrgName(c.getOrgName());
			signer.setOrgIdCardNum(c.getOrgIdCardNum());
			signer.setLegalRepName(c.getLegalRepName());
			signer.setLegalRepIdCardNum(c.getLegalRepIdCardNum());
			signer.setPsnAccount(c.getPsnAccount());
			signer.setPsnName(c.getPsnName());
			signer.setPsnIdCardNum(c.getPsnIdCardNum());
			signer.setNoticeTypes(props.getSign().getNoticeTypes());
			signer.setAutoSign(resolveAutoSign(cmd, c));
			signer.setSealId(resolveSealId(cmd, c));

			EsignCreateFlowCmd.SignFieldCmd field = new EsignCreateFlowCmd.SignFieldCmd();
			field.setFileId(fileId);
			field.setCustomBizNum(cmd.getBizNum());
			field.setPositionPage(String.valueOf(cmd.getPage() == null ? 1 : cmd.getPage()));
			field.setPositionX(cmd.getPositionX() != null ? cmd.getPositionX() : toPixel(cmd.getOffsetX()));
			field.setPositionY(cmd.getPositionY() != null ? cmd.getPositionY() : toPixel(cmd.getOffsetY()));
			field.setAssignedSealId(resolveSealId(cmd, c));
			field.setAutoSign(Boolean.TRUE.equals(resolveAutoSign(cmd, c)));
			signer.getSignFields().add(field);

			flowCmd.getSigners().add(signer);
		}
		return flowCmd;
	}

	/** 印章ID取值优先级：签署方 &gt; 命令 &gt; 业务表设置 &gt; Nacos 默认值 */
	private String resolveSealId(OdocSealCmd cmd, OdocSealSignerCmd c) {
		if (StringUtils.hasText(c.getSealId())) {
			return c.getSealId();
		}
		if (StringUtils.hasText(cmd.getSealId())) {
			return cmd.getSealId();
		}
		String fromSetting = sealConfig.esignDefaultSealId(currentSetting());
		if (StringUtils.hasText(fromSetting)) {
			return fromSetting;
		}
		return props.getSeal() == null ? null : props.getSeal().getDefaultSealId();
	}

	/** 读当前公文通用设置（读失败按 null 处理，回退 Nacos） */
	private WfOdocSetting currentSetting() {
		try {
			return settingService.get();
		} catch (Exception e) {
			log.warn("[odoc-seal] 读取公文设置失败，回退 Nacos 配置：{}", e.getMessage());
			return null;
		}
	}

	/** 是否后台自动落章取值优先级：签署方 &gt; 命令 &gt; 配置 */
	private Boolean resolveAutoSign(OdocSealCmd cmd, OdocSealSignerCmd c) {
		if (c.getAutoSign() != null) {
			return c.getAutoSign();
		}
		if (cmd.getAutoSign() != null) {
			return cmd.getAutoSign();
		}
		return props.getSign().isAutoSignPlatform();
	}

	/** 取第 index 个签署方的签署链接 */
	private String signUrlOf(EsignFlowCreateResult result, int index) {
		List<EsignFlowCreateResult.SignerUrl> urls = result.getSignerUrls();
		if (urls == null || urls.isEmpty()) {
			return null;
		}
		if (index < 0 || index >= urls.size()) {
			return null;
		}
		return urls.get(index).getSignUrl();
	}

	/** 毫米 → 像素（96dpi） */
	private Integer toPixel(Double mm) {
		if (mm == null) {
			return null;
		}
		return (int) Math.round(mm / MM_PER_INCH * DPI);
	}

	/** 复用既有签署流程的结果 */
	private OdocSealOutcome pendingOutcome(WfOdocSignFlow flow) {
		OdocSealOutcome outcome = new OdocSealOutcome();
		outcome.setMode(MODE_ESIGN);
		outcome.setSuccess(true);
		outcome.setFinished(WfOdocSignFlow.SIGN_STATUS_FINISHED == flow.getSignStatus());
		outcome.setEsignFlowId(flow.getFlowId());
		outcome.setSignFlowId(flow.getId());
		outcome.setArchiveVersionId(flow.getArchiveVersionId());
		outcome.setSignUrl(firstSignUrl(flow.getId()));
		outcome.setMessage("复用已发起的签署流程");
		return outcome;
	}

	private String firstSignUrl(Long flowId) {
		WfOdocSignSigner signer = signerMapper.selectOne(Wrappers.<WfOdocSignSigner>lambdaQuery()
			.eq(WfOdocSignSigner::getFlowId, flowId)
			.isNotNull(WfOdocSignSigner::getSignUrl)
			.orderByAsc(WfOdocSignSigner::getSignOrder)
			.last("limit 1"));
		return signer == null ? null : signer.getSignUrl();
	}

	/** e签宝印章 → 统一印章选项 */
	private OdocSealOption option(EsignSeal seal) {
		OdocSealOption o = new OdocSealOption();
		o.setSealId(seal.getSealId());
		o.setSealName(seal.getSealName());
		o.setSealType(seal.getSealType());
		o.setOrgId(seal.getOrgId());
		o.setDefaultSeal(seal.isDefaultSeal());
		o.setMode(MODE_ESIGN);
		return o;
	}

}
