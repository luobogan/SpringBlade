package org.springblade.order;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.order.constant.OrderConstant;

/**
 * 订单服务启动类
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class OrderApplication {

    public static void main(String[] args) {
        BladeApplication.run(OrderConstant.APPLICATION_ORDER_NAME, OrderApplication.class, args);
    }

}
