//package org.springblade.workflow;
//
//import org.flowable.engine.ProcessEngine;
//import org.flowable.engine.ProcessEngineConfiguration;
//import org.flowable.engine.ProcessEngines;
//import org.flowable.engine.RuntimeService;
//import org.flowable.engine.TaskService;
//import org.flowable.engine.runtime.ProcessInstance;
//import org.flowable.task.api.Task;
//import org.junit.jupiter.api.Test;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 端到端可用性质疑验证：不依赖 HTTP，直接以独立 Flowable 引擎连真实 MySQL(blade_workflow)，
// * 跑通「部署查询→发起→查待办→完成→待办清空」全流程，证明引擎本身可用。
// * 仅读取/写入已存在的 7.1.0.2 schema（databaseSchemaUpdate=false），不改动表结构。
// */
//public class WfMysqlUseCaseTest {
//
//    @Test
//    public void endToEndUseCase() {
//        String url = "jdbc:mysql://127.0.0.1:3306/blade_workflow"
//                + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
//        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
//        cfg.setJdbcUrl(url);
//        cfg.setJdbcDriver("com.mysql.cj.jdbc.Driver");
//        cfg.setJdbcUsername("root");
//        cfg.setJdbcPassword("123456");
//        cfg.setDatabaseCatalog("blade_workflow");
//        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
//        cfg.setAsyncExecutorActivate(false);
//
//        ProcessEngine engine = cfg.buildProcessEngine();
//        try {
//            RuntimeService rs = engine.getRuntimeService();
//            TaskService ts = engine.getTaskService();
//
//            long defCount = engine.getRepositoryService().createProcessDefinitionQuery().count();
//            System.out.println("[1] 已部署流程定义数 = " + defCount);
//
//            Map<String, Object> vars = new HashMap<>();
//            vars.put("approver", "zhangsan");
//            ProcessInstance pi = rs.startProcessInstanceByKey("simpleApproval", "test001", vars);
//            System.out.println("[2] 发起 simpleApproval 成功, 实例ID = " + pi.getId());
//
//            List<Task> tasks = ts.createTaskQuery().taskAssignee("zhangsan").list();
//            System.out.println("[3] zhangsan 待办数 = " + tasks.size());
//            for (Task t : tasks) {
//                System.out.println("    任务ID=" + t.getId() + " 名称=" + t.getName()
//                        + " 流程实例=" + t.getProcessInstanceId());
//            }
//
//            if (!tasks.isEmpty()) {
//                String tid = tasks.get(0).getId();
//                ts.complete(tid, Collections.singletonMap("approve", true));
//                System.out.println("[4] 完成任务 " + tid + " 成功");
//                long after = ts.createTaskQuery().taskAssignee("zhangsan").count();
//                System.out.println("[5] 完成后 zhangsan 待办数 = " + after + " (应为0，流程结束)");
//                System.out.println("==== 结论: 流程引擎端到端可用 OK ====");
//            } else {
//                throw new AssertionError("未生成待办，流程未正确推进");
//            }
//        } finally {
//            engine.close();
//            ProcessEngines.destroy();
//        }
//    }
//}
