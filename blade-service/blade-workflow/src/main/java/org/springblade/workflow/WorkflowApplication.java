package org.springblade.workflow;

import org.springblade.core.cloud.client.BladeCloudApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springblade.core.launch.BladeApplication;
import org.springblade.workflow.constant.WorkflowConstant;

/**
 * 审批流程服务启动类
 */
@BladeCloudApplication
@ComponentScan(basePackages = "org.springblade")
public class WorkflowApplication {

    public static void main(String[] args) {
        BladeApplication.run(WorkflowConstant.APPLICATION_WORKFLOW_NAME, WorkflowApplication.class, args);
    }

}
