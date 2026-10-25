package org.springblade.workflow.odoc.esign;

import org.springblade.workflow.odoc.esign.dto.EsignCreateFlowCmd;
import org.springblade.workflow.odoc.esign.dto.EsignFlowCreateResult;
import org.springblade.workflow.odoc.esign.dto.EsignFlowDetail;
import org.springblade.workflow.odoc.esign.dto.EsignSeal;

import java.util.List;

/**
 * e签宝（SaaS API V3）服务契约。
 *
 * <p><b>设计要点</b>：本接口是系统内唯一接触 e签宝的地方，业务代码（公文流程）只依赖本接口，
 * 以便后续在 公有云 SaaS / 混合云「天印」/ 私有化 之间切换时只替换实现（决策 D1）。</p>
 *
 * <p>密钥等敏感配置只存在于服务端，不外泄到前端。</p>
 */
public interface IEsignService {

	// ==================== 文件 ====================

	/**
	 * 上传待签文件（三步走）：获取上传地址 → 推送文件流 → 轮询至就绪。
	 *
	 * <p>官方要求：文件有异步处理过程，<b>必须</b>等 {@code fileStatus} 为 {@code 2} 或 {@code 5}
	 * 才能发起签署。</p>
	 *
	 * @return e签宝 fileId
	 */
	String uploadFile(byte[] content, String fileName);

	/** 文件是否已准备完成（fileStatus = 2 或 5） */
	boolean isFileReady(String fileId);

	// ==================== 签署流程 ====================

	/** 基于文件创建签署流程 */
	EsignFlowCreateResult createSignFlow(EsignCreateFlowCmd cmd);

	/** 发起签署 */
	void startSignFlow(String flowId);

	/** 获取签署方签署链接 */
	String getSignUrl(String flowId, String psnAccount, String orgName, int urlType);

	/** 查询流程详情（回调补偿的核心手段） */
	EsignFlowDetail getFlowDetail(String flowId);

	/** 获取签署完成件下载地址（归档回写用） */
	String getFileDownloadUrl(String flowId);

	/** 下载签署完成件字节（归档回写用） */
	byte[] downloadSignedFile(String flowId);

	/** 撤销签署流程 */
	void revoke(String flowId);

	/** 催办 */
	void urge(String flowId);

	// ==================== 印章 ====================

	/** 企业自有印章列表 */
	List<EsignSeal> listOrgOwnSeals(String orgId);

	/** 企业被授权印章列表 */
	List<EsignSeal> listOrgAuthorizedSeals(String orgId);

	// ==================== 回调 ====================

	/** 验签（timestamp + query + body + secret + signature） */
	boolean checkCallback(String timestamp, String query, String body, String signature);
}
