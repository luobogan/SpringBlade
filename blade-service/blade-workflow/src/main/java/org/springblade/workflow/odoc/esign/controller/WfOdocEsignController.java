package org.springblade.workflow.odoc.esign.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.entity.WfOdocSignSigner;
import org.springblade.workflow.odoc.esign.service.EsignFlowService;
import org.springblade.workflow.odoc.mapper.WfOdocSignFlowMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSignSignerMapper;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealOption;
import org.springblade.workflow.odoc.seal.OdocSealStrategyHolder;
import org.springblade.workflow.odoc.vo.OdocSealStatusVO;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公文电子签章（e签宝）控制器。
 *
 * <p>回调入口 {@code POST /odoc/esign/callback} 供厂商外网回调，方法级放行
 * （{@code @PreAuth("permitAll()")}），安全性由<b>验签</b>保证：请求头
 * {@code X-Tsign-Open-Ca-Timestamp} + URL query + 原始 body 用 appSecret 做 HMAC。</p>
 *
 * <p>⚠️ 部署要求：该路径需同时加入 Nacos {@code blade.secure.skip-url}（网关放行）与
 * {@code blade.xss.skip-url}（XSS 过滤器不得改写报文，否则验签必失败）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/odoc/esign")
@RequiredArgsConstructor
@Tag(name = "公文电子签章", description = "e签宝回调与签署入口")
public class WfOdocEsignController {

	/** 厂商回调时间戳请求头 */
	private static final String HEADER_TIMESTAMP = "X-Tsign-Open-Ca-Timestamp";

	/** 厂商回调签名请求头 */
	private static final String HEADER_SIGNATURE = "X-Tsign-Open-Ca-Signature";

	private final EsignFlowService flowService;
	private final WfOdocSignFlowMapper flowMapper;
	private final WfOdocSignSignerMapper signerMapper;
	private final OdocSealStrategyHolder strategyHolder;

	/**
	 * e签宝签署回调（厂商 → 本系统）。
	 *
	 * <p>返回 {@code SUCCESS} 表示已接收；验签失败返回失败，厂商会重试（已落审计记录）。</p>
	 */
	@PreAuth("permitAll()")
	@PostMapping("/callback")
	@Operation(summary = "e签宝签署回调", description = "厂商回调入口（免鉴权，靠验签保护）")
	public R<String> callback(HttpServletRequest request,
							   @RequestHeader(value = HEADER_TIMESTAMP, required = false) String timestamp,
							   @RequestHeader(value = HEADER_SIGNATURE, required = false) String signature,
							   @RequestBody(required = false) String body) {
		String rawQuery = request.getQueryString() == null ? "" : request.getQueryString();
		String query = buildQuery(request);
		boolean ok = flowService.handleCallback(timestamp, query, rawQuery, body, signature);
		if (!ok) {
			return R.fail("验签失败");
		}
		return R.success("SUCCESS");
	}

	/**
	 * 取签署链接（本地签署流程ID）。
	 *
	 * <p>供前端在签批页展示「去签署」入口；已全部签署完成时不再返回链接。</p>
	 */
	@GetMapping("/sign-url/{signFlowId}")
	@Operation(summary = "取签署链接", description = "按本地签署流程ID取签署地址")
	public R<String> signUrl(@PathVariable("signFlowId") Long signFlowId) {
		WfOdocSignFlow flow = flowMapper.selectById(signFlowId);
		if (flow == null) {
			throw new ServiceException("签署流程不存在");
		}
		WfOdocSignSigner signer = signerMapper.selectOne(Wrappers.<WfOdocSignSigner>lambdaQuery()
			.eq(WfOdocSignSigner::getFlowId, signFlowId)
			.isNotNull(WfOdocSignSigner::getSignUrl)
			.orderByAsc(WfOdocSignSigner::getSignOrder)
			.last("limit 1"));
		return R.data(signer == null ? null : signer.getSignUrl());
	}

