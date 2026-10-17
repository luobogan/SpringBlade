package org.springblade.workflow.odoc.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocSealService;
import org.springblade.workflow.odoc.service.IWfOdocSecretRuleService;
import org.springblade.formmode.feign.IFormmodeClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfNodeMustDoActionExecutor} 节点必做动作纯逻辑单测（mock Mapper/Service，不依赖 MySQL）。
 *
 * <p>锁定：套红必做（回归）、签章硬拦截（含 CSV 多选命中、sealRequired 门控、CA 不可用降级）、
 * 清稿/打印仅记录不阻断、非公文流程直接放行。</p>
 */
class WfNodeMustDoActionExecutorTest {

	private final IWfOdocDocService docService = mock(IWfOdocDocService.class);
	private final WfOdocProcessSetMapper processSetMapper = mock(WfOdocProcessSetMapper.class);
	private final IWfOdocSealService sealService = mock(IWfOdocSealService.class);
	private final IFormmodeClient formmodeClient = mock(IFormmodeClient.class);
	private final IWfOdocSecretRuleService secretRuleService = mock(IWfOdocSecretRuleService.class);

	private final WfNodeMustDoActionExecutor executor =
		new WfNodeMustDoActionExecutor(docService, processSetMapper, sealService, formmodeClient, secretRuleService);

	private WfInstance inst(long id, long defId) {
		WfInstance i = new WfInstance();
		i.setId(id);
		i.setDefId(defId);
		return i;
	}

	private WfInstance instForm(long id, long defId, Long formId, Long dataId) {
		WfInstance i = inst(id, defId);
		i.setFormId(formId);
		i.setDataId(dataId);
		return i;
	}

	private static Map<String, Object> formValues(String... kv) {
		Map<String, Object> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}

	private WfProcessNode node(String key) {
		WfProcessNode n = new WfProcessNode();
		n.setNodeKey(key);
		return n;
	}

	private WfOdocDoc doc(long id, int hasUsedTemplet) {
		WfOdocDoc d = new WfOdocDoc();
		d.setId(id);
		d.setHasUsedTemplet(hasUsedTemplet);
		return d;
	}

	@Test
	@DisplayName("套红节点已套红 → 放行")
	void taohongDone() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setMustTaohong(1);
		set.setTaohongNodeKey("th");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc(100L, 1));
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("th")));
	}

	@Test
	@DisplayName("套红节点未套红 → 拒提交")
	void taohongMissing() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setMustTaohong(1);
		set.setTaohongNodeKey("th");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc(100L, 0));
		assertThrows(ServiceException.class, () -> executor.checkBeforeSubmit(inst(1, 9), node("th")));
	}

	@Test
	@DisplayName("签章节点已签章(sealRequired=1) → 放行")
	void sealDone() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSealRequired(1);
		set.setSignNodeKey("sign");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(sealService.sealAvailable()).thenReturn(true);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc(100L, 1));
		WfOdocSeal s = new WfOdocSeal();
		s.setStatus(1);
		when(sealService.list(100L)).thenReturn(Collections.singletonList(s));
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("sign")));
	}

	@Test
	@DisplayName("签章节点未签章(命中 CSV 多选) → 拒提交")
	void sealMissingViaCsv() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSealRequired(1);
		set.setSignatureNodeKeys("a, sign ,b");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(sealService.sealAvailable()).thenReturn(true);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc(100L, 1));
		when(sealService.list(100L)).thenReturn(Collections.emptyList());
		assertThrows(ServiceException.class, () -> executor.checkBeforeSubmit(inst(1, 9), node("sign")));
	}

	@Test
	@DisplayName("签章节点但未装配 CA → 降级放行，不查签章记录")
	void sealDegradedWhenUnavailable() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSealRequired(1);
		set.setSignNodeKey("sign");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(sealService.sealAvailable()).thenReturn(false);
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("sign")));
		verify(sealService, never()).list(any());
	}

	@Test
	@DisplayName("签章未开启(sealRequired=0) → 放行且不查签章记录")
	void sealNotRequired() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSealRequired(0);
		set.setSignNodeKey("sign");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc(100L, 0));
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("sign")));
		verify(sealService, never()).list(any());
	}

	@Test
	@DisplayName("清稿/打印节点 → 仅记录，不阻断")
	void clearPrintLogOnly() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setCleanCopyNodeKeys("cc");
		set.setPrintNodeKeys("pp");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("cc")));
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("pp")));
	}

	@Test
	@DisplayName("无流程设置(非公文流程) → 直接放行，不查正文")
	void noProcessSet() {
		when(processSetMapper.selectOne(any())).thenReturn(null);
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(inst(1, 9), node("any")));
		verify(docService, never()).getDocByInstanceId(any());
	}

	@Test
	@DisplayName("密级收发限制：收文单位越限 → 拒提交")
	void secretLevelViolation() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSecretLevelFieldKey("fld_sec");
		set.setReceiveUnitFieldKey("fld_unit");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(secretRuleService.violationMessage("机密", "5")).thenReturn("收文单位密级不符：X");
		FormDataVO fd = new FormDataVO();
		fd.setFieldValues(formValues("fld_sec", "机密", "fld_unit", "5"));
		when(formmodeClient.getBusinessData(any(), any())).thenReturn(R.data(fd));
		assertThrows(ServiceException.class,
			() -> executor.checkBeforeSubmit(instForm(1, 9, 10L, 20L), node("n")));
	}

	@Test
	@DisplayName("密级收发限制：未配密级/收文单位字段 → 不校验、不读表单")
	void secretLevelNoFields() {
		when(processSetMapper.selectOne(any())).thenReturn(new WfOdocProcessSet());
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(instForm(1, 9, 10L, 20L), node("n")));
		verify(formmodeClient, never()).getBusinessData(any(), any());
		verify(secretRuleService, never()).violationMessage(any(), any());
	}

	@Test
	@DisplayName("密级收发限制：校验通过 → 放行")
	void secretLevelOk() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setSecretLevelFieldKey("fld_sec");
		set.setReceiveUnitFieldKey("fld_unit");
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(secretRuleService.violationMessage(any(), any())).thenReturn(null);
		FormDataVO fd = new FormDataVO();
		fd.setFieldValues(formValues("fld_sec", "公开", "fld_unit", "5"));
		when(formmodeClient.getBusinessData(any(), any())).thenReturn(R.data(fd));
		assertDoesNotThrow(() -> executor.checkBeforeSubmit(instForm(1, 9, 10L, 20L), node("n")));
	}
}
