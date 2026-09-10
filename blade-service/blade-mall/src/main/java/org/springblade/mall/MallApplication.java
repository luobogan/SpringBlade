package org.springblade.mall;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.mall.constant.MallConstant;

/**
 * 商城服务启动类
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class MallApplication {

    public static void main(String[] args) {
        BladeApplication.run(MallConstant.APPLICATION_MALL_NAME, MallApplication.class, args);
    }

}



