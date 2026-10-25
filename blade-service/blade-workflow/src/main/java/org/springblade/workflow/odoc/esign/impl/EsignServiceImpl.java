package org.springblade.workflow.odoc.esign.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.esign.IEsignService;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springblade.workflow.odoc.esign.core.EsignHttpExecutor;
import org.springblade.workflow.odoc.esign.core.EsignSigner;
import org.springblade.workflow.odoc.esign.dto.EsignCreateFlowCmd;
import org.springblade.workflow.odoc.esign.dto.EsignFlowCreateResult;
import org.springblade.workflow.odoc.esign.dto.EsignFlowDetail;
import org.springblade.workflow.odoc.esign.dto.EsignSeal;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * e签宝 SaaS API V3 服务实现（HTTP 直连 OpenAPI，签名算法移植自官方 Demo）。
 *
 * <p>接口路径均取自官方 Demo {@code SaaSAPI_V3_Demo_JAVA}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsignServiceImpl implements IEsignService {

	private static final int FILE_READY_RETRY = 5;
	private static final long FILE_READY_SLEEP_MS = 2000;

	private final EsignProperties props;
	private final EsignHttpExecutor executor;
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ==================== 文件 ====================

	@Override
	public String uploadFile(byte[] content, String fileName) {
		String md5 = md5Base64(content);
		String body = "{\"contentMd5\":\"" + md5 + "\",\"fileName\":\"" + esc(fileName) + "\","
			+ "\"fileSize\":" + content.length + ",\"convertToPDF\":false,"
			+ "\"contentType\":\"application/octet-stream\"}";
		String resp = executor.request("/v3/files/file-upload-url", "POST", body);
		JsonNode data = data("/v3/files/file-upload-url", resp);
		String fileId = text(data, "fileId");
		String uploadUrl = text(data, "fileUploadUrl");
		if (fileId == null || uploadUrl == null) {
			throw new IllegalStateException("获取文件上传地址失败：" + resp);
		}
		// 第二步：推送文件流
		executor.uploadStream(uploadUrl, content, md5);
		// 第三步：轮询至就绪（官方要求：异步处理完成后才能发起签署）
		for (int i = 0; i < FILE_READY_RETRY; i++) {
			if (isFileReady(fileId)) {
				return fileId;
			}
			sleep(FILE_READY_SLEEP_MS);
		}
		log.warn("[esign] 文件超过重试次数仍未就绪，fileId={}", fileId);
		return fileId;
	}

	@Override
	public boolean isFileReady(String fileId) {
		String apiPath = "/v3/files/" + fileId;
		String resp = executor.request(apiPath, "GET", null);
		JsonNode data = data(apiPath, resp);
		String status = text(data, "fileStatus");
		return "2".equals(status) || "5".equals(status);
	}

	// ==================== 签署流程 ====================

	@Override
	public EsignFlowCreateResult createSignFlow(EsignCreateFlowCmd cmd) {
		String apiPath = "/v3/sign-flow/create-by-file";
		String resp = executor.request(apiPath, "POST", buildCreateFlowBody(cmd));
		JsonNode data = data(apiPath, resp);
		EsignFlowCreateResult result = new EsignFlowCreateResult();
		result.setFlowId(text(data, "signFlowId"));
		if (cmd.getDocs() != null && !cmd.getDocs().isEmpty()) {
			result.setFileId(cmd.getDocs().get(0).getFileId());
		}
		return result;
	}

	@Override
	public void startSignFlow(String flowId) {
		String apiPath = "/v3/sign-flow/" + flowId + "/start";
		check(apiPath, executor.request(apiPath, "POST", "{}"));
	}

	@Override
	public String getSignUrl(String flowId, String psnAccount, String orgName, int urlType) {
		StringBuilder sb = new StringBuilder("{\"clientType\":\"ALL\",\"needLogin\":true,\"urlType\":")
			.append(urlType);
		if (psnAccount != null && !psnAccount.isEmpty()) {
			sb.append(",\"operator\":{\"psnAccount\":\"").append(esc(psnAccount)).append("\"}");
		}
		if (orgName != null && !orgName.isEmpty()) {
			sb.append(",\"organization\":{\"orgName\":\"").append(esc(orgName)).append("\"}");
		}
		sb.append("}");
		String apiPath = "/v3/sign-flow/" + flowId + "/sign-url";
		String resp = executor.request(apiPath, "POST", sb.toString());
		JsonNode data = data(apiPath, resp);
		return text(data, "shortUrl") != null ? text(data, "shortUrl") : text(data, "url");
	}

	@Override
	public EsignFlowDetail getFlowDetail(String flowId) {
		String apiPath = "/v3/sign-flow/" + flowId + "/detail";
		String resp = executor.request(apiPath, "GET", null);
		JsonNode data = data(apiPath, resp);
		EsignFlowDetail detail = new EsignFlowDetail();
		detail.setFlowId(flowId);
		detail.setFlowTitle(text(data, "signFlowTitle"));
		String status = text(data, "signFlowStatus");
		detail.setFlowStatus(status == null ? null : Integer.valueOf(status));
		// 2=已完成（以官方枚举为准，待补充完整事件/状态清单）
		detail.setFinished("2".equals(status));
		JsonNode signers = data == null ? null : data.get("signers");
		if (signers != null && signers.isArray()) {
			List<EsignFlowDetail.SignerStatus> list = new ArrayList<>();
			for (JsonNode s : signers) {
				EsignFlowDetail.SignerStatus st = new EsignFlowDetail.SignerStatus();
				st.setAccountId(text(s, "accountId"));
				st.setAccountName(text(s, "accountName") != null ? text(s, "accountName") : text(s, "psnName"));
				String sr = text(s, "signResult");
				st.setSignResult(sr == null ? null : Integer.valueOf(sr));
				st.setSigned("2".equals(sr));
				st.setSignTime(text(s, "signTime"));
				list.add(st);
			}
			detail.setSigners(list);
		}
		return detail;
	}

	@Override
	public String getFileDownloadUrl(String flowId) {
		String apiPath = "/v3/sign-flow/" + flowId + "/file-download-url";
		String resp = executor.request(apiPath, "GET", null);
		return text(data(apiPath, resp), "fileDownloadUrl");
	}

	@Override
	public byte[] downloadSignedFile(String flowId) {
		String url = getFileDownloadUrl(flowId);
		if (url == null || url.isBlank()) {
			throw new IllegalStateException("获取签署完成件下载地址失败 flowId=" + flowId);
		}
		return executor.download(url);
	}

	@Override
	public void revoke(String flowId) {
		String apiPath = "/v3/sign-flow/" + flowId + "/revoke";
		check(apiPath, executor.request(apiPath, "POST", "{}"));
	}

	@Override
	public void urge(String flowId) {
		String apiPath = "/v3/sign-flow/" + flowId + "/urge";
		check(apiPath, executor.request(apiPath, "POST", "{}"));
	}

	// ==================== 印章 ====================

	@Override
	public List<EsignSeal> listOrgOwnSeals(String orgId) {
		return listSeals("/v3/seals/org-own-seal-list?orgId=" + orgId + "&pageNum=1&pageSize=100");
	}

	@Override
	public List<EsignSeal> listOrgAuthorizedSeals(String orgId) {
		return listSeals("/v3/seals/org-authorized-seal-list?orgId=" + orgId + "&pageNum=1&pageSize=100");
	}

	private List<EsignSeal> listSeals(String apiPath) {
		String resp = executor.request(apiPath, "GET", null);
		JsonNode data = data(apiPath, resp);
		List<EsignSeal> list = new ArrayList<>();
		JsonNode arr = data == null ? null : data.get("seals");
		if (arr == null && data != null) {
			arr = data.get("list");
		}
		if (arr != null && arr.isArray()) {
			for (JsonNode n : arr) {
				EsignSeal seal = new EsignSeal();
				seal.setSealId(text(n, "sealId"));
				seal.setSealName(text(n, "sealName"));
				seal.setOrgId(text(n, "orgId"));
				seal.setSealType(text(n, "sealType"));
				list.add(seal);
			}
		}
		return list;
	}

	// ==================== 回调 ====================

	@Override
	public boolean checkCallback(String timestamp, String query, String body, String signature) {
		return EsignSigner.callbackCheck(timestamp, query, body, props.getAppSecret(), signature);
	}

	// ==================== 私有工具 ====================

	/** 构造 create-by-file 请求体 */
	private String buildCreateFlowBody(EsignCreateFlowCmd cmd) {
		StringBuilder sb = new StringBuilder("{\"docs\":[");
		for (int i = 0; i < cmd.getDocs().size(); i++) {
			EsignCreateFlowCmd.Doc d = cmd.getDocs().get(i);
			if (i > 0) {
				sb.append(',');
			}
			sb.append("{\"fileId\":\"").append(esc(d.getFileId())).append("\",\"fileName\":\"")
				.append(esc(d.getFileName())).append("\"}");
		}
		sb.append("],\"signFlowConfig\":{\"signFlowTitle\":\"").append(esc(cmd.getTitle())).append("\"")
			.append(",\"chargeConfig\":{\"chargeMode\":").append(cmd.getChargeMode() == null ? 0 : cmd.getChargeMode())
			.append("}},\"signers\":[");
		for (int i = 0; i < cmd.getSigners().size(); i++) {
			EsignCreateFlowCmd.SignerCmd s = cmd.getSigners().get(i);
			if (i > 0) {
				sb.append(',');
			}
			sb.append("{\"noticeConfig\":{\"noticeTypes\":\"")
				.append(esc(s.getNoticeTypes() == null ? props.getSign().getNoticeTypes() : s.getNoticeTypes()))
				.append("\"},\"signConfig\":{\"signOrder\":")
				.append(s.getSignOrder() == null ? 1 : s.getSignOrder()).append("},\"signerType\":")
				.append(s.getSignerType() == null ? 1 : s.getSignerType()).append(',');
			// 签署方主体信息
			sb.append(signerInfo(s));
			// 签署区（用印位置）
			sb.append(",\"signFields\":[");
			for (int j = 0; j < s.getSignFields().size(); j++) {
				EsignCreateFlowCmd.SignFieldCmd f = s.getSignFields().get(j);
				if (j > 0) {
					sb.append(',');
				}
				sb.append("{\"fileId\":\"").append(esc(f.getFileId())).append('"')
					.append(",\"customBizNum\":\"").append(esc(f.getCustomBizNum())).append('"')
					.append(",\"signFieldType\":").append(f.getSignFieldType()).append(",\"normalSignFieldConfig\":{")
					.append("\"assignedSealId\":\"").append(esc(f.getAssignedSealId() == null
						? props.getSeal().getDefaultSealId() : f.getAssignedSealId())).append('"')
					.append(",\"autoSign\":").append(f.getAutoSign() != null ? f.getAutoSign()
						: props.getSign().isAutoSignPlatform())
					.append(",\"freeMode\":").append(Boolean.TRUE.equals(f.getFreeMode()))
					.append(",\"movableSignField\":").append(Boolean.TRUE.equals(f.getMovableSignField()))
					.append(",\"signFieldStyle\":").append(f.getSignFieldStyle())
					.append(",\"signFieldPosition\":{\"positionPage\":\"").append(esc(f.getPositionPage())).append('"')
					.append(",\"positionX\":").append(f.getPositionX() == null ? 0 : f.getPositionX())
					.append(",\"positionY\":").append(f.getPositionY() == null ? 0 : f.getPositionY())
					.append("}}}");
			}
			sb.append("]}");
		}
		sb.append("]}");
		return sb.toString();
	}

	/** 签署方主体：企业走 orgSignerInfo，个人走 psnSignerInfo */
	private String signerInfo(EsignCreateFlowCmd.SignerCmd s) {
		if (Integer.valueOf(1).equals(s.getSignerType())) {
			return "\"orgSignerInfo\":{\"orgId\":\"" + esc(s.getAccountId()) + "\",\"orgName\":\""
				+ esc(s.getOrgName()) + "\",\"orgInfo\":{\"orgIDCardNum\":\"" + esc(s.getOrgIdCardNum())
				+ "\",\"orgIDCardType\":\"" + esc(s.getOrgIdCardType()) + "\",\"legalRepName\":\""
				+ esc(s.getLegalRepName()) + "\",\"legalRepIDCardNum\":\"" + esc(s.getLegalRepIdCardNum())
				+ "\"},\"transactorInfo\":{\"psnAccount\":\"" + esc(s.getPsnAccount())
				+ "\",\"psnInfo\":{\"psnName\":\"" + esc(s.getPsnName()) + "\",\"psnIDCardNum\":\""
				+ esc(s.getPsnIdCardNum()) + "\"}}}";
		}
		return "\"psnSignerInfo\":{\"psnAccount\":\"" + esc(s.getPsnAccount())
			+ "\",\"psnInfo\":{\"psnName\":\"" + esc(s.getPsnName()) + "\",\"psnIDCardNum\":\""
			+ esc(s.getPsnIdCardNum()) + "\"}}";
	}

	/**
	 * 取响应 data 节点；先校验厂商业务状态，失败即抛出带厂商提示的异常。
	 *
	 * <p>e签宝 V3 以 HTTP 200 + 业务码返回失败（如凭据无效 401），若不校验会被静默当成
	 * 「空结果」，导致「印章列表为空」「签署流程已创建」等假象。故此处统一显式失败。</p>
	 */
	private JsonNode data(String apiPath, String resp) {
		JsonNode root = parse(resp);
		if (root == null) {
			return null;
		}
		check(apiPath, root);
		return root.get("data");
	}

	/** 校验厂商业务码：code != 0 或 success=false 视为失败 */
	private void check(String apiPath, String resp) {
		JsonNode root = parse(resp);
		if (root == null) {
			return;
		}
		check(apiPath, root);
	}

	private void check(String apiPath, JsonNode root) {
		JsonNode code = root.get("code");
		if (code != null && code.isNumber() && code.asInt() != 0) {
			throw new ServiceException("调用 e签宝接口失败(" + apiPath + ")："
				+ vendorMessage(root) + "（code=" + code.asInt() + "）");
		}
		JsonNode success = root.get("success");
		if (success != null && success.isBoolean() && !success.asBoolean()) {
			throw new ServiceException("调用 e签宝接口失败(" + apiPath + ")：" + vendorMessage(root));
		}
	}

	/** 提取厂商错误提示（兼容 message/errorMsg/msg 等不同字段名） */
	private String vendorMessage(JsonNode root) {
		for (String field : new String[]{"message", "errorMsg", "msg", "errorMessage"}) {
			String v = text(root, field);
			if (v != null && !v.isBlank()) {
				return v;
			}
		}
		return root.toString();
	}

	/** 解析响应报文，解析失败返回 null（由调用方按空结果处理） */
	private JsonNode parse(String resp) {
		try {
			return objectMapper.readTree(resp == null ? "{}" : resp);
		} catch (Exception e) {
			log.warn("[esign] 解析响应失败：{}", resp);
			return null;
		}
	}

	private String text(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private String md5Base64(byte[] content) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return Base64.getEncoder().encodeToString(md5.digest(content));
		} catch (Exception e) {
			throw new IllegalStateException("计算文件 MD5 失败", e);
		}
	}

	/** JSON 字符串转义（最小化处理，避免拼接注入） */
	private String esc(String v) {
		if (v == null) {
			return "";
		}
		return v.replace("\\", "\\\\").replace("\"", "\\\"")
			.replace("\n", "\\n").replace("\r", "\\r");
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
