package org.springblade.portal;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.portal.constant.PortalConstant;
import org.springframework.context.annotation.ComponentScan;

/**
 * 配置驱动门户服务启动类
 * 对标泛微 E-cology 门户引擎（homepage/hpelement 配置体系）
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class PortalApplication {

    public static void main(String[] args) {
        BladeApplication.run(PortalConstant.APPLICATION_PORTAL_NAME, PortalApplication.class, args);
    }

}
