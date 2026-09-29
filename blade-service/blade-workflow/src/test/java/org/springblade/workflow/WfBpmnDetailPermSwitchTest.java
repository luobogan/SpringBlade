package org.springblade.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-5：detail-perm / detail-filter 开关「读 BPMN」分支的回归测试（纯函数，无 Spring 容器）。
 *
 * <p>覆盖 {@link WfBpmnExtensionReader#toDetailPermVOs}（明细表整表权限，对应 {@code wf_node_detail_perm}）
 * 与 {@link WfBpmnExtensionReader#toDetailFilterVOs}（明细表字段筛选，对应 {@code wf_node_detail_filter}）：
 * 全字段映射、dtIndex 升序、modeType 过滤、空/脏数据容错。</p>
 */
class WfBpmnDetailPermSwitchTest {

	private static BpmnExtensionUtil.WfDetailTablePermExt tablePerm(String dtIndex, String canAdd, String openPaging) {
		BpmnExtensionUtil.WfDetailTablePermExt d = new BpmnExtensionUtil.WfDetailTablePermExt();
		d.dtIndex = dtIndex;
		d.canAdd = canAdd;
		d.openPaging = openPaging;
		return d;
	}

	private static BpmnExtensionUtil.WfDetailFilterExt filter(String dtIndex, String modeType,
	                                                         String fieldName, String compareType, String isRequired) {
		BpmnExtensionUtil.WfDetailFilterExt d = new BpmnExtensionUtil.WfDetailFilterExt();
		d.dtIndex = dtIndex;
		d.modeType = modeType;
		d.fieldName = fieldName;
		d.compareType = compareType;
		d.isRequired = isRequired;
		return d;
	}

	@Test
	@DisplayName("detailTablePerm：全字段映射 + 按 dtIndex 升序")
	void detailTablePermMapsAndSorts() {
		BpmnExtensionUtil.WfDetailTablePermExt d = tablePerm("1", "1", "1");
		d.canEdit = "0";
		d.canDelete = "1";
		d.hideEmpty = "0";
		d.defaultRows = "3";
		d.required = "1";
		d.printSerial = "0";
		d.allowScroll = "1";

		List<DetailPermVO> r = WfBpmnExtensionReader.toDetailPermVOs(
			List.of(tablePerm("2", "0", "0"), d, tablePerm("1", "1", "1")));
		assertEquals(3, r.size());
		// 升序：dtIndex 1,1,2
		assertEquals(List.of(1, 1, 2), r.stream().map(DetailPermVO::getDtIndex).toList());
		DetailPermVO w = r.get(0);
		assertEquals(Integer.valueOf(1), w.getCanAdd());
		assertEquals(Integer.valueOf(0), w.getCanEdit());
		assertEquals(Integer.valueOf(1), w.getCanDelete());
		assertEquals(Integer.valueOf(3), w.getDefaultRows());
		assertEquals(Integer.valueOf(1), w.getRequired());
		assertEquals(Integer.valueOf(0), w.getPrintSerial());
		assertEquals(Integer.valueOf(1), w.getAllowScroll());
		assertEquals(Integer.valueOf(1), w.getOpenPaging());
	}

	@Test
	@DisplayName("detailFilter：modeType 过滤 + 按 dtIndex/fieldName 升序 + 全字段映射")
	void detailFilterFiltersByModeTypeAndSorts() {
		List<BpmnExtensionUtil.WfDetailFilterExt> exts = List.of(
			filter("2", "1", "b", "3", "1"),
			filter("1", "2", "a", "1", "0"), // modeType=2 应被排除
			filter("1", "1", "a", "2", "1")
		);
		List<DetailFilterVO> r = WfBpmnExtensionReader.toDetailFilterVOs(1, exts);
		assertEquals(2, r.size(), "modeType=2 的规则应被过滤掉");
		assertEquals(List.of("a", "b"), r.stream().map(DetailFilterVO::getFieldName).toList());
		assertEquals(List.of(1, 2), r.stream().map(DetailFilterVO::getDtIndex).toList());
		assertEquals(Integer.valueOf(2), r.get(0).getCompareType());
		assertEquals(Integer.valueOf(1), r.get(0).getIsRequired());
		assertEquals("b", r.get(1).getFieldName());
	}

	@Test
	@DisplayName("detailFilter：modeType 为 null 时返回全部（不过滤）")
	void detailFilterNullModeTypeReturnsAll() {
		List<BpmnExtensionUtil.WfDetailFilterExt> exts = List.of(
			filter("1", "1", "a", "1", "0"),
			filter("1", "2", "b", "2", "1")
		);
		assertEquals(2, WfBpmnExtensionReader.toDetailFilterVOs(null, exts).size());
	}

	@Test
	@DisplayName("空/脏数据容错：null 入参、空列表、非数字 dtIndex")
	void toleratesNullAndDirty() {
		assertTrue(WfBpmnExtensionReader.toDetailPermVOs(null).isEmpty());
		assertTrue(WfBpmnExtensionReader.toDetailPermVOs(List.of()).isEmpty());
		assertTrue(WfBpmnExtensionReader.toDetailFilterVOs(1, null).isEmpty());

		BpmnExtensionUtil.WfDetailTablePermExt bad = tablePerm("x", "1", "0");
		List<DetailPermVO> r = WfBpmnExtensionReader.toDetailPermVOs(List.of(bad));
		assertEquals(1, r.size());
		assertNull(r.get(0).getDtIndex(), "非数字 dtIndex 应容错为 null");
	}
}
