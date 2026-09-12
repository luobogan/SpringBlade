package org.springblade.workflow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 发起流程请求
 *
 * <p>对齐文档 §6.3 {@code POST /instance/start}：发起流程（formId + dataId + 流程定义）→ 写快照。</p>
 * <p>同时兼容原 {@code blade-flow} 契约 {@code createWorkflowRequest(workflowId, dataId, fieldValues)}
 * 的入参语义（dataId + fieldValues），便于存量触发表对接。</p>
 *
 * <p>业务主键 {@code bizKey} 由服务端按 {@code formId:dataId} 生成，不在此处传递。</p>
 */
@Data
public class StartProcessDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID（wf_process_definition.id）
     * <p>为空时按 procKey 取最新已发布版本</p>
     */
    private Long defId;

    /**
     * 流程定义Key（wf_process_definition.proc_key，= BPMN process id）
     */
    private String procKey;

    /**
     * 表单ID（workflow_bill.id）
     */
    private Long formId;

    /**
     * 业务数据ID（formtable_main_{formId}.id）
     */
    private Long dataId;

    /**
     * 流程标题
     */
    private String title;

    /**
     * 发起人
     */
    private Long starter;

    /**
     * 紧急程度 0/1/2（对齐 ecology requestlevel）
     */
    private Integer urgency;

    /**
     * 表单字段值（用于写 wf_form_snapshot 数据快照；兼容原契约 fieldValues）
     */
    private Map<String, Object> fieldValues;

    /**
     * 流程变量（含 ${approver} 等引擎变量）
     */
    private Map<String, Object> variables;

    /**
     * 父流程实例ID（发起子流程时回填；普通发起为空）
     */
    private Long parentId;

}
