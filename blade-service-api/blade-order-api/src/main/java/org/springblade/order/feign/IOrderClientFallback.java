package org.springblade.order.feign;

import org.springblade.core.tool.api.R;
import org.springblade.order.vo.OrderVO;
import org.springframework.stereotype.Component;

@Component
public class IOrderClientFallback implements IOrderClient {

    @Override
    public R<OrderVO> getByOrderNo(String orderNo) {
        return R.fail("订单服务不可用");
    }

    @Override
    public R<Boolean> updatePayStatus(String orderNo, String paymentNo) {
        return R.fail("订单服务不可用");
    }

    @Override
    public R<Boolean> cancelOrder(String orderNo) {
        return R.fail("订单服务不可用");
    }
}
