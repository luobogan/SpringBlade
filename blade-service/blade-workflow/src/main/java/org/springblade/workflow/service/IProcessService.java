package org.springblade.workflow.service;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springblade.workflow.vo.TaskVO;

import java.util.List;
import java.util.Map;

/**
 * 流程<b>引擎适配</b>服务（Flowable）
 *
 * <p>职责边界：只做「引擎层面」的原子操作（发起、查任务、完成），
 * <b>不</b>涉及 wf_* 业务语义表。业务语义（实例/任务/日志/权限）由
 * {@link IWfInstanceService}、{@link IWfTaskService} 负责，
 * 以此保证 wf_* 与引擎表解耦，保留替换引擎的演进位。</p>
 */
public interface IProcessService {

    /**
     * 发起引擎流程实例
     *
     * @param procKey   流程定义Key（BPMN process id）
     * @param bizKey    业务主键（formId:dataId）
     * @param variables 流程变量
     * @return 引擎实例ID（PROC_INST_ID_）
     */
    String startInstance(String procKey, String bizKey, Map<String, Object> variables);

    /**
     * 按**流程定义ID**发起引擎流程实例（方案 §3 —— 定义级隔离的技术根）。
     *
     * <p>与 {@link #startInstance} 的区别：后者由引擎解析「该 key 的<b>最新部署</b>」，
     * 版本取决于部署时序（任何一次重新部署都会改变"最新"）；本方法精确绑定
     * {@code ACT_RE_PROCDEF.ID_}，与部署时序彻底解耦 ——
     * 这是「绝对不会跑错版本」的根，也是灰度路由与秒级回滚的基础。</p>
     *
     * @param processDefinitionId 流程定义ID（{@code ACT_RE_PROCDEF.ID_}）
     * @param bizKey              业务主键（formId:dataId）
     * @param variables           流程变量
     * @return 引擎实例ID（PROC_INST_ID_）
     */
    String startInstanceById(String processDefinitionId, String bizKey, Map<String, Object> variables);

    /**
     * 查询指定引擎实例的当前活动任务
     */
    List<TaskVO> currentTasks(String engineInstId);

    /**
     * 按办理人查询引擎任务
     */
    List<TaskVO> queryTasks(String assignee);

    /**
     * 完成引擎任务
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 部署 BPMN 2.0 定义到引擎
     *
     * @param procKey  流程定义Key（BPMN process id，作为部署资源名前缀）
     * @param bpmnXml  BPMN 2.0 XML 文本（bpmn-js 画布产出）
     * @return 引擎部署ID
     */
    String deployProcess(String procKey, String bpmnXml);

    /**
     * 部署 BPMN 2.0 定义到引擎（测试专用：关闭 BPMN 语义校验）。
     *
     * <p>草稿流程在画布中保存时可能含未配置完整的元素（如未配时间的定时事件、
     * 未关联 messageRef 的抛出事件、空 executionListener 等），Flowable 默认语义校验会拒绝部署。
     * 测试态仅需「能跑通主干路径」，故关闭 BPMN 语义校验与 XML Schema 校验，
     * 让草稿可被部署；与 {@link #deployProcess}（生产发布，保留完整校验）区分。</p>
     *
     * @param procKey  流程定义Key（BPMN process id）
     * @param bpmnXml  BPMN 2.0 XML 文本
     * @return 引擎部署ID
     */
    String deployProcessForTest(String procKey, String bpmnXml);

    /**
     * 将引擎实例的当前活动节点跳转到指定节点（「节点信息 → 指定流转」的运行期消费）。
     *
     * <p>用于处理人手动指定下一节点：不按 BPMN 默认流向，直接 move 到 {@code toActivityKey}。
     * 若 {@code variables} 非空，先写入流程变量再跳转。</p>
     *
     * @param engineInstId    引擎实例ID（PROC_INST_ID_）
     * @param fromActivityKey 当前活动节点Key（= BPMN 元素 id）
     * @param toActivityKey   目标活动节点Key
     * @param variables       跳转前写入的流程变量（可为 null）
     */
    void moveActivity(String engineInstId, String fromActivityKey, String toActivityKey, Map<String, Object> variables);

