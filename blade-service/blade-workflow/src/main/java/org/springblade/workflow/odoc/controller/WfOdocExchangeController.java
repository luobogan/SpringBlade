package org.springblade.workflow.odoc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.dto.OdocExchangeSendDTO;
import org.springblade.workflow.odoc.entity.WfOdocExchangeInner;
import org.springblade.workflow.odoc.entity.WfOdocExchangeSetting;
import org.springblade.workflow.odoc.service.IWfOdocExchangeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公文交换控制器（对应 E9 {@code OdocInnerChangeAction} + {@code OdocExchangeWorkflowAction}）。
 *
 * <p>路径与 {@link WfOdocController} 同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文交换", description = "内部交换(发送/列表/接收) + 外部交换(设置/适配器)")
public class WfOdocExchangeController {

    private final IWfOdocExchangeService exchangeService;

    // ===================== 内部交换 =====================

    @PostMapping("/exchange/inner/send")
    @Operation(summary = "内部交换发送", description = "把公文推送到目标机构")
    public R<List<WfOdocExchangeInner>> sendInner(@RequestBody OdocExchangeSendDTO dto) {
        return R.data(exchangeService.sendInner(dto));
    }

    @GetMapping("/exchange/inner/list")
    @Operation(summary = "内部交换列表", description = "我发出的交换记录")
    public R<IPage<WfOdocExchangeInner>> innerList(@RequestParam(required = false) Integer direction,
                                                   @RequestParam(required = false) Integer officalType,
                                                   @RequestParam(required = false) Long current,
                                                   @RequestParam(required = false) Long size) {
        return R.data(exchangeService.pageInner(direction, officalType, current, size));
    }

    @PostMapping("/exchange/inner/{id}/receive")
    @Operation(summary = "内部交换标记已接收")
    public R<Boolean> receiveInner(@PathVariable Long id) {
        return R.data(exchangeService.receiveInner(id));
    }

    // ===================== 外部交换 =====================

    @GetMapping("/exchange/external/available")
    @Operation(summary = "外部交换是否已接入", description = "前端据此显示/隐藏外部交换入口")
    public R<Boolean> externalAvailable() {
        return R.data(exchangeService.externalAvailable());
    }

    @GetMapping("/exchange/external/setting")
    @Operation(summary = "外部交换平台设置列表")
    public R<List<WfOdocExchangeSetting>> listSetting() {
        return R.data(exchangeService.listSetting());
    }

    @PostMapping("/exchange/external/setting")
    @Operation(summary = "保存外部交换平台设置", description = "按 platformCode 幂等 upsert")
    public R<WfOdocExchangeSetting> saveSetting(@RequestBody WfOdocExchangeSetting setting) {
        return R.data(exchangeService.saveSetting(setting));
    }

    @DeleteMapping("/exchange/external/setting/{id}")
    @Operation(summary = "删除外部交换平台设置")
    public R<Boolean> removeSetting(@PathVariable Long id) {
        return R.data(exchangeService.removeSetting(id));
    }

    @PostMapping("/exchange/external/send")
    @Operation(summary = "外部交换发送", description = "委派 IOdocExchangeClient 适配器；未接入明确报错")
    public R<String> sendExternal(@RequestParam String platformCode, @RequestParam Long instanceId) {
        return R.data(exchangeService.sendExternal(platformCode, instanceId), "已发送");
    }

}
