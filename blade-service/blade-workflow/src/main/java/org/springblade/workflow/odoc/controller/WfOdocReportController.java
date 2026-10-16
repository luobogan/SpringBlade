package org.springblade.workflow.odoc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.service.IWfOdocReportService;
import org.springblade.workflow.odoc.vo.OdocStatisticVO;
import org.springblade.workflow.odoc.vo.OdocTodoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公文统计 / 流程监控控制器（对应 E9 {@code OdocReportAction} + {@code OdocMonitorListAction} 列表部分）。
 *
 * <p>监控管控动作（删除/归档/暂停/打开/取消/收回）复用通用 {@code WfInstanceController}（流程管理员）。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文统计/监控", description = "公文统计(按类型) + 在途公文监控列表")
public class WfOdocReportController {

	private final IWfOdocReportService reportService;

	@GetMapping("/report/statistics")
	@Operation(summary = "公文统计", description = "按公文类型汇总在途/办结等；officalType 可收敛单类型")
	public R<List<OdocStatisticVO>> statistics(@RequestParam(required = false) Integer officalType) {
		return R.data(reportService.statistics(officalType));
	}

	@GetMapping("/monitor/list")
	@Operation(summary = "在途公文监控列表", description = "仅流程管理员；管控动作复用 /instance/**")
	public R<IPage<OdocTodoVO>> monitorList(OdocListQuery query) {
		return R.data(reportService.monitorPage(query));
	}

}
