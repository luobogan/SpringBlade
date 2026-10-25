package org.springblade.workflow.odoc.esign.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.odoc.entity.WfOdocSignFlow;
import org.springblade.workflow.odoc.entity.WfOdocSignNotify;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EsignFlowService} 单测：回调落库的幂等语义。
 *
 * <p>关键场景：同一 {@code (esignFlowId, action, ts)} 先因验签失败被记为「处理失败」，
 * 厂商重投同一事件（签名正确）时必须允许重试，不能被误判为「重复投递已忽略」。</p>
 */
class EsignFlowServiceTest {

	private static final String FLOW_ID = "sandbox-selftest-flow";
	private static final String ACTION = "SIGN_FLOW";
	private static final String TS = "1792908853299";

	private IEsignService esignService;
	private WfOdocSignFlowMapper flowMapper;
	private WfOdocSignSignerMapper signerMapper;
	private WfOdocSignNotifyMapper notifyMapper;
	private OdocSealAdvancer advancer;
	private EsignFlowService flowService;

	/** 回调记录表（内存模拟，唯一键 esignFlowId + action + ts） */
	private final List<WfOdocSignNotify> store = new ArrayList<>();

	@BeforeEach
	void setUp() {
		esignService = mock(IEsignService.class);
		flowMapper = mock(WfOdocSignFlowMapper.class);
		signerMapper = mock(WfOdocSignSignerMapper.class);
		notifyMapper = mock(WfOdocSignNotifyMapper.class);
		advancer = mock(OdocSealAdvancer.class);
		flowService = new EsignFlowService(esignService, mock(EsignProperties.class), flowMapper,
			signerMapper, notifyMapper, mock(IWfOdocDocService.class), mock(IOdocFileStore.class), advancer);

		store.clear();
		// insert：命中唯一键 → 抛 DuplicateKeyException
		org.mockito.Mockito.doAnswer(inv -> {
			WfOdocSignNotify n = inv.getArgument(0);
			for (WfOdocSignNotify e : store) {
				if (Objects.equals(e.getEsignFlowId(), n.getEsignFlowId())
					&& Objects.equals(e.getAction(), n.getAction())
					&& Objects.equals(e.getTs(), n.getTs())) {
					throw new DuplicateKeyException("uk_esign_flow_id_action_ts");
				}
			}
			store.add(n);
			return 1;
		}).when(notifyMapper).insert(any(WfOdocSignNotify.class));
		// selectOne：本用例只涉及一个唯一键，取内存中的记录
		when(notifyMapper.selectOne(any())).thenAnswer(inv -> store.isEmpty() ? null : store.get(store.size() - 1));
		when(notifyMapper.updateById(any(WfOdocSignNotify.class))).thenReturn(1);
		when(signerMapper.selectCount(any())).thenReturn(0L);
	}

	private String body() {
		return "{\"action\":\"" + ACTION + "\",\"flowId\":\"" + FLOW_ID + "\",\"timestamp\":\"" + TS + "\"}";
	}

	private String query() {
		return "signFlowId=" + FLOW_ID;
	}

	@Test
	@DisplayName("回调验签失败：落库并标记处理失败")
	void callbackVerifyFailed() {
		when(esignService.checkCallback(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

		boolean ok = flowService.handleCallback(TS, query(), query(), body(), "deadbeef");

		assertFalse(ok);
		assertEquals(1, store.size());
		assertEquals(WfOdocSignNotify.PROCESSED_FAILED, store.get(0).getProcessed());
	}

	@Test
	@DisplayName("同 ts 重投且验签通过：上次失败的记录允许重试，不被误判为重复投递")
	void callbackRetryAfterFailure() {
		when(esignService.checkCallback(anyString(), anyString(), anyString(), anyString()))
			.thenReturn(false, true);
		// 未匹配到本地签署流程
		when(flowMapper.selectOne(any())).thenReturn(null);

		assertFalse(flowService.handleCallback(TS, query(), query(), body(), "deadbeef"));
		assertEquals(WfOdocSignNotify.PROCESSED_FAILED, store.get(0).getProcessed());

		// 同一 ts 再投一次（签名正确）→ 应继续处理（去查本地签署流程），而不是「重复投递已忽略」
		boolean ok = flowService.handleCallback(TS, query(), query(), body(), "goodsig");

		assertTrue(ok);
		assertEquals(1, store.size());
		verify(flowMapper, times(1)).selectOne(any());
	}

	@Test
	@DisplayName("已成功处理的重复投递：忽略且不再查本地签署流程")
	void callbackDuplicateIgnored() {
		when(esignService.checkCallback(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
		WfOdocSignFlow flow = new WfOdocSignFlow();
		flow.setId(1L);
		flow.setFlowId(FLOW_ID);
		flow.setDocId(2L);
		flow.setNotifyCount(0);
		when(flowMapper.selectOne(any())).thenReturn(flow);
		EsignFlowDetail detail = new EsignFlowDetail();
		detail.setFlowId(FLOW_ID);
		detail.setFlowStatus(1);
		detail.setFinished(false);
		when(esignService.getFlowDetail(anyString())).thenReturn(detail);

		assertTrue(flowService.handleCallback(TS, query(), query(), body(), "goodsig"));
		assertEquals(WfOdocSignNotify.PROCESSED_DONE, store.get(0).getProcessed());
		verify(flowMapper, times(1)).selectOne(any());

		// 再次投递同一事件 → 视为重复，忽略
		assertTrue(flowService.handleCallback(TS, query(), query(), body(), "goodsig"));
		verify(flowMapper, times(1)).selectOne(any());
	}
}
