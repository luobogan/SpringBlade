/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.WechatPayRequestDTO;
import org.springblade.mall.dto.WechatPayResponseDTO;
import org.springblade.mall.service.WechatPayService;
import org.springframework.web.bind.annotation.*;

/**
 * 微信支付控制器
 * 提供微信支付统一下单、查询等接口
 *
 * @author youpinmall
 * @date 2026-03-05
 */
@Tag(name = "微信支付", description = "微信支付相关接口")
@RestController
@RequestMapping("/pay/wechat")
@RequiredArgsConstructor
public class PayController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(PayController.class);

    private final WechatPayService wechatPayService;

    /**
     * 创建微信支付订单
     */
    @PostMapping("/unified-order")
    @Operation(summary = "创建微信支付订单", description = "调用微信支付统一下单接口，返回前端支付参数")
    public R<WechatPayResponseDTO> createWechatPayOrder(
            @Parameter(description = "支付请求信息") @Valid @RequestBody WechatPayRequestDTO requestDTO, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        log.info("收到微信支付请求，userId={}, orderNo={}, amount={}, tradeType={}",
                user.getUserId(), requestDTO.getOrderNo(), requestDTO.getAmount(), requestDTO.getTradeType());

        // 调用微信支付服务
        WechatPayResponseDTO responseDTO = wechatPayService.createWechatPayOrder(user.getUserId(), requestDTO);

        return R.data(responseDTO, "创建支付订单成功");
    }

    /**
     * 查询微信支付订单状态
     */
    @GetMapping("/status/{paymentNo}")
    @Operation(summary = "查询微信支付订单状态", description = "查询微信支付订单的支付状态")
    public R<String> queryWechatPayStatus(
            @Parameter(description = "支付单号") @PathVariable String paymentNo) {
        try {
            String status = wechatPayService.queryWechatPayStatus(paymentNo);
            return R.data(status);
        } catch (Exception e) {
            log.error("查询微信支付状态失败，paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 微信支付退款
     */
    @PostMapping("/refund")
    @Operation(summary = "微信支付退款", description = "发起微信支付退款申请")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R<String> wechatRefund(
            @Parameter(description = "支付单号") @RequestParam String paymentNo,
            @Parameter(description = "退款金额（元）") @RequestParam java.math.BigDecimal refundAmount,
            @Parameter(description = "退款原因") @RequestParam(required = false) String reason, BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            String refundNo = wechatPayService.wechatRefund(paymentNo, refundAmount, reason);
            return R.data(refundNo, "退款申请成功");
        } catch (Exception e) {
            log.error("微信退款失败，paymentNo={}", paymentNo, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询微信退款状态
     */
    @GetMapping("/refund/status/{refundNo}")
    @Operation(summary = "查询微信退款状态", description = "查询微信退款申请的退款状态")
    public R<String> queryWechatRefundStatus(
            @Parameter(description = "退款单号") @PathVariable String refundNo) {
        try {
            String status = wechatPayService.queryWechatRefundStatus(refundNo);
            return R.data(status);
        } catch (Exception e) {
            log.error("查询微信退款状态失败，refundNo={}", refundNo, e);
            return R.fail(e.getMessage());
        }
    }


}



