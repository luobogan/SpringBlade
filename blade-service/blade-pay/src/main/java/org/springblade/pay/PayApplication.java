package org.springblade.pay;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.pay.constant.PayConstant;

/**
 * 支付服务启动类
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class PayApplication {

    public static void main(String[] args) {
        BladeApplication.run(PayConstant.APPLICATION_PAY_NAME, PayApplication.class, args);
    }

}
