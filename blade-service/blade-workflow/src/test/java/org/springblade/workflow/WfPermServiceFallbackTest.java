package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.entity.WfNodeDetailFilter;
import org.springblade.workflow.entity.WfNodeDetailPerm;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.mapper.WfNodeDetailFilterMapper;
import org.springblade.workflow.mapper.WfNodeDetailPermMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.impl.WfPermServiceImpl;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P3-5 安全护栏回归：perm/detailPerm/detailFilter 开关开启时，逐节点「BPMN 读到空即回退 wf_* 表」，
 * 避免草稿/缺扩展/缺子元素定义被静默降级为零（dev 实测踩坑：2026-10-01 草稿定义翻转 perm 后权限归零）。
 */
@ExtendWith(MockitoExtension.class)
class WfPermServiceFallbackTest {

	@Mock
	private WfNodeFieldPermMapper fieldPermMapper;
	@Mock
	private WfNodeDetailPermMapper detailPermMapper;
	@Mock
	private WfNodeDetailFilterMapper detailFilterMapper;
	@Mock
	private WfBpmnExtensionReader bpmnReader;

	private WfPermServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new WfPermServiceImpl(fieldPermMapper, detailPermMapper, detailFilterMapper, bpmnReader);
		// 模拟「开关全部开启」
		ReflectionTestUtils.setField(service, "permFromBpmn", true);
		ReflectionTestUtils.setField(service, "detailPermFromBpmn", true);
		ReflectionTestUtils.setField(service, "detailFilterFromBpmn", true);
	}

	@Test
	@DisplayName("perm 开关开 + BPMN 无数据 → 回退 wf_node_field_perm 表（不降级为空）")
	void fieldPermFallsBackToDbWhenBpmnEmpty() {
		WfNodeFieldPerm row = new WfNodeFieldPerm();
		row.setScope("main");
		row.setFieldName("title");
		row.setIsVisible(1);
		row.setIsEditable(0);
		row.setIsRequired(0);
		row.setPerm(2);
		when(fieldPermMapper.selectList(any())).thenReturn(List.of(row));
		when(bpmnReader.fieldPerms(1L, "Task_1")).thenReturn(List.of()); // BPMN 空

		List<FieldPermVO> result = service.getFieldPerm(1L, "Task_1");

		assertThat(result).hasSize(1);
		FieldPermVO vo = result.get(0);
		assertThat(vo.getFieldName()).isEqualTo("title");
		assertThat(vo.getVisible()).isTrue();
		assertThat(vo.getEditable()).isFalse();
		assertThat(vo.getRequired()).isFalse();
	}

	@Test
	@DisplayName("perm 开关开 + BPMN 有数据 → 读 BPMN 扩展（不回退）")
	void fieldPermReadsBpmnWhenPresent() {
		BpmnExtensionUtil.WfFieldPermExt ext = new BpmnExtensionUtil.WfFieldPermExt();
		ext.field = "title";
		ext.visible = "1";
		ext.editable = "0";
		ext.required = "0";
		when(bpmnReader.fieldPerms(1L, "Task_1")).thenReturn(List.of(ext));

		List<FieldPermVO> result = service.getFieldPerm(1L, "Task_1");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFieldName()).isEqualTo("title");
		assertThat(result.get(0).getVisible()).isTrue();
	}

	@Test
	@DisplayName("detailPerm 开关开 + BPMN 无数据 → 回退 wf_node_detail_perm 表")
	void detailPermFallsBackToDbWhenBpmnEmpty() {
		WfNodeDetailPerm row = new WfNodeDetailPerm();
		row.setDtIndex(1);
		row.setCanAdd(1);
		row.setCanEdit(0);
		when(detailPermMapper.selectList(any())).thenReturn(List.of(row));
		when(bpmnReader.detailTablePerms(1L, "Task_1")).thenReturn(List.of());

		List<DetailPermVO> result = service.getDetailPerm(1L, "Task_1");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDtIndex()).isEqualTo(1);
		assertThat(result.get(0).getCanAdd()).isEqualTo(1);
	}

	@Test
	@DisplayName("detailFilter 开关开 + BPMN 无数据 → 回退 wf_node_detail_filter 表")
	void detailFilterFallsBackToDbWhenBpmnEmpty() {
		WfNodeDetailFilter row = new WfNodeDetailFilter();
		row.setDtIndex(1);
		row.setFieldName("amount");
		row.setModeType(1);
		when(detailFilterMapper.selectList(any())).thenReturn(List.of(row));
		when(bpmnReader.detailFilters(1L, "Task_1")).thenReturn(List.of());

		List<DetailFilterVO> result = service.getDetailFilter(1L, "Task_1", 1);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFieldName()).isEqualTo("amount");
	}
}
