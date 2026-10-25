package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealStrategyHolder;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springblade.workflow.odoc.vo.OdocSealConfigVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 公文通用设置控制器（单例）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文通用设置", description = "模块级开关 + 新建流程默认值 + 电子签章方案")
public class WfOdocSettingController {

	private final IWfOdocSettingService settingService;

	private final OdocSealStrategyHolder strategyHolder;

	@GetMapping("/setting")
	@Operation(summary = "取通用设置", description = "无落库行时返回默认值")
	public R<WfOdocSetting> get() {
		return R.data(settingService.get());
	}

	@PostMapping("/setting")
	@Operation(summary = "保存通用设置", description = "单例 upsert")
	public R<WfOdocSetting> save(@RequestBody WfOdocSetting setting) {
		return R.data(settingService.save(setting));
	}

	@GetMapping("/setting/seal")
	@Operation(summary = "取电子签章方案配置", description = "含自研/第三方方案可用性与第三方接入状态")
	public R<OdocSealConfigVO> sealConfig() {
		OdocSealConfigVO vo = settingService.sealConfig();
		vo.setModes(buildModes(vo));
		return R.data(vo);
	}

	@PostMapping("/setting/seal")
	@Operation(summary = "保存电子签章方案配置", description = "只更新签章相关列；第三方 appId/appSecret 仍由 Nacos 维护")
	public R<OdocSealConfigVO> saveSealConfig(@RequestBody OdocSealConfigVO config) {
		OdocSealConfigVO vo = settingService.saveSealConfig(config);
		vo.setModes(buildModes(vo));
		return R.data(vo);
	}

	/** 组装可选方案列表（含不可选原因，供前端禁用与提示） */
	private List<OdocSealConfigVO.Mode> buildModes(OdocSealConfigVO vo) {
		List<OdocSealConfigVO.Mode> list = new ArrayList<>();

		boolean localReady = false;
		for (IOdocSealStrategy strategy : strategyHolder.all()) {
			if (IOdocSealStrategy.MODE_LOCAL.equals(strategy.mode())) {
				localReady = strategy.available();
				break;
			}
		}
		OdocSealConfigVO.Mode local = new OdocSealConfigVO.Mode();
		local.setValue(0);
		local.setLabel("自研印章（本地 CA）");
		local.setSelectable(localReady);
		local.setReason(localReady ? null : "本地签章组件未装配（缺少 CA 组件）");
		list.add(local);

		boolean esignReady = Boolean.TRUE.equals(vo.getEsignReady());
		OdocSealConfigVO.Mode esign = new OdocSealConfigVO.Mode();
		esign.setValue(1);
		esign.setLabel("第三方电子签章（e签宝）");
		esign.setSelectable(esignReady);
		esign.setReason(esignReady ? null : "尚未就绪：需在 Nacos 配置 odoc.esign.enabled / app-id / app-secret");
		list.add(esign);
		return list;
	}
}
