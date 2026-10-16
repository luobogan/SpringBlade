package org.springblade.workflow.odoc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.dto.OdocSuperviseQuery;
import org.springblade.workflow.odoc.dto.OdocSuperviseSaveDTO;
import org.springblade.workflow.odoc.entity.WfOdocSupervise;
import org.springblade.workflow.odoc.entity.WfOdocSuperviseSetting;
import org.springblade.workflow.odoc.service.IWfOdocSuperviseService;
import org.springblade.workflow.odoc.vo.OdocSuperviseVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公文督办控制器（对应 E9 {@code OdocSuperviceAction}）。
 *
 * <p>路径与 {@link WfOdocController} 同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文督办", description = "督办设置/列表/计数/发起/提醒")
public class WfOdocSuperviseController {

	private final IWfOdocSuperviseService superviseService;

	@GetMapping("/supervise/setting")
	@Operation(summary = "督办设置", description = "按流程定义取")
	public R<WfOdocSuperviseSetting> getSetting(@RequestParam Long defId) {
		return R.data(superviseService.getSetting(defId));
	}

	@PostMapping("/supervise/setting")
	@Operation(summary = "保存督办设置", description = "按 defId 幂等 upsert")
	public R<WfOdocSuperviseSetting> saveSetting(@RequestBody WfOdocSuperviseSetting setting) {
		return R.data(superviseService.saveSetting(setting));
	}

	@GetMapping("/supervise/list")
	@Operation(summary = "督办列表", description = "superviseType 0我发起的督办 / 1我被督办")
	public R<IPage<OdocSuperviseVO>> list(OdocSuperviseQuery query) {
		return R.data(superviseService.page(query));
	}

	@GetMapping("/supervise/count")
	@Operation(summary = "督办计数", description = "督办中数量（角标）")
	public R<Long> count(OdocSuperviseQuery query) {
		return R.data(superviseService.count(query));
	}

	@PostMapping("/supervise")
	@Operation(summary = "发起督办", description = "对在途实例发起督办")
	public R<WfOdocSupervise> add(@RequestBody OdocSuperviseSaveDTO dto) {
		return R.data(superviseService.add(dto));
	}

	@PostMapping("/supervise/{id}/remind")
	@Operation(summary = "督办提醒", description = "记录最近提醒时间")
	public R<Boolean> remind(@PathVariable Long id) {
		return R.data(superviseService.remind(id));
	}

}
