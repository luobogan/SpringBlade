package org.springblade.formmode;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.formmode.constant.FormmodeConstant;

/**
 * 表单建模服务启动类
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class FormmodeApplication {

    public static void main(String[] args) {
        BladeApplication.run(FormmodeConstant.APPLICATION_FORMMODE_NAME, FormmodeApplication.class, args);
    }

}
