package org.springblade.mall.controller;

import org.springblade.mall.entity.Logistics;
import org.springblade.mall.service.LogisticsService;
import org.springblade.mall.vo.LogisticsVO;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 物流信息Controller
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/admin/logistics")
@Tag(name = "物流信息管理", description = "物流信息管理")
public class LogisticsController extends BladeController {

    private LogisticsService logisticsService;

    /**
     * 获取物流信息
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取物流信息", description = "根据订单ID获取物流信息")
    public R<LogisticsVO> getLogisticsByOrderId(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        try {
            LogisticsVO logisticsVO = logisticsService.getLogisticsByOrderId(orderId);
            return R.data(logisticsVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 发货
     */
    @PostMapping("/ship")
    @Operation(summary = "发货", description = "订单发货操作")
    public R<Logistics> shipOrder(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "物流单号") @RequestParam String trackingNo,
            @Parameter(description = "物流公司") @RequestParam String logisticsCompany) {
        try {
            Logistics logistics = logisticsService.shipOrder(orderId, trackingNo, logisticsCompany);
            return R.data(logistics);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新物流状态
     */
    @PutMapping("/status")
    @Operation(summary = "更新物流状态", description = "更新物流状态和最新信息")
    public R<?> updateLogisticsStatus(
            @Parameter(description = "物流ID") @RequestParam Long logisticsId,
            @Parameter(description = "物流状态") @RequestParam String status,
            @Parameter(description = "最新物流信息") @RequestParam String latestInfo) {
        try {
            logisticsService.updateLogisticsStatus(logisticsId, status, latestInfo);
            return R.success();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}


