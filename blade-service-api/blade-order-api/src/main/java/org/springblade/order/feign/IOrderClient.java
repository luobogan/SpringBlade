package org.springblade.order.feign;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.order.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    value = AppConstant.APPLICATION_ORDER_NAME,
    fallback = IOrderClientFallback.class
)
public interface IOrderClient {

    String API_PREFIX = "/client/order";

    String GET_BY_ORDER_NO = API_PREFIX + "/get-by-order-no";
    String UPDATE_PAY_STATUS = API_PREFIX + "/update-pay-status";
    String CANCEL_ORDER = API_PREFIX + "/cancel";

    @GetMapping(GET_BY_ORDER_NO)
    R<OrderVO> getByOrderNo(@RequestParam("orderNo") String orderNo);

    @PostMapping(UPDATE_PAY_STATUS)
    R<Boolean> updatePayStatus(
        @RequestParam("orderNo") String orderNo,
        @RequestParam("paymentNo") String paymentNo
    );

    @PostMapping(CANCEL_ORDER)
    R<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo);
}
