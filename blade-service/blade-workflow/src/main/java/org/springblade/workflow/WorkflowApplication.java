package org.springblade.workflow;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 审批流程服务启动类
 */
@BladeCloudApplication
@EnableScheduling
// 预防性修复：显式声明 Feign 客户端扫描范围，确保扫到 api 模块（org.springblade.*.feign）里的
// @FeignClient 接口（IFormmodeClient / IUserClient 等）及其 @Component fallback。
// 仅依赖 @BladeCloudApplication 的元注解时，若注册未生效会导致注入点报 "bean of type ... not found"。
@EnableFeignClients(basePackages = {"org.springblade"})
@ComponentScan(basePackages = "org.springblade")
public class WorkflowApplication {

    public static void main(String[] args) {
        BladeApplication.run(WorkflowConstant.APPLICATION_WORKFLOW_NAME, WorkflowApplication.class, args);
    }

}
