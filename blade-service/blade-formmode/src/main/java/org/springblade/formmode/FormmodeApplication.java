package org.springblade.formmode;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.formmode.constant.FormmodeConstant;

/**
 * 表单建模服务启动类
 */
@BladeCloudApplication
public class FormmodeApplication {

    public static void main(String[] args) {
        BladeApplication.run(FormmodeConstant.APPLICATION_FORMMODE_NAME, FormmodeApplication.class, args);
    }

}