	/**
	 * 查公文签署状态（供前端待办/签批页展示「签署中 / 去签署 / 已签章」）。
	 *
	 * @param docId 公文正文ID
	 */
	@GetMapping("/status")
	@Operation(summary = "查公文签署状态", description = "按公文ID返回当前签署流程状态与签署链接")
	public R<OdocSealStatusVO> status(@RequestParam("docId") Long docId) {
		OdocSealStatusVO vo = new OdocSealStatusVO();
		vo.setDocId(docId);
		WfOdocSignFlow flow = flowMapper.selectOne(Wrappers.<WfOdocSignFlow>lambdaQuery()
			.eq(WfOdocSignFlow::getDocId, docId)
			.orderByDesc(WfOdocSignFlow::getId)
			.last("limit 1"));
		if (flow == null) {
			vo.setSignStatusText("未发起签署");
			return R.data(vo);
		}
		vo.setSealMode(flow.getSealMode());
		vo.setSignStatus(flow.getSignStatus());
		vo.setSignStatusText(statusText(flow.getSignStatus()));
		vo.setSignFlowId(flow.getId());
		vo.setEsignFlowId(flow.getFlowId());
		vo.setArchiveVersionId(flow.getArchiveVersionId());
		vo.setFinished(flow.getSignStatus() != null
			&& flow.getSignStatus() == WfOdocSignFlow.SIGN_STATUS_FINISHED);
		WfOdocSignSigner signer = signerMapper.selectOne(Wrappers.<WfOdocSignSigner>lambdaQuery()
			.eq(WfOdocSignSigner::getFlowId, flow.getId())
			.isNotNull(WfOdocSignSigner::getSignUrl)
			.orderByAsc(WfOdocSignSigner::getSignOrder)
			.last("limit 1"));
		vo.setSignUrl(signer == null ? null : signer.getSignUrl());
		return R.data(vo);
	}

	/** 签署状态文案 */
	private String statusText(Integer signStatus) {
		if (signStatus == null) {
			return "未发起签署";
		}
		switch (signStatus) {
			case WfOdocSignFlow.SIGN_STATUS_PENDING:
				return "待发起";
			case WfOdocSignFlow.SIGN_STATUS_RUNNING:
				return "签署中";
			case WfOdocSignFlow.SIGN_STATUS_FINISHED:
				return "已签署";
			case WfOdocSignFlow.SIGN_STATUS_CANCELED:
				return "已撤销";
			case WfOdocSignFlow.SIGN_STATUS_REJECTED:
				return "已拒签";
			case WfOdocSignFlow.SIGN_STATUS_EXPIRED:
				return "已过期";
			default:
				return "失败";
		}
	}

	/**
	 * 查第三方可用印章列表（当前机构自有 + 授权印章）。
	 *
	 * <p>供「公文通用设置」选择默认印章、或流程设置里给签署方指定印章。</p>
	 */
	@GetMapping("/seals")
	@Operation(summary = "查第三方印章列表", description = "e签宝当前机构可用印章（自有+授权）")
	public R<List<OdocSealOption>> seals() {
		for (IOdocSealStrategy strategy : strategyHolder.all()) {
			if (IOdocSealStrategy.MODE_ESIGN.equals(strategy.mode())) {
				try {
					return R.data(strategy.listSeals());
				} catch (Exception e) {
					// 未就绪/厂商报错：返回可读提示而不是 500（设置页据此提示「未就绪」）
					log.warn("[esign] 印章列表获取失败：{}", e.getMessage());
					return R.fail("获取印章列表失败：" + e.getMessage());
				}
			}
		}
		return R.data(new ArrayList<>());
	}

	/**
	 * 构建验签用的 query 串：官方口径「按 key 字典序排序后拼接 value」。
	 */
	private String buildQuery(HttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();
		if (params == null || params.isEmpty()) {
			return "";
		}
		List<String> keys = new ArrayList<>(params.keySet());
		keys.sort(String::compareTo);
		List<String> values = new ArrayList<>();
		for (String key : keys) {
			String[] vs = params.get(key);
			if (vs != null && vs.length > 0) {
				values.add(vs[0]);
			}
		}
		return String.join("&", values);
	}

}