    /**
     * 将引擎实例的当前活动节点<b>并行跳转</b>到多个目标节点（「指定流转·多目标」的运行期消费）。
     *
     * <p>单目标退化为 {@link #moveActivity}；多目标用 {@code moveActivityIdToActivityIds}
     * 一次扇出多组 token（每个目标节点一条并行分支）。</p>
     *
     * @param engineInstId    引擎实例ID
     * @param fromActivityKey 当前活动节点Key
     * @param toActivityKeys  目标活动节点Key 列表（≥1）
     * @param variables       跳转前写入的流程变量（可为 null）
     */
    void moveActivityToActivities(String engineInstId, String fromActivityKey,
                                 List<String> toActivityKeys, Map<String, Object> variables);

    /**
     * 查询引擎实例经历过的<b>历史活动实例</b>（节点 + 流转），按开始时间升序。
     *
     * <p>用于「真实引擎假数据测试」的<b>覆盖率采集</b>：把引擎实际经过的节点集合，
     * 与 {@code wf_process_node} / {@code wf_node_link} 全量集合比对，识别死节点与未验证分支。
     * 排他网关走了哪条分支，由下游被激活的节点反推（无需 history=full 记录 sequenceFlow）。</p>
     *
     * @param engineInstId 引擎实例ID
     * @return 历史活动实例列表（含 startEvent / userTask / endEvent / 网关等）
     */
    List<HistoricActivityInstance> historicActivities(String engineInstId);

    /**
     * 查询引擎实例的历史流程实例（含结束状态、开始/结束时间）。
     *
     * @param engineInstId 引擎实例ID
     * @return 历史流程实例（未结束返回 null）
     */
    HistoricProcessInstance historicProcess(String engineInstId);

    /**
     * 查询引擎中该 procKey 的<b>最新版本</b>所属的部署ID（{@code ACT_RE_DEPLOYMENT.ID_}）。
     *
     * <p>用途：「流程定义 deployment_id 落库」之后的<b>精确比对</b>——把引擎 latest 与
     * {@code wf_process_definition.deployment_id} 对比，判断是否被测试部署（或手工部署）
     * 顶替。引擎里没有该 key 的部署时返回 null（未部署 / 已卸载）。</p>
     *
     * @param procKey 流程定义Key（BPMN process id）
     * @return latest 版本的部署ID；无部署返回 null
     */
    String latestDeploymentId(String procKey);

    /**
     * 查询引擎中该 procKey 的<b>最新版本</b>对应的流程定义ID（{@code ACT_RE_PROCDEF.ID_}）。
     *
     * <p>用途：部署后把「激活的那一版定义ID」回写 {@code wf_process_definition.proc_def_id}，
     * 供发起时按 {@link #startInstanceById} 精确启动（方案 §3）；也可用于测试态部署后
     * 取「测试版本的定义ID」，与正式版本物理隔离。</p>
     *
     * @param procKey 流程定义Key（BPMN process id）
     * @return latest 版本的 processDefinitionId；无部署返回 null
     */
    String latestProcDefId(String procKey);

    /**
     * 删除引擎部署（级联清理流程定义与历史数据）。
     *
     * <p>用于「真实引擎假数据测试」的<b>测试态清理</b>：测试部署走 {@code test_<procKey>} 独立 key，
     * 跑完即卸载，不污染生产定义。</p>
     *
     * @param deploymentId 部署ID
     */
    void deleteDeployment(String deploymentId);

    /**
     * 列出引擎中 <b>procKey 匹配给定模式</b> 的所有部署ID（去重）。
     *
     * <p>用途：清理「孤儿测试部署」（方案 §6.4 C9/C10 / S12）。测试部署走
     * {@code procKey__test} 独立 key，正常由 {@link #deleteDeployment} 卸载；但若实例被手工删除、
     * 或清理时卸载失败，这些 {@code __test} 部署会长期留在引擎库中 —— 既白占空间，
     * 又会让巡检 ⑥ 误报「latest 部署被测试顶替」。清理测试数据时用它扫出全部测试部署，
     * 再对照 {@code wf_instance.test_deployment_id} 判定哪些已无人引用（= 孤儿）后卸载。</p>
     *
     * @param keyLike 引擎 key 匹配模式（SQL LIKE 语义，如 {@code %__test}）
     * @return 部署ID列表（去重）；无匹配返回空列表
     */
    List<String> deploymentIdsByKeyLike(String keyLike);

    /**
     * 统计引擎当前待执行作业数（普通作业 + 定时作业）。
     *
     * <p>用于断言一次正常流转后<b>没有残留引擎作业</b>（验证异步/定时未被意外触发）。
     * 定时器边界事件（节点类型 5）等高级场景的覆盖将借助此方法驱动 {@code ManagementService} 执行作业。</p>
     *
     * @return 待执行作业总数
     */
    long pendingJobCount();

}
