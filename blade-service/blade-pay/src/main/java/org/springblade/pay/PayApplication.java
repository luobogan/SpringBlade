package org.springblade.pay;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.core.launch.constant.AppConstant;

/**
 * 支付服务启动类
 */
@BladeCloudApplication
public class PayApplication {

    public static void main(String[] args) {
        BladeApplication.run(AppConstant.APPLICATION_PAY_NAME, PayApplication.class, args);
    }

}
