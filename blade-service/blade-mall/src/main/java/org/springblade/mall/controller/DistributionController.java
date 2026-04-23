package org.springblade.mall.controller;

import org.springblade.mall.dto.BatchImportDTO;
import org.springblade.mall.dto.BatchUpdateDTO;
import org.springblade.mall.entity.SalesChannel;
import org.springblade.mall.service.DistributionService;
import org.springblade.mall.vo.ProductChannelVO;
import org.springblade.mall.vo.ProductVO;
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

import java.util.List;
import java.util.Map;

/**
 * 铺货商品管理控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/admin/distribution")
@Tag(name = "铺货商品管理", description = "铺货商品管理")
public class DistributionController extends BladeController {

    private DistributionService distributionService;

    @PostMapping("/batch-import")
    @Operation(summary = "批量导入商品", description = "批量导入商品")
    public R<List<ProductVO>> batchImport(@Parameter(description = "批量导入信息") @RequestBody BatchImportDTO dto) {
        try {
            List<ProductVO> products = distributionService.batchImport(dto);
            R<List<ProductVO>> result = R.data(products);
            result.setMsg("批量导入成功，共导入 " + products.size() + " 个商品");
            return result;
        } catch (Exception e) {
            return R.fail("批量导入失败: " + e.getMessage());
        }
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量更新商品", description = "批量更新商品")
    public R<?> batchUpdate(@Parameter(description = "批量更新信息") @RequestBody BatchUpdateDTO dto) {
        try {
            distributionService.batchUpdate(dto);
            return R.success("批量更新成功");
        } catch (Exception e) {
            return R.fail("批量更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch-delete")
    @Operation(summary = "批量删除商品", description = "批量删除商品")
    public R<?> batchDelete(@Parameter(description = "商品ID列表") @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> productIds = request.get("productIds");
            distributionService.batchDelete(productIds);
            return R.success("批量删除成功");
        } catch (Exception e) {
            return R.fail("批量删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/channels")
    @Operation(summary = "获取销售渠道列表", description = "获取销售渠道列表")
    public R<List<SalesChannel>> getChannels() {
        try {
            List<SalesChannel> channels = distributionService.getChannels();
            return R.data(channels);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/channels")
    @Operation(summary = "创建销售渠道", description = "创建销售渠道")
    public R<SalesChannel> createChannel(@Parameter(description = "销售渠道信息") @RequestBody SalesChannel channel) {
        try {
            SalesChannel created = distributionService.createChannel(channel);
            R<SalesChannel> result = R.data(created);
            result.setMsg("渠道创建成功");
            return result;
        } catch (Exception e) {
            return R.fail("渠道创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/channels/{id}/status")
    @Operation(summary = "更新渠道状态", description = "更新渠道状态")
    public R<?> updateChannelStatus(
            @Parameter(description = "渠道ID") @PathVariable Long id,
            @Parameter(description = "状态信息") @RequestBody Map<String, Integer> request) {
        try {
            distributionService.updateChannelStatus(id, request.get("status"));
            return R.success("渠道状态更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/products/{productId}/channels")
    @Operation(summary = "获取商品渠道列表", description = "获取商品渠道列表")
    public R<List<ProductChannelVO>> getProductChannels(@Parameter(description = "商品ID") @PathVariable Long productId) {
        try {
            List<ProductChannelVO> channels = distributionService.getProductChannels(productId);
            return R.data(channels);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/products/{productId}/channels/{channelId}")
    @Operation(summary = "商品上架到渠道", description = "商品上架到渠道")
    public R<?> publishToChannel(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "渠道ID") @PathVariable Long channelId) {
        try {
            distributionService.publishToChannel(productId, channelId);
            return R.success("商品已上架到渠道");
        } catch (Exception e) {
            return R.fail("上架失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{productId}/channels/{channelId}")
    @Operation(summary = "商品从渠道下架", description = "商品从渠道下架")
    public R<?> unpublishFromChannel(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "渠道ID") @PathVariable Long channelId) {
        try {
            distributionService.unpublishFromChannel(productId, channelId);
            return R.success("商品已从渠道下架");
        } catch (Exception e) {
            return R.fail("下架失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-publish")
    @Operation(summary = "批量上架商品到渠道", description = "批量上架商品到渠道")
    public R<?> batchPublishToChannels(@Parameter(description = "批量上架信息") @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> productIds = request.get("productIds");
            List<Long> channelIds = request.get("channelIds");
            distributionService.batchPublishToChannels(productIds, channelIds);
            return R.success("批量上架成功");
        } catch (Exception e) {
            return R.fail("批量上架失败: " + e.getMessage());
        }
    }

    @PostMapping("/products/{productId}/channels/{channelId}/sync")
    @Operation(summary = "同步商品到渠道", description = "同步商品到渠道")
    public R<?> syncChannelProduct(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "渠道ID") @PathVariable Long channelId) {
        try {
            distributionService.syncChannelProduct(productId, channelId);
            return R.success("商品信息已同步");
        } catch (Exception e) {
            return R.fail("同步失败: " + e.getMessage());
        }
    }
}



