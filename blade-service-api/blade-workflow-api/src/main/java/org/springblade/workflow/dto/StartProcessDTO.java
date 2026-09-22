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
     * 引擎启动用的流程 key（默认取定义的 procKey）。
     *
     * <p>测试态传 {@code procKey + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX}：测试部署使用独立 key，
     * 避免「每次测试部署都把正式版本顶成最新」（Flowable 按 key 取最新部署）。</p>
     */
    private String engineKey;

    /**
     * 精确指定的引擎流程定义ID（{@code ACT_RE_PROCDEF.ID_}）—— 优先级**高于** {@link #engineKey}。
     *
     * <p>非空时发起走 {@code startProcessInstanceById}：与「哪个部署最新」彻底解耦，
     * 是「绝对不会跑错版本」的技术根（方案 §3），也是灰度路由的落点（§4.3）。
     * 为空则回退按 key 启动（存量定义 / 尚未回写 procDefId 时保持既有行为）。</p>
     */
    private String procDefId;

    /**
     * 表单ID（workflow_bill.id）
     */
    private Long formId;

    /**
     * 业务数据ID（formtable_main_{formId}.id）
     */
    private Long dataId;

    /**
     * 草稿实例ID：非空时表示「提交草稿」——把该草稿实例<b>原地提升</b>为正式运行实例，
     * 复用其 {@code wf_instance} 行与业务数据行（避免 {@code uk_biz_key(formId:dataId)} 唯一键冲突），
     * 不新建实例；草稿期合成的待办（engineTaskId 为空）由服务端删除后由引擎重新生成。
     */
    private Long draftInstId;

    /**
     * 流程标题
     */
    private String title;

    /**
     * 发起人
     */
    private Long starter;

    /**
     * 发起人签字意见（富文本 HTML；写入流转意见第一条「提交」记录）
     */
    private String opinion;

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

    /**
     * 测试态标记：为 true 时本次发起的实例/待办打 is_test=1，可一键清理，且不触发节点附加操作/子流程副作用
     */
    private Boolean testFlag;

    /**
     * 测试临时部署ID（Flowable deploymentId），用于测试清理时级联卸载
     */
    private String testDeploymentId;

    /**
     * 是否「单据发起」（由业务表单/触发表驱动发起，业务行由单据侧维护与流程的关联）。
     *
     * <p>为 true 时，发起成功后<b>不</b>回填 {@code request_id}（避免覆盖单据侧已维护的关联，
     * 也避免对没有 request_id 列的存量单据表产生误报）。为 false/空 时，凡业务行由本侧创建
     * （含「先保存草稿再提交」复用 {@code /form/save} 返回的业务行），都会回填 request_id 以保证
     * 单据 ↔ 流程双向反查可用。</p>
     */
    private Boolean billInitiated;

}
