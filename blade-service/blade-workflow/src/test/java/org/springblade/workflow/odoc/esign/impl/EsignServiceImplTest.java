package org.springblade.workflow.odoc.esign.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springblade.workflow.odoc.esign.core.EsignHttpExecutor;
import org.springblade.workflow.odoc.esign.dto.EsignFlowDetail;
import org.springblade.workflow.odoc.esign.dto.EsignSeal;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link EsignServiceImpl} 单测：厂商业务码必须显式失败，不能被静默吞成空结果。
 *
 * <p>e签宝 V3 以 HTTP 200 + {@code code != 0} 表示失败（如凭据无效 401）。
 * 若不校验，会出现「印章列表为空」「签署流程已创建」等假象。</p>
 */
class EsignServiceImplTest {

	private EsignHttpExecutor executor;
	private EsignServiceImpl service;

	@BeforeEach
	void setUp() {
		executor = mock(EsignHttpExecutor.class);
		service = new EsignServiceImpl(mock(EsignProperties.class), executor);
	}

	private void stubResponse(String resp) {
		when(executor.request(anyString(), anyString(), any())).thenReturn(resp);
	}

	@Test
	@DisplayName("listOrgOwnSeals：厂商成功响应解析出印章列表")
	void listSealsSuccess() {
		stubResponse("{\"code\":0,\"message\":\"成功\",\"data\":{\"seals\":[{\"sealId\":\"s1\",\"sealName\":\"公章\",\"orgId\":\"o1\"}]}}");

		List<EsignSeal> seals = service.listOrgOwnSeals("o1");

		assertEquals(1, seals.size());
		assertEquals("s1", seals.get(0).getSealId());
		assertEquals("公章", seals.get(0).getSealName());
	}

	@Test
	@DisplayName("listOrgOwnSeals：厂商返回 code=401 时抛业务异常（不再静默返回空列表）")
	void listSealsVendorError() {
		stubResponse("{\"success\":false,\"code\":401,\"message\":\"应用ID或密钥错误\"}");

		ServiceException e = assertThrows(ServiceException.class, () -> service.listOrgOwnSeals("o1"));
		assertTrue(e.getMessage().contains("应用ID或密钥错误"), e.getMessage());
		assertTrue(e.getMessage().contains("401"), e.getMessage());
	}

	@Test
	@DisplayName("listOrgOwnSeals：厂商 success=false 无 code 时同样抛异常")
	void listSealsSuccessFalse() {
		stubResponse("{\"success\":false,\"errorMsg\":\"签名校验失败\"}");

		ServiceException e = assertThrows(ServiceException.class, () -> service.listOrgOwnSeals("o1"));
		assertTrue(e.getMessage().contains("签名校验失败"), e.getMessage());
	}

	@Test
	@DisplayName("getFlowDetail：厂商失败时抛异常，不再返回空详情")
	void flowDetailVendorError() {
		stubResponse("{\"success\":false,\"code\":403,\"message\":\"无权限访问签署流程\"}");

		ServiceException e = assertThrows(ServiceException.class, () -> service.getFlowDetail("flow-1"));
		assertTrue(e.getMessage().contains("无权限访问签署流程"), e.getMessage());
	}

	@Test
	@DisplayName("getFlowDetail：成功响应解析签署方与整体状态")
	void flowDetailSuccess() {
		stubResponse("{\"code\":0,\"data\":{\"signFlowStatus\":\"1\",\"signFlowTitle\":\"测试\","
			+ "\"signers\":[{\"accountId\":\"u1\",\"psnName\":\"张三\",\"signResult\":\"0\"}]}}");

		EsignFlowDetail detail = service.getFlowDetail("flow-1");

		assertEquals("测试", detail.getFlowTitle());
		assertTrue(!detail.isFinished());
		assertEquals(1, detail.getSigners().size());
		assertEquals("u1", detail.getSigners().get(0).getAccountId());
	}

	@Test
	@DisplayName("startSignFlow：厂商失败时抛异常（发起签署失败必须可见）")
	void startSignFlowVendorError() {
		stubResponse("{\"success\":false,\"code\":500,\"message\":\"签署流程已存在\"}");

		ServiceException e = assertThrows(ServiceException.class, () -> service.startSignFlow("flow-1"));
		assertTrue(e.getMessage().contains("签署流程已存在"), e.getMessage());
	}
}
