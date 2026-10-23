package org.springblade.doc;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springblade.core.launch.BladeApplication;
import org.springblade.doc.constant.DocConstant;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 内容引擎服务启动类（DocEngine）。
 *
 * <p>服务经 {@code @BladeCloudApplication} 自动注册 Nacos（应用名 {@value DocConstant#APPLICATION_DOC_NAME}）。</p>
 */
@BladeCloudApplication
// 预防性：显式 Feign 扫描，确保阶段3集成（流程/公文/用户中心 Feign）可被注入
@EnableFeignClients(basePackages = {"org.springblade"})
@ComponentScan(basePackages = "org.springblade")
public class DocApplication {

    public static void main(String[] args) {
        BladeApplication.run(DocConstant.APPLICATION_DOC_NAME, DocApplication.class, args);
    }

}
