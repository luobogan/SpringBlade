package org.springblade.mall;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.core.launch.constant.AppConstant;

/**
 * 商城服务启动类
 */
@BladeCloudApplication
public class MallApplication {

    public static void main(String[] args) {
        BladeApplication.run(AppConstant.APPLICATION_MALL_NAME, MallApplication.class, args);
    }

}



