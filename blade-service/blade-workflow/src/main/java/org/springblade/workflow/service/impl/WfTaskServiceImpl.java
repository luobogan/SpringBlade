package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.odoc.action.WfNodeMustDoActionExecutor;
import org.springblade.workflow.odoc.number.WfOdocNumberAssignListener;
import org.springblade.workflow.odoc.number.WfOdocLayoutAssignListener;
import org.springblade.workflow.odoc.sign.OdocSealTriggerResult;
import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.dto.WfTaskListFilter;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.dto.CirculateDTO;
import org.springblade.workflow.dto.ForwardDTO;
import org.springblade.workflow.dto.RejectDTO;
import org.springblade.workflow.dto.UrgeDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.entity.WfExtTask;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.exception.WfAccessDeniedException;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfSubflowRequestMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.mapper.WfExtTaskMapper;
import org.springblade.workflow.entity.WfSubflowRequest;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.service.helper.WfTaskActReader;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springblade.workflow.service.helper.WfWriteHelper;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.message.constant.MessageConstant;
import org.springblade.message.dto.NoticeBizStateDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.reject.WfRejectManager;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.utils.WfConditionVarUtil;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.utils.WfNoticeContext;
import org.springblade.workflow.config.WfRetirementProperties;
import org.springblade.workflow.config.WfTaskResidueProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springblade.workflow.config.WfLedgerModeProperties;
import org.springblade.workflow.vo.RejectCandidateVO;
import org.springblade.workflow.vo.RejectCandidatesVO;
import org.springblade.workflow.vo.TaskVO;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程任务语义服务实现
 *
 * <p>会签 / 或签 / 依次语义（对齐 ecology {@code sign_order}）在此落地：</p>
 * <ul>
 *   <li>{@code 0 或签}：任一人处理即推进引擎，同节点其余待办置办结</li>
 *   <li>{@code 1 会签}：全部处理人办完后才推进引擎</li>
 *   <li>{@code 2 依次}：按批次逐个激活，最后一人处理完才推进引擎</li>
 * </ul>
 * <p>退回 / 撤回 / 转发 / 加签 / 抄送 / 催办等国产 OA 语义同样在此实现，
 * BPMN 仅承载主干流转。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfTaskServiceImpl implements IWfTaskService {

    /** 或签 */
    private static final int SIGN_ANY = 0;
    /** 会签 */
    private static final int SIGN_ALL = 1;
    /** 依次 */
    private static final int SIGN_SEQUENCE = 2;

    // 节点信息「操作菜单」的操作码（与前端 wfDict.MENUS_OPTIONS 保持一致）
    private static final String MENU_SUBMIT = "submit";
    private static final String MENU_REJECT = "reject";
    private static final String MENU_FORWARD = "forward";

    /** 系统自动通过时的办理人占位（无人上下文，如超时任务） */
    private static final long AUTO_OPERATOR = 0L;
    /** 超时自动通过的默认意见 */
    private static final String AUTO_OPINION = "超时自动通过";

    /** 退回后再提交的处理方式：逐级审批（缺省） */
    private static final int RESUBMIT_SEQUENTIAL = 1;
    /** 退回后再提交的处理方式：直达本节点（重新提交时跳过中间节点，直接回到执行退回的节点） */
    private static final int RESUBMIT_DIRECT = 2;
    /**
     * 「直达本节点」引擎变量：值 = 执行退回的节点Key。退回时写入，
     * 退回目标节点重新提交时消费（跳过中间节点直达）并清除。
     */
    private static final String VAR_RESUBMIT_DIRECT_NODE = "wf_resubmit_direct_node";
    /** 退回弹窗操作者列最多展示的姓名数量（超出以「等N人」收尾） */
    private static final int MAX_OPERATOR_NAMES = 3;

    private final WfTaskMapper taskMapper;
    /** 扩展任务权威存储（协办/加签协办/抄送/传阅/已读/退回重提交/自由流程步骤） */
    private final WfExtTaskMapper extTaskMapper;
    /** 批次 1：按 BIZ_TASK_ID_ 反查 ACT 的双模式任务解析器（取代 taskMapper.selectById） */
    private final org.springblade.workflow.service.helper.WfTaskBizResolver bizResolver;
    /** 批次 2：ACT 业务列直写器（转办换人只改办理人列，不覆盖 BIZ_TASK_ID_） */
    private final org.springblade.workflow.service.helper.WfTaskActDirectWriter directWriter;
    /** 批次 2（D17 方案 X）：运行中为当前 UserTask 动态新增多实例执行体，生成独立 ACT 任务 */
    private final org.flowable.engine.RuntimeService runtimeService;
    /** 批次 2：定位加签后新生成的引擎任务（按实例 + 节点 + 办理人） */
    private final org.flowable.engine.TaskService flowableTaskService;
    private final WfInstanceMapper instanceMapper;
    /** 二期 T9：审批完成后把该待办的流程通知回写「已处理」（失败仅记日志，不影响审批事务） */
    private final INoticeClient noticeClient;
    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeOperatorMapper operatorMapper;
    /** 出口条件读取：提交前补齐网关 UEL 变量，避免 Unknown property */
    private final WfNodeLinkMapper linkMapper;
    private final WfApprovalLogMapper logMapper;
    private final IProcessService processService;
    private final IWfInstanceService instanceService;
    private final NodeActionExecutor nodeActionExecutor;
    private final IUserClient userClient;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfRejectManager rejectManager;
    private final WfSubflowRequestMapper subflowRequestMapper;
    /** 公文「节点必做动作」拦截（套红/后续签章/清稿等提交前校验） */
    private final WfNodeMustDoActionExecutor odocMustDoActionExecutor;
    /** 公文文号「出口监听」（定稿节点取号并回写表单） */
    private final WfOdocNumberAssignListener odocNumberAssignListener;
    /** 公文信息镜像同步（提交后置：表单公文属性 → wf_odoc_request，供待办/查询列表免 join 表单） */
    private final org.springblade.workflow.odoc.sync.WfOdocRequestSyncListener odocRequestSyncListener;
    /** 公文版式「出口监听」（转版式/定稿节点自动转 PDF/OFD，失败不阻断主干） */
    private final WfOdocLayoutAssignListener odocLayoutAssignListener;
    /** 节点操作者解析（退回弹窗「操作者」列展示用，解析失败不阻塞） */
    /** 公文用印「出口监听」（签章/定稿节点发起用印：local 同步盖章 / esign 异步发起签署） */
    private final org.springblade.workflow.odoc.sign.WfOdocSignAssignListener odocSignAssignListener;
    private final org.springblade.workflow.resolver.WfOperatorResolver operatorResolver;
    /** 任务业务列双写收口器（去 wf_ 表写侧，方案 A1：扩展 ACT_RU_TASK/ACT_HI_TASKINST） */
    private final WfTaskActWriter taskActWriter;
    /** 待办/已办读源=act 时的 ACT_* 读取器（含读源开关，默认 wf，零行为变化） */
    private final WfTaskActReader taskActReader;
    /** 定义期节点配置读取（BPMN wf: 扩展） */
    private final WfBpmnExtensionReader bpmnReader;
    /** 流转日志唯一写入口（含 ACT_HI_COMMENT 审批意见双写） */
    private final WfWriteHelper writeHelper;
    /** 自由流程推进器（特性开关默认关闭；非自由实例零行为变化） */
    private final org.springblade.workflow.freeflow.IWfFreeFlowDriver freeFlowDriver;
    /** 自由流程影子节点装饰（把静态池节点包装成本实例该步骤的节点） */
    private final org.springblade.workflow.freeflow.WfFreeNodeResolver freeNodeResolver;

    /**
     * 定义源开关：false=读 wf_process_node（默认，零行为变化）；true=从 BPMN {@code wf:node} 扩展读。
     *
     * <p>切 true 前须确认设计器保存/回填已把节点配置写入 BPMN（{@code writeNode}），
     * 否则读到的维度为空会被当成「未配置」处理。</p>
     */
    @Value("${blade.workflow.definition-from-bpmn.enabled:false}")
    private boolean definitionFromBpmn;

    @Autowired
    private WfRetirementProperties retirement = new WfRetirementProperties();

    /** 台账写入模式（direct=不落 wf_task）：降级反模式在 direct 下不可静默走空 wf_task，须抛错暴露 */
    @Autowired
    private WfLedgerModeProperties ledgerMode;

    /**
     * 「wf_task 兜底残留合并」开关（去 wf_ 表 · P6 退役第一台阶）。
     *
     * <p>true（默认，零行为变化）：列表/分页把「ACT 单行表达不了的行」（存量 N:1 会签 / 孤儿 / 合成待办）
     * 从 {@code wf_task} 补进来，角标同口径（{@code WfTaskActReader#countFromAct} 共用本开关），
     * 保证零丢单。</p>
     *
     * <p>false（纯 ACT 单源）：完全不查 {@code wf_task}，{@code total} 严格等于 ACT count，
     * 消除「actTotal + residueTotal」与实际可翻行数的错位（深页漏行的根因之一）。</p>
     *
     * <p>关闭前须按 {@code doc/md/去wf_表-任务读源翻源上线检查.md} §2 校验达标阈值，
     * 并确认合成待办（草稿待办 / 退回发起人重提交）从待办列表消失在业务上可接受。</p>
     */
    @Autowired
    private WfTaskResidueProperties residueMerge = new WfTaskResidueProperties();

    /**
     * 「blade 扩展任务」的状态集合：协办(7)/抄送(8)/传阅(11)。
     *
     * @deprecated 2026-11-09 起扩展任务已迁至 {@code wf_ext_task}，列表/角标改按
     *     {@code WfExtTask.type} 判据读取。本常量仅保留供 {@code wfTaskQuery(extOnly=true)}
     *     弃用分支追溯，主流程不再使用；请勿据此新增 wf_task 读取逻辑。
     */
    @Deprecated
    private static final List<Integer> EXT_TASK_STATUSES =
        List.of(WfTask.STATUS_COADJUTANT, WfTask.STATUS_CIRCULATE, WfTask.STATUS_READ);

    /**
     * 抄送/传阅下沉开关（迁移阶段3「双写校验」）。
     *
     * @deprecated 2026-11-09 起「双写校验」不再保留：扩展任务已一次性迁至 {@code wf_ext_task}，
     *     本开关与引擎 identityLink 预热路径仅保留在 {@link #insertCirculateToWfTaskDeprecated} 供追溯，
     *     主流程 {@link #circulate} 不再读取它。请勿重新启用（会形成双写）。
     */
    @Deprecated
    @Value("${blade.workflow.cc-identity-link.enabled:false}")
    private boolean ccIdentityLinkEnabled;

    /**
     * 会签/或签/依次下沉引擎多实例总开关（下沉迁移方案 §1.1 A 档，与 WfDefinitionServiceImpl 同源）。
     * 默认 false：沿用自研「单 userTask + wf_task 多条待办」会签。
     * <p>开启后：① 部署期已在 userTask 注入 multiInstanceLoopCharacteristics（按人拆分引擎任务）；
     * ② 本类的运行时会签门禁（{@link #doApprove} 的 countPending/closeSiblings/激活下批）须让位给引擎完成条件，
     * 即「同意」只 completeTask 当前引擎任务，放行交由引擎；③ 进入节点前的集合变量 wfMiAssignees_&lt;nodeKey&gt;
     * 由 {@link #injectMiCollectionVars} 在每个流转点注入。</p>
     */
    @Value("${blade.workflow.engine-multi-instance.enabled:false}")
    private boolean multiInstanceEnabled;

    @Override
    public List<WfTaskVO> todo(Long assignee, WfTaskListFilter filter) {
        WfTaskListFilter f = filter == null ? new WfTaskListFilter() : filter;
        if (f.getStatuses() == null || f.getStatuses().isEmpty()) {
            f.setStatuses(List.of(WfTask.STATUS_TODO));
        }
        return list(assignee, f);
    }

    @Override
    public List<WfTaskVO> done(Long assignee, WfTaskListFilter filter) {
        WfTaskListFilter f = filter == null ? new WfTaskListFilter() : filter;
        if (f.getStatuses() == null || f.getStatuses().isEmpty()) {
            f.setStatuses(List.of(WfTask.STATUS_DONE, WfTask.STATUS_FINISHED,
                WfTask.STATUS_AUTO_SUBMIT, WfTask.STATUS_COADJUTANT,
                WfTask.STATUS_CIRCULATE, WfTask.STATUS_READ));
        }
        return list(assignee, f);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long taskId, ApproveDTO dto) {
        return doApprove(taskId, dto, SecureUtil.getUserId(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        return doApprove(taskId, dto, AUTO_OPERATOR, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, java.util.Map<String, Object> variables) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        dto.setVariables(variables);
        return doApprove(taskId, dto, AUTO_OPERATOR, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, Long operator) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        // 流程测试：以节点「接收人」身份审批；未指定时回退系统。
        Long op = (operator == null) ? AUTO_OPERATOR : operator;
        return doApprove(taskId, dto, op, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(Long taskId, String opinion, java.util.Map<String, Object> variables, Long operator) {
        ApproveDTO dto = new ApproveDTO();
        dto.setOpinion(opinion == null ? AUTO_OPINION : opinion);
        dto.setVariables(variables);
        Long op = (operator == null) ? AUTO_OPERATOR : operator;
        return doApprove(taskId, dto, op, true);
    }

    /**
     * 同意处理主体。
     *
     * @param operator 办理人（系统自动通过时为 {@link #AUTO_OPERATOR}）
     * @param system   系统自动触发：跳过「操作菜单」校验（否则未开 submit 的节点无法自动通过）
     */
    private boolean doApprove(Long taskId, ApproveDTO dto, Long operator, boolean system) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 自由流程：一次加载实例步骤快照，并把静态池节点装饰成本实例该步骤的节点。
        // 非自由实例为空上下文，decorate 原样返回；特性开关关闭时连查询都不会发生。
        org.springblade.workflow.freeflow.WfFreeNodeContext freeCtx = freeFlowDriver.context(inst);
        WfProcessNode node = freeNodeResolver.decorate(freeCtx,
            loadNode(inst.getDefId(), task.getNodeKey()), task.getNodeKey());

        // 0. 记录级鉴权：只有该任务办理人本人（或流程管理员）能同意。
        //    系统自动提交 / 超时自动通过走 system=true，无人上下文，不受此限。
        if (!system) {
            WfAuthUtil.requireOperateTask(task, "同意");
            // 节点信息 → 运行时消费：操作菜单校验 + 签字意见必填
            requireOperate(node, MENU_SUBMIT);
        }
        String opinion = resolveOpinion(node, dto == null ? null : dto.getOpinion());

        // 节点信息 → 运行时消费：二次认证 + 字段校验（失败即拒绝，不推进）
        validateBeforeApprove(node, inst, operator, dto, system);

        // 1. 本任务置已办
        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        // 业务列双写 ACT_*：子状态 DONE 落到 ACT_HI_TASKINST（已办读源前置）；开关默认关
        taskActWriter.sync(task);
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), operator,
            WfApprovalLog.LOG_APPROVE, opinion);

        // 2. 按节点审批方式判定是否推进引擎。
        //    引擎多实例模式下放行由 Flowable 完成条件驱动，跳过自研发散计数（运行时会签门禁让位引擎）：
        //    「同意」只 completeTask 当前引擎任务，是否推进/关闭兄弟任务由引擎决定。
        if (!isEngineMultiInstance(inst, task.getNodeKey())) {
            // 自由流程：优先取装饰后节点的 signOrder（来自 wf_free_node.sign_type），
            // 否则 resolveSignOrder 内部读未装饰的静态节点会让会签门禁静默失效。
            int signOrder = (node != null && node.getSignOrder() != null)
                ? node.getSignOrder() : resolveSignOrder(inst.getDefId(), task.getNodeKey());
            if (signOrder == SIGN_ALL) {
                // 会签：仍有同节点待办则不推进
                if (countPending(inst.getId(), task.getNodeKey()) > 0) {
                    log.info("[blade-workflow] 会签节点仍有待办，暂不推进. instId={}, nodeKey={}",
                        inst.getId(), task.getNodeKey());
                    return true;
                }
            } else if (signOrder == SIGN_SEQUENCE) {
                // 依次：激活下一批次，未到最后一人则不推进
                WfTask next = nextPending(inst.getId(), task.getNodeKey());
                if (next != null) {
                    next.setReceiveTime(new Date());
                    // 批次 1：不再更新 wf_task 台账（receiveTime 无对应 ACT 业务列，CREATE_TIME_ 已承载）
                    log.info("[blade-workflow] 依次审批激活下一处理人. instId={}, nextTaskId={}",
                        inst.getId(), next.getId());
                    return true;
                }
            } else {
                // 或签：办结同节点其余待办
                closeSiblings(inst.getId(), task.getNodeKey(), task.getId());
            }
        }

        // 节点信息 → 运行时消费：子流程「全部归档才能提交」—— 本次提交前若本节点仍有未归档子流程，拒绝提交
        if (node != null && WfNodeSettingsUtil.subflowAllEndBeforeSubmit(node) == 1) {
            long pending = subflowRequestMapper.selectCount(Wrappers.<WfSubflowRequest>lambdaQuery()
                .eq(WfSubflowRequest::getMainInstId, inst.getId())
                .eq(WfSubflowRequest::getMainNodeKey, node.getNodeKey())
                .eq(WfSubflowRequest::getStatus, 0));
            if (pending > 0) {
                throw new ServiceException("该节点存在未归档的子流程（" + pending + " 条），全部归档后才能提交");
            }
        }

        // 2.5 提交变量（提前准备：普通提交与「退回发起人后由创建节点重新提交」两条路径共用）
        Map<String, Object> vars = new HashMap<>(8);
        if (dto != null && dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }
        // 引擎多实例：把全部 MI 节点的集合变量 wfMiAssignees_<nodeKey> 注入流转变量，
        // 保证进入该节点前集合已就绪（引擎据此逐人展开）。幂等、持久。
        injectMiCollectionVars(inst.getDefId(), inst, vars, operator);
        // 网关 UEL 条件变量兜底：见 supplyMissingConditionVars 注释。
        // 位置严格排在 validateBeforeApprove（必填/二次认证校验）之后，且只改本次提交的 vars，
        // 绝不写回表单快照 / 业务表，因此不会绕过任何必填校验。
        supplyMissingConditionVars(inst.getDefId(), inst.getEngineInstId(), vars);

        // 2.6 公文准备（取号 / 公文镜像 / 版式转换）
        //     必须排在「节点必做动作校验」之前：esign 方案下签章前提是「已有 PDF/OFD 版式文件」，
        //     而版式文件正是在这里自动转换出来的（先转换 → 再校验 → 再发起签署）。
        //     同时必须排在【引擎推进之前】：esign 是异步的，提交后流程停在签章节点等待签署，
        //     由回调推进（OdocSealAdvancer）；若放在推进之后，等待期间就不会发起签署。
        boolean testInst = inst.getIsTest() != null && inst.getIsTest() == 1;
        OdocSealTriggerResult sealResult = null;
        if (!testInst) {
            // 公文文号出口监听：定稿节点取号并回写表单（失败不阻断主干，仅告警）
            // 自由流程传步骤快照：定稿点改由 wf_free_node.is_finalize 判定
            try {
                odocNumberAssignListener.onNodeSubmit(inst, node, freeCtx);
            } catch (Exception e) {
                log.warn("[blade-workflow] 公文文号取号/回写失败（忽略）. instId={}, nodeKey={}, {}",
                    inst.getId(), node.getNodeKey(), e.getMessage());
            }
            // 公文信息镜像同步：把表单公文属性冗余到 wf_odoc_request（失败不阻断主干，仅告警）
            try {
                odocRequestSyncListener.syncInstance(inst);
            } catch (Exception e) {
                log.warn("[blade-workflow] 公文信息镜像同步失败（忽略）. instId={}, nodeKey={}, {}",
                    inst.getId(), node.getNodeKey(), e.getMessage());
            }
            // 公文版式出口监听：转版式/定稿节点自动转 PDF/OFD（幂等；失败不阻断主干，仅告警）
            try {
                odocLayoutAssignListener.onNodeSubmit(inst, node, freeCtx);
            } catch (Exception e) {
                log.warn("[blade-workflow] 公文版式自动转换失败（忽略）. instId={}, nodeKey={}, {}",
                    inst.getId(), node.getNodeKey(), e.getMessage());
            }
        }

        // 公文「节点必做动作」提交拦截：套红/签章/密级（版式已就绪，esign 口径可校验）
        odocMustDoActionExecutor.checkBeforeSubmit(inst, node, freeCtx);

        // 2.7 公文用印发起（签章/定稿节点）
        //   local：同步盖章 → 流程照常推进；
        //   esign：异步发起签署 → 返回 waiting=true → 本次提交不推进引擎，签完由回调推进。
        if (!testInst) {
            try {
                sealResult = odocSignAssignListener.onNodeSubmit(inst, node, operator, task, vars);
            } catch (Exception e) {
                log.warn("[blade-workflow] 公文用印发起失败（忽略）. instId={}, nodeKey={}, {}",
                    inst.getId(), node.getNodeKey(), e.getMessage());
            }
        }

        // 「签完才流转」：第三方签章已发起、等待签署完成 → 本次提交不推进引擎，
        // 流程停在签章节点；签署完成（回调/补偿）后由 OdocSealAdvancer 推进到下一节点。
        if (sealResult != null && sealResult.isWaiting()) {
            log.info("[blade-workflow] 第三方签章等待签署，本次提交不推进引擎. instId={}, nodeKey={}, flowId={}",
                inst.getId(), task.getNodeKey(), sealResult.getEsignFlowId());
            markNoticeHandled(inst, task);
            return true;
        }

        // 3. 推进引擎并同步后续任务
        // 节点信息 → 运行时消费：「指定流转」。开启后由处理人手动指定下一节点（模式1 可指定操作者）；
        // 模式3（多目标）：以节点上配置的 targets 为准，提交时并行扇出到多个目标节点（仅提交时生效，退回不适用）。
        // 3.x MI 空集合守卫：引擎进入 MI 节点时若集合为空会抛 FlowableException，
        // 这里转成 ServiceException，保证前端按原文案收到「下个节点未解析到办理人」的提示。
        // 3.0 自由流程实例改由 WfFreeFlowDriver 按实例步骤推进（中间步池内迁移、末步归档）；
        //     非自由实例保持下面原有的「指定流转 / completeTask」链路，零回归。
        boolean freeFlow = freeFlowDriver.isEnabled(inst);
        try {
        if (freeFlow) {
            freeFlowDriver.submit(inst, task, dto, operator, vars, freeCtx);
        } else {
        int appointMode = WfNodeSettingsUtil.appointFlowMode(node);
        if (!system && appointMode != 0) {
            if (appointMode == 3) {
                // 多目标：取节点配置的 targets（{nodeKey, operatorIds[], signType}），并行跳转
                List<Map<String, Object>> targets = WfNodeSettingsUtil.appointFlowTargets(node);
                if (targets == null || targets.isEmpty()) {
                    throw new ServiceException("当前节点启用了「指定流转（多目标）」，但未配置目标节点");
                }
                List<String> toKeys = new ArrayList<>();
                Map<String, Long> overrideAssignees = new LinkedHashMap<>();
                Set<String> seen = new LinkedHashSet<>();
                for (Map<String, Object> tg : targets) {
                    String tk2 = tg == null ? null : String.valueOf(tg.get("nodeKey"));
                    if (tk2 == null || tk2.isBlank()) {
                        continue;
                    }
                    if (tk2.equals(task.getNodeKey())) {
                        throw new ServiceException("指定流转的目标节点不能是当前节点：" + tk2);
                    }
                    if (loadNode(inst.getDefId(), tk2) == null) {
                        throw new ServiceException("指定流转的目标节点不存在：" + tk2);
                    }
                    if (!seen.add(tk2)) {
                        continue; // 去重
                    }
                    toKeys.add(tk2);
                    Object ops = tg.get("operatorIds");
                    if (ops instanceof List && !((List<?>) ops).isEmpty()) {
                        Object first = ((List<?>) ops).get(0);
                        Long oid = parseId(first);
                        if (oid != null && oid > 0) {
                            overrideAssignees.put(tk2, oid);
                        }
                    }
                }
                if (toKeys.isEmpty()) {
                    throw new ServiceException("当前节点启用了「指定流转（多目标）」，但未配置有效目标节点");
                }
                processService.moveActivityToActivities(inst.getEngineInstId(), task.getNodeKey(), toKeys, vars);
                instanceService.advance(inst.getId(), operator, overrideAssignees, AdvanceSrc.SUBMIT);
            } else {
                String nextNodeKey = dto == null ? null : dto.getNextNodeKey();
                if (nextNodeKey == null || nextNodeKey.isBlank()) {
                    throw new ServiceException("当前节点启用了「指定流转」，请选择下一节点");
                }
                if (nextNodeKey.equals(task.getNodeKey())) {
                    throw new ServiceException("指定流转的目标节点不能是当前节点");
                }
                if (loadNode(inst.getDefId(), nextNodeKey) == null) {
                    throw new ServiceException("指定流转的目标节点不存在：" + nextNodeKey);
                }
                // 模式1：用户指定操作者；模式2：忽略用户操作者，按目标节点设置解析
                Long overrideAssignee = (appointMode == 1 && dto != null) ? dto.getNextAssignee() : null;
                processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), nextNodeKey, vars);
                instanceService.advance(inst.getId(), operator, nextNodeKey, overrideAssignee);
            }
        } else {
            processService.completeTask(task.getEngineTaskId(), vars);
            // 「直达本节点」退回后的重新提交：token 已随 complete 流到下一节点，
            // 这里直接跳回执行退回的节点（跳过中间节点）；跳转失败/不适用则降级逐级审批
            if (!jumpDirectIfMarked(inst, operator)) {
                instanceService.advance(inst.getId(), operator);
            }
            }
            }
            } catch (org.flowable.common.engine.api.FlowableException fe) {
            // MI 空集合守卫异常（非 MI 节点解析为空已在 advanceInternal 内转 ServiceException 阻断）。
            // 保留原文案，改抛 ServiceException 让前端收到干净中文错误信息。
            String raw = fe.getMessage() == null ? "" : fe.getMessage();
            if (raw.contains("Unknown property used in expression")) {
                // 理论上 supplyMissingConditionVars 已补齐所有条件变量；这里是兜底漏网时的中文防线，
                // 避免用户再看到 Flowable/JUEL 的英文原始异常。
                String var = extractUelVar(raw);
                throw new org.springblade.core.log.exception.ServiceException(
                    "流程出口条件引用的字段【" + (var == null ? "未知字段" : var) + "】未随表单提交下发，"
                        + "引擎无法判断审批路径；请检查：①该字段是否已在表单中配置 "
                        + "②该字段是否设为必填 ③出口条件引用的字段名是否正确。原始信息：" + raw);
            }
            throw new org.springblade.core.log.exception.ServiceException(raw);
            }

            // 节点信息 → 运行时消费：节点后附加操作 + 子流程触发（异常策略由 NodeActionExecutor 吸收）
        // 测试态：跳过附加操作/子流程副作用（对齐 ecology istest，避免污染真实业务数据）
        if (!testInst) {
            nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, operator);
            nodeActionExecutor.triggerSubflow(inst, node, NodeActionExecutor.TRIGGER_AFTER_SUBMIT, operator);
        }

        // 二期 T9（对齐 ecology updateBizState）：待办已办理 → 把 TASK_CREATED 时发的
        // 流程通知（bizRefType=WF_TASK, bizRefId=引擎任务ID）回写为「已处理」，
        // 通知列表不再是永久"待办"。失败仅记日志（尽力而为语义，可补偿），不影响审批事务。
        markNoticeHandled(inst, task);
        return true;
    }

    /**
     * 待办通知状态回写（已处理）。
     *
     * <p>二期 T9（对齐 ecology updateBizState）：待办已办理 → 把 TASK_CREATED 时发的
     * 流程通知（bizRefType=WF_TASK, bizRefId=引擎任务ID）回写为「已处理」。
     * 失败仅记日志（尽力而为语义，可补偿），不影响审批事务。</p>
     */
    private void markNoticeHandled(WfInstance inst, WfTask task) {
        try {
            if (task != null && task.getEngineTaskId() != null) {
                NoticeBizStateDTO biz = new NoticeBizStateDTO();
                biz.setTenantId(inst == null ? null : inst.getTenantId());
                biz.setBizRefType("WF_TASK");
                biz.setBizRefId(task.getEngineTaskId());
                biz.setBizState(MessageConstant.BIZ_STATE_HANDLED);
                noticeClient.markBizState(biz);
            }
        } catch (Exception e) {
            log.warn("[WfTaskServiceImpl] 待办通知状态回写失败（忽略）. engineTaskId={}, {}",
                task == null ? null : task.getEngineTaskId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long taskId, RejectDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能退回
        WfAuthUtil.requireOperateTask(task, "退回");
        WfProcessNode node = loadNode(inst.getDefId(), task.getNodeKey());
        // 节点信息 → 运行时消费：未勾选「退回」则不允许退回
        requireOperate(node, MENU_REJECT);
        // 主开关：节点表 allow_reject=0 时关闭退回（与 operateMenu 双重保险）
        if (node != null && node.getAllowReject() != null && node.getAllowReject() == 0) {
            throw new ServiceException("当前节点未开放退回");
        }

        // 计算可退回节点集合（反向回溯 + 白名单；已剔除引擎停不住的类型，见 WfRejectManager）
        List<WfProcessNode> candidates = rejectManager.computeRejectableNodes(
            inst.getDefId(), task.getNodeKey(), node, inst.getId());
        if (candidates.isEmpty()) {
            // 上游没有可停留的节点（如当前就是第一个审批节点、上游只有开始节点的情况本不该走到这里）。
            // 明确报错，避免调用方拿到 null 目标后又「移了个寂寞」。
            throw new ServiceException("当前节点没有可退回的节点：上游不存在可退的审批/提交节点");
        }
        int rejectType = WfNodeSettingsUtil.rejectType(node);

        // 解析目标节点
        String targetNodeKey;
        if (dto != null && dto.getTargetNodeKey() != null && !dto.getTargetNodeKey().isBlank()) {
            targetNodeKey = dto.getTargetNodeKey();
        } else {
            // 选择退回（rejectType=2）必须显式选节点；直接退回（1）退默认/上一节点
            if (rejectType == 2) {
                throw new ServiceException("该节点需选择退回节点");
            }
            targetNodeKey = rejectManager.resolveDefaultRejectNode(
                inst.getDefId(), task.getNodeKey(), node, candidates);
        }
        // 目标合法性：必须在可退回集合内（对齐 ecology DoRejectRequestCmd 的目标校验）
        if (!rejectManager.isRejectable(candidates, targetNodeKey)) {
            throw new ServiceException("退回目标节点不合法：" + targetNodeKey);
        }

        task.setStatus(WfTask.STATUS_DONE);
        task.setOperateTime(new Date());
        // 业务列双写 ACT_*：退回也是任务完成态，子状态 DONE 需落到 ACT_HI_TASKINST
        taskActWriter.sync(task);
        // 留痕记「节点接收人」而非当前登录人：代跑/测试态下当前登录人是管理员，记管理员会让
        // 流程信息看起来是「管理员退回了这张单」，与代跑留痕口径不一致（见方案 C14/C16）
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), task.getAssignee(),
            WfApprovalLog.LOG_REJECT, dto == null ? null : dto.getOpinion());
        closeSiblings(inst.getId(), task.getNodeKey(), task.getId());

        // 引擎回退：把当前节点 token 移动到目标节点（保持实例运行，不终止）
        Map<String, Object> vars = new HashMap<>(4);
        // 引擎多实例：退回目标节点可能是多实例，注入集合变量保证进入前就绪
        injectMiCollectionVars(inst.getDefId(), inst, vars, task.getAssignee());
        // 「退回后再提交的处理方式」：直达本节点 → 写引擎变量，供退回目标节点重新提交时
        // 跳过中间节点直达本次执行退回的节点（见 jumpDirectIfMarked）；逐级审批则清残留标记
        int resubmitMode = (dto != null && dto.getResubmitMode() != null)
            ? dto.getResubmitMode() : RESUBMIT_SEQUENTIAL;
        if (resubmitMode == RESUBMIT_DIRECT) {
            vars.put(VAR_RESUBMIT_DIRECT_NODE, task.getNodeKey());
        } else {
            processService.removeVariables(inst.getEngineInstId(), List.of(VAR_RESUBMIT_DIRECT_NODE));
        }
        // 引擎回退：把当前节点 token 移动到目标节点（保持实例运行，不终止）。目标节点若是
        // 「创建/申请人填单」（nodeType=0，创建节点已是 UserTask 等待态），同样直接
        // moveActivity + advance —— 引擎停在创建节点并为其生成真实待办（办理人=发起人），
        // 无需专用「退回发起人」路径。
        processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), targetNodeKey, vars);
        // 同步 wf_task / 当前节点：advance 读引擎当前活动任务，为目标节点生成待办
        // 来源标记 REJECT：退回链路的「流程异常处理」兜底不生效（对齐 ecology「退回忽略异常处理设置」）
        instanceService.advance(inst.getId(), task.getAssignee(), null, null, AdvanceSrc.REJECT);
        // 自由流程：退回后重置目标步骤及其后步骤的「已流转」状态，否则重新提交仍按 flowOrder 前进
        if (freeFlowDriver.isEnabled(inst)) {
            freeFlowDriver.onReject(inst.getId(), targetNodeKey);
        }

        // 节点信息 → 运行时消费：节点后附加操作（退回场景，仅执行勾选「退回时触发」的条目）
        // 测试态：跳过附加操作 —— 对齐 doApprove 的 PHASE_POST 跳过（:309-313），
        // 否则在测试实例上点退回会真的执行「写业务数据 / 调外部接口」类动作（方案 V14/C16）
        if (!isTestInst(inst)) {
            nodeActionExecutor.execute(inst, node, NodeActionExecutor.PHASE_POST, task.getAssignee(), true);
        }
        return true;
    }

    @Override
    public RejectCandidatesVO rejectNodes(Long taskId) {
        WfTask task = resolveTask(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能查询
        WfAuthUtil.requireOperateTask(task, "查询退回节点");
        WfProcessNode node = loadNode(inst.getDefId(), task.getNodeKey());
        if (node != null && node.getAllowReject() != null && node.getAllowReject() == 0) {
            throw new ServiceException("当前节点未开放退回");
        }
        List<WfProcessNode> candidates = rejectManager.computeRejectableNodes(
            inst.getDefId(), task.getNodeKey(), node, inst.getId());
        int rejectType = WfNodeSettingsUtil.rejectType(node);
        String defaultNodeKey = rejectManager.resolveDefaultRejectNode(
            inst.getDefId(), task.getNodeKey(), node, candidates);

        RejectCandidatesVO vo = new RejectCandidatesVO();
        vo.setType(rejectType);
        vo.setDefaultNodeKey(defaultNodeKey);
        vo.setRemind(WfNodeSettingsUtil.rejectRemind(node));
        vo.setChangeNode(WfNodeSettingsUtil.rejectChangeNode(node));
        List<RejectCandidateVO> nodes = new ArrayList<>();
        for (WfProcessNode n : candidates) {
            RejectCandidateVO c = new RejectCandidateVO();
            c.setNodeKey(n.getNodeKey());
            c.setNodeName(n.getNodeName());
            c.setNodeType(n.getNodeType());
            c.setOperators(resolveOperatorNames(inst, n, task.getAssignee()));
            nodes.add(c);
        }
        vo.setNodes(nodes);
        return vo;
    }

    /**
     * 解析候选节点的当前操作者姓名（退回弹窗「操作者」列展示用）。
     *
     * <p>创建节点(0) = 发起人本人；其余节点走 {@link WfOperatorResolver#resolve}
     * （与流转 {@code advance} 生成待办同一解析口径）。姓名经 {@code IUserClient#userInfo}
     * 换取，最多展示 {@link #MAX_OPERATOR_NAMES} 个（超出以「等N人」收尾）。</p>
     *
     * <p>解析失败 / 为空返回 {@code null}（前端显示 -）：<b>绝不抛异常阻塞退回弹窗</b> ——
     * 操作者列是展示性信息，退回的权限与目标校验由 reject 主流程负责。</p>
     */
    private String resolveOperatorNames(WfInstance inst, WfProcessNode node, Long currentOperator) {
        try {
            List<Long> ids;
            if (node.getNodeType() != null && node.getNodeType() == 0) {
                ids = inst.getStarter() == null ? List.of() : List.of(inst.getStarter());
            } else {
                ids = operatorResolver.resolve(inst.getDefId(), node.getNodeKey(), inst.getId(),
                    inst.getStarter(), currentOperator);
            }
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            List<String> names = new ArrayList<>();
            for (Long id : ids) {
                if (id == null || id <= 0 || names.size() >= MAX_OPERATOR_NAMES) {
                    continue;
                }
                try {
                    R<UserInfo> r = userClient.userInfo(id);
                    if (r != null && r.getData() != null && r.getData().getUser() != null
                        && r.getData().getUser().getRealName() != null) {
                        names.add(r.getData().getUser().getRealName());
                    }
                } catch (Exception ignore) {
                    // 单个用户查询失败不影响整列展示
                }
            }
            if (names.isEmpty()) {
                return null;
            }
            String joined = String.join("、", names);
            return ids.size() > MAX_OPERATOR_NAMES ? joined + " 等" + ids.size() + "人" : joined;
        } catch (Exception e) {
            log.debug("[blade-workflow] 解析退回候选节点操作者失败. defId={}, nodeKey={}",
                inst.getDefId(), node.getNodeKey(), e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forward(Long taskId, ForwardDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能把自己的待办转出去
        WfAuthUtil.requireOperateTask(task, "转办");
        // 测试态：转办会给「目标人」新建一条待办（且存在标记断层 V10），必须后端拒绝
        assertNotTestTask(task, "转办");
        if (dto == null || dto.getAssignee() == null) {
            throw new ServiceException("转办目标人不能为空");
        }
        // 节点信息 → 运行时消费：未勾选「转办」则不允许转办
        requireOperate(loadNode(inst.getDefId(), task.getNodeKey()), MENU_FORWARD);

        // 【批次 2 / D17 方案 X 语义修正】转办 = 把「同一条待办」的办理人换给目标人，
        // bizTaskId 保持不变。旧实现是「原任务置 DONE + 新建同 engineTaskId 的 TODO 行」，
        // 在 ACT 单行模型下等于原地覆盖（丢掉原 BIZ_TASK_ID_ → 前端已打开的办理页失效），
        // 且批次 1 起 wf_task 已零写入、第二条行无处落地。
        // 用户可见效果不变：原办理人的待办消失、目标人看到同一条待办。
        java.util.LinkedHashMap<String, Object> cols = new java.util.LinkedHashMap<>();
        cols.put("BIZ_ASSIGNEE_", dto.getAssignee());
        cols.put("ORIGINAL_USER_", task.getAssignee() == null ? null : String.valueOf(task.getAssignee()));
        directWriter.writePartial(task.getEngineTaskId(), cols);

        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_FORWARD, dto.getOpinion());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSign(Long taskId, AddSignDTO dto) {
        WfTask task = requireTodoTask(taskId);
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能加签
        WfAuthUtil.requireOperateTask(task, "加签");
        // 测试态：加签会给「加签人」新建一条待办（且存在标记断层 V10），必须后端拒绝
        assertNotTestTask(task, "加签");
        if (dto == null || dto.getAssignee() == null) {
            throw new ServiceException("加签人不能为空");
        }

        // 【批次 2 / D17 方案 X】加签 = 运行中为当前 UserTask 动态新增一个多实例执行体，
        // 引擎生成「独立 ACT 任务」（独立 ACT_RU_TASK.ID_），从而：
        //   1) 原办理人待办保留（加签是「增加」办理人，旧实现是「替换」，语义错误）；
        //   2) 加签人有自己的引擎任务 → 不产生 N:1，1:1 判据恒成立；
        //   3) 每个加签人有独立 bizTaskId，列表与操作链各自独立。
        // 前置：该节点须为「部署期已注入多实例」的形态（engine-multi-instance.enabled=true
        // 且定义已重新部署）。Flowable 8.1 对非 MI 节点抛
        // FlowableException("No multi instance execution found for activity id ...")，此处转可读提示。
        String engineInstId = inst.getEngineInstId();
        String nodeKey = task.getNodeKey();
        String assignee = String.valueOf(dto.getAssignee());

        // 加签前该实例的引擎任务快照（用于定位新生成的那条）
        java.util.Set<String> before = new java.util.HashSet<>();
        for (org.flowable.task.api.Task t : flowableTaskService.createTaskQuery()
            .processInstanceId(engineInstId).list()) {
            before.add(t.getId());
        }
        try {
            runtimeService.addMultiInstanceExecution(nodeKey, engineInstId,
                Map.of("wfMiAssignee", assignee));
        } catch (org.flowable.common.engine.api.FlowableException e) {
            throw new ServiceException("当前节点未启用多人会签，无法加签：" + e.getMessage());
        }
        org.flowable.task.api.Task addedTask = null;
        for (org.flowable.task.api.Task t : flowableTaskService.createTaskQuery()
            .processInstanceId(engineInstId).taskAssignee(assignee).list()) {
            if (!before.contains(t.getId()) && nodeKey.equals(t.getTaskDefinitionKey())) {
                addedTask = t;
                break;
            }
        }
        if (addedTask == null) {
            throw new ServiceException("加签失败：引擎未生成新的待办任务");
        }
        // D17/V6 补偿：加签人并回集合变量，否则驳回重建会丢加签人（见 appendMiAssignee）
        appendMiAssignee(engineInstId, nodeKey, assignee);

        // 直写新任务的业务列（独立 bizTaskId，1:1 契约）
        org.springblade.workflow.service.helper.TaskBizRecord rec =
            new org.springblade.workflow.service.helper.TaskBizRecord();
        rec.setBizTaskId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        rec.setEngineTaskId(addedTask.getId());
        rec.setInstId(inst.getId());
        rec.setNodeKey(nodeKey);
        rec.setAssignee(dto.getAssignee());
        rec.setSignOrder(dto.getAddSignType());
        rec.setStatus(WfTask.STATUS_TODO);
        rec.setReceiveTime(new Date());
        // 标记继承：同 forward（V10 / C2）
        rec.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        rec.setTenantId(inst.getTenantId());
        directWriter.writeFull(rec);

        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_COMMENT,
            ("加签：" + (dto.getOpinion() == null ? "" : dto.getOpinion())));
        return true;
    }

    /**
     * D17 补偿：把加签人并回部署期注入的多实例集合变量 {@code wfMiAssignees_<nodeKey>}。
     *
     * <p>依据 {@code WfAddSignMiSpikeTest} 实证：Flowable 8.1 的
     * {@code addMultiInstanceExecution} <b>不会写回集合变量</b>，驳回重建按集合变量重建
     * MI 实例时加签人会丢失（V6 语义）。故加签后必须并回，否则「加签 → 驳回 → 重提」丢加签人。</p>
     */
    private void appendMiAssignee(String engineInstId, String nodeKey, String assignee) {
        String varName = "wfMiAssignees_" + nodeKey;
        Object cur = runtimeService.getVariable(engineInstId, varName);
        java.util.List<String> list = new java.util.ArrayList<>();
        if (cur instanceof java.util.Collection) {
            for (Object o : (java.util.Collection<?>) cur) {
                if (o != null) {
                    list.add(String.valueOf(o));
                }
            }
        }
        if (!list.contains(assignee)) {
            list.add(assignee);
        }
        runtimeService.setVariable(engineInstId, varName, list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean circulate(Long taskId, CirculateDTO dto) {
        WfTask task = resolveTask(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）能发起抄送
        WfAuthUtil.requireOperateTask(task, "抄送");
        // 测试态：抄送会给真人建「已办」条目（status=8，V9），必须后端拒绝
        assertNotTestTask(task, "抄送");
        if (dto == null || dto.getAssignees() == null || dto.getAssignees().isEmpty()) {
            throw new ServiceException("抄送人不能为空");
        }
        for (Long assignee : dto.getAssignees()) {
            // 【去 wf_ 表 · P6 退役第一台阶】扩展任务（抄送）写入 wf_ext_task，不再写 wf_task、不再 sync ACT。
            // wf_task 收敛为「仅引擎任务本体台账」；列表/角标的扩展任务段改由 wf_ext_task 读取。
            WfExtTask cc = new WfExtTask();
            cc.setInstId(inst.getId());
            cc.setEngineTaskId(task.getEngineTaskId());
            cc.setNodeKey(task.getNodeKey());
            cc.setAssignee(assignee);
            cc.setType(WfExtTask.TYPE_CIRCULATE);
            cc.setStatus(WfExtTask.STATUS_PENDING);
            cc.setOpinion(dto.getOpinion());
            cc.setReceiveTime(new Date());
            if (inst != null) {
                cc.setTitle(inst.getTitle());
            }
            // 标记继承：同 forward（V10 / C2）—— 抄送任务会进入被抄送人的「已办」（V9）
            cc.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
            extTaskMapper.insert(cc);
        }
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_CIRCULATE, dto.getOpinion());
        return true;
    }

    /**
     * 「抄送写 wf_task + 引擎 identityLink 预热」旧路径。
     *
     * @deprecated 2026-11-09 起扩展任务（抄送）已迁至 {@code wf_ext_task}，本方法不再被调用，
     *     仅保留供追溯/回滚参考。不要恢复调用：会与 {@link #circulate} 形成双写，
     *     且 {@code taskActWriter.sync} 会把抄送行的 BIZ_TASK_ID_ 投影覆盖宿主引擎任务。
     */
    @Deprecated
    private void insertCirculateToWfTaskDeprecated(WfTask host, WfInstance inst, Long assignee) {
        // 批次 1：该旧路径彻底停用 —— 不再写 wf_task、不再 sync、不再预热 identityLink。
        // 抄送已由 circulate() 写 wf_ext_task；保留方法签名仅供追溯，禁止恢复调用
        // （恢复会用抄送行的 BIZ_TASK_ID_ 覆盖宿主引擎任务）。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean urge(Long taskId, UrgeDTO dto) {
        WfTask task = resolveTask(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：催办限「发起人 / 该节点办理人 / 流程管理员」——
        // 发起人催审批人、同节点办理人之间互相催办都是常见诉求，无关人员无权干预该单
        if (!WfAuthUtil.isSelfOrAdmin(inst.getStarter()) && !WfAuthUtil.canOperateTask(task)) {
            throw new WfAccessDeniedException("无权催办：只有流程发起人、当前节点办理人或流程管理员可以催办");
        }
        // 测试态：催办会在「当前登录人」名下写留痕且无实际意义，后端拒绝
        assertNotTestTask(task, "催办");
        appendLog(inst.getId(), task.getId(), task.getNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, dto == null ? null : dto.getOpinion());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markViewed(Long taskId) {
        if (taskId == null) {
            return false;
        }
        WfTask task = resolveTask(taskId);
        if (task == null) {
            // 【去 wf_ 表】扩展任务（协办/抄送/传阅/已读）的 id 不在 wf_task，改由 wf_ext_task 解析。
            // 打开抄送/传阅条目时标记「已读」：viewTime 只记首次，status 置为已读(1)。
            WfExtTask ext = resolveExtTask(taskId);
            if (ext == null) {
                return false;
            }
            WfAuthUtil.requireSelfOrAdmin(ext.getAssignee(), "标记已读");
            if (ext.getViewTime() != null
                && ext.getStatus() != null && ext.getStatus() != WfExtTask.STATUS_PENDING) {
                return true;
            }
            WfExtTask patch = new WfExtTask();
            patch.setId(taskId);
            patch.setViewTime(new Date());
            patch.setStatus(WfExtTask.STATUS_DONE);
            return extTaskMapper.updateById(patch) > 0;
        }
        // 记录级鉴权：只有该任务办理人本人（或流程管理员）打开办理页才会写「已查看」
        WfAuthUtil.requireOperateTask(task, "标记查看");
        // 只记首次：已看过不改时间（流程图「已查看」只关心是否打开过）
        if (task.getViewTime() != null) {
            return true;
        }
        WfTask patch = new WfTask();
        patch.setId(taskId);
        patch.setViewTime(new Date());
        // 批次 1：不再更新 wf_task 台账，直接把 viewTime 同步到 ACT 业务列
        task.setViewTime(patch.getViewTime());
        taskActWriter.sync(task);
        return true;
    }

    @Override
    public WfTaskVO detail(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务不存在");
        }
        WfTask task = resolveTask(taskId);
        if (task == null) {
            // 【去 wf_ 表】扩展任务条目：从 wf_ext_task 解析并做同等记录级鉴权
            WfExtTask ext = resolveExtTask(taskId);
            if (ext == null) {
                throw new ServiceException("任务不存在");
            }
            WfAuthUtil.requireSelfOrAdmin(ext.getAssignee(), "查看");
            return toVO(ext);
        }
        // 记录级鉴权：仅任务办理人本人（或流程管理员）可见，防越权探查他人任务
        WfAuthUtil.requireOperateTask(task, "查看");
        return toVO(task);
    }

    /**
     * 扩展任务解析：按 {@code wf_ext_task.id} 查，排除 type=7 装饰行（非列表条目）。
     *
     * <p>列表 VO 的 id 对扩展任务即为 {@code wf_ext_task.id}；操作接口（detail/markViewed）
     * 先按 A 类 wf_task 解析，查不到再走此处，保证前端点击扩展任务条目不报「任务不存在」。</p>
     */
    private WfExtTask resolveExtTask(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return extTaskMapper.selectOne(Wrappers.<WfExtTask>lambdaQuery()
            .eq(WfExtTask::getId, taskId)
            .ne(WfExtTask::getType, WfExtTask.TYPE_DECORATION)
            .last("LIMIT 1"));
    }

    @Override
    public Map<String, Long> count(Long assignee) {
        Map<String, Long> result = new HashMap<>(4);
        // 记录级鉴权：非流程管理员只能看自己的角标数（顶栏待办红点）
        Long target = WfAuthUtil.resolveSelfIfNotAdmin(assignee);
        // 读源=act：角标计数读 ACT_*（含 wf_task 兜底，保证数字与翻源前一致）；失败降级走 wf_task
        if (taskActReader.actRead()) {
            try {
                return taskActReader.countFromAct(target);
            } catch (Exception e) {
                if (ledgerMode.isDirect()) {
                    // direct 模式下 wf_task 为空，降级读会得到空角标（用户以为没待办，比报错更危险），
                    // 故直接抛错暴露 ACT 读取故障，而不是静默返回空。
                    throw new IllegalStateException(
                        "读源=act 角标计数读取 ACT_* 失败，direct 模式禁止降级走空 wf_task. assignee=" + target, e);
                }
                log.warn("[blade-workflow] 读源=act 角标计数读取 ACT_* 失败，降级走 wf_task. assignee={}, {}",
                    target, e.getMessage());
            }
        }
        // 测试态任务不计入角标（方案 §6.4 C1 / V2）：否则顶栏红点会把测试单算进去，
        // 把用户引去「办理」一条根本不该出现在生产面的单子
        Long todo = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, target)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .eq(WfTask::getIsTest, 0));
        Long done = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, target)
            .ne(WfTask::getStatus, WfTask.STATUS_TODO)
            .eq(WfTask::getIsTest, 0));
        result.put("todo", todo == null ? 0L : todo);
        result.put("done", done == null ? 0L : done);
        return result;
    }

    // ------------------------------------------------------------------ 服务端真分页（2026-11-08）

    @Override
    public IPage<WfTaskVO> todoPage(Long assignee, WfTaskListFilter filter, long current, long size) {
        return page(assignee, filter, current, size, true);
    }

    @Override
    public IPage<WfTaskVO> donePage(Long assignee, WfTaskListFilter filter, long current, long size) {
        return page(assignee, filter, current, size, false);
    }

    /**
     * 待办/已办服务端分页（两源分段取数，2026-11-09 修正）。
     *
     * <p>{@code total} = ACT 段 count + 扩展任务段 count（同口径相加），绝不使用
     * 「减 seen」的错误公式（seen 只是当前页 id，不能用于全量计数）。</p>
     *
     * <p><b>分段取数</b>：把全量结果视为「ACT 段 [0, actTotal) + 扩展任务段
     * [actTotal, total)」，按全局 {@code offset} 决定本页从各段取多少行，
     * 保证<b>不丢行、不重复、深页可翻到扩展任务</b>。</p>
     *
     * <p><b>修正背景</b>：旧实现「扩展任务只在第一页并入、深页 offset 仍按 ACT 主表算」
     * 在造数验证中确认会丢单（done total=280、pageSize=200 时 80 条扩展任务全部翻不到）。
     * 代价是跨段不保证严格全局时间序（两源各自有序），但列表条数与 id 集合完整。</p>
     */
    private IPage<WfTaskVO> page(Long assignee, WfTaskListFilter filter, long current, long size, boolean onlyTodo) {
        Long target = WfAuthUtil.resolveSelfIfNotAdmin(assignee);
        WfTaskListFilter f = filter == null ? new WfTaskListFilter() : filter;
        if (f.getStatuses() == null || f.getStatuses().isEmpty()) {
            f.setStatuses(onlyTodo
                ? List.of(WfTask.STATUS_TODO)
                : List.of(WfTask.STATUS_DONE, WfTask.STATUS_FINISHED,
                    WfTask.STATUS_AUTO_SUBMIT, WfTask.STATUS_COADJUTANT,
                    WfTask.STATUS_CIRCULATE, WfTask.STATUS_READ));
        }
        long pageNo = Math.max(current, 1);
        long pageSize = Math.min(Math.max(size, 1), 200);
        long offset = (pageNo - 1) * pageSize;
        Page<WfTaskVO> result = new Page<>(pageNo, pageSize);

        if (taskActReader.actRead()) {
            try {
                long actTotal = onlyTodo
                    ? taskActReader.countTodoFromAct(target, f)
                    : taskActReader.countDoneFromAct(target, f);
                // 【去 wf_ 表 · 2026-11-09】扩展任务段：数据源由 wf_task 换成 wf_ext_task，
                // 与 countFromAct 的扩展段严格同口径（type 判据），保证角标与列表一致。
                boolean mergeResidue = residueMerge.isEnabled();
                long residueTotal = mergeResidue ? countResidue(target, f, onlyTodo) : 0L;
                long total = actTotal + residueTotal;
                result.setTotal(total);

                // 【两源分段取数】把全量结果看成「ACT 段 [0, actTotal) + 扩展段 [actTotal, total)」，
                // 按全局 offset 决定本页从哪一段取、取多少。旧实现「扩展任务只在第一页并入」会让
                // 深页永远翻不到扩展任务（造数实测：done total=280 但 pageSize=200 时 80 条扩展全丢）。
                List<WfTaskVO> records = new ArrayList<>();
                long actLimit = Math.min(pageSize, Math.max(0L, actTotal - offset));
                if (actLimit > 0) {
                    List<WfTaskVO> actRows = onlyTodo
                        ? taskActReader.todoPageFromAct(target, f, offset, actLimit)
                        : taskActReader.donePageFromAct(target, f, offset, actLimit);
                    // 【③ 第 8 项】自由流程步骤名从 wf_ext_task.ext_json 回读覆盖（ACT 无该列）
                    decorateFreeFlowNames(actRows);
                    records.addAll(actRows);
                }
                long extOffset = Math.max(0L, offset - actTotal);
                long extLimit = Math.min(pageSize - records.size(), Math.max(0L, residueTotal - extOffset));
                if (mergeResidue && extLimit > 0) {
                    List<WfExtTask> extRows = selectExtPage(target, f, onlyTodo, extOffset, extLimit);
                    for (WfExtTask t : extRows) {
                        records.add(toVO(t));
                    }
                }
                // 页内按时间倒序（两源各自已有序；跨段严格全局序不是本分页的契约，但不丢行/不重复）
                records.sort(java.util.Comparator.comparing(
                    v -> onlyTodo ? v.getReceiveTime() : v.getOperateTime(),
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
                result.setRecords(records);
                return result;
            } catch (Exception e) {
                if (ledgerMode.isDirect()) {
                    throw new IllegalStateException(
                        "读源=act 分页读取失败，direct 模式禁止降级走空 wf_task 分页. assignee=" + target, e);
                }
                log.warn("[blade-workflow] 读源=act 分页读取失败，降级走 wf_task 分页. assignee={}, {}",
                    target, e.getMessage());
            }
        }

        // 回退 wf_* 单源路径：MyBatis-Plus 物理分页，total 由 count 查询给出
        Page<WfTask> p = new Page<>(pageNo, pageSize);
        IPage<WfTask> raw = taskMapper.selectPage(p, wfTaskQuery(target, f.getStatuses(), f, null, false));
        List<WfTaskVO> records = new ArrayList<>(raw.getRecords().size());
        for (WfTask t : raw.getRecords()) {
            records.add(toVO(t));
        }
        result.setRecords(records);
        result.setTotal(raw.getTotal());
        return result;
    }

    /** 扩展任务段条数：与 {@code countFromAct} 的扩展段严格同口径（type 判据）。 */
    private long countResidue(Long target, WfTaskListFilter f, boolean onlyTodo) {
        Long n = extTaskMapper.selectCount(extTaskQuery(target, f.getStatuses(), f, null, onlyTodo));
        return n == null ? 0L : n;
    }

    /**
     * 扩展任务段分页（两源分段取数的第二段）。
     *
     * <p>复用 {@link #extTaskQuery} 的全部筛选条件（含 type 判据、草稿/测试态/逻辑删除排除、
     * 实例维度与时间范围），只追加物理分页，保证与 {@link #countResidue} 条数口径一致。</p>
     */
    private List<WfExtTask> selectExtPage(Long target, WfTaskListFilter f, boolean onlyTodo,
                                          long offset, long limit) {
        if (limit <= 0) {
            return java.util.Collections.emptyList();
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfExtTask> qw =
            extTaskQuery(target, f.getStatuses(), f, null, onlyTodo);
        // 二级排序 id 保证 LIMIT/OFFSET 稳定（同一时刻创建的扩展任务不会跨页重叠）
        qw.orderByDesc(WfExtTask::getId);
        qw.last("LIMIT " + limit + " OFFSET " + offset);
        return extTaskMapper.selectList(qw);
    }

    // ------------------------------------------------------------------ 私有方法

    private List<WfTaskVO> list(Long assignee, WfTaskListFilter filter) {
        // 记录级鉴权：非流程管理员一律只能查自己的待办/已办，忽略传入的 assignee
        Long target = WfAuthUtil.resolveSelfIfNotAdmin(assignee);
        List<Integer> statuses = filter.getStatuses();
        // 读源=act：待办读 ACT_RU_TASK、已办读 ACT_HI_TASKINST，不再回读 wf_task。
        // 失败自动降级走 wf_task —— 灰度期「列表可用」优先于「读源正确」。
        if (taskActReader.actRead() && statuses != null && !statuses.isEmpty()) {
            try {
                boolean onlyTodo = statuses.size() == 1 && statuses.get(0) != null
                    && statuses.get(0) == WfTask.STATUS_TODO;
                List<WfTaskVO> merged = new ArrayList<>(
                    onlyTodo ? taskActReader.todoFromAct(target, filter) : taskActReader.doneFromAct(target, filter));
                // 【③ 第 8 项】自由流程步骤名从 wf_ext_task.ext_json 回读覆盖（ACT 无该列）
                decorateFreeFlowNames(merged);
                // 【去 wf_ 表 · 2026-11-09】扩展任务段改由 wf_ext_task 读取（协办/抄送/传阅/
                // 已读/退回重提交），列表不再回读 wf_task。查询施加与 ACT 段同口径的筛选。
                if (residueMerge.isEnabled()) {
                    java.util.Set<Long> seen = merged.stream().map(WfTaskVO::getId)
                        .filter(v -> v != null).collect(java.util.stream.Collectors.toSet());
                    List<WfExtTask> residue = extTaskMapper.selectList(
                        extTaskQuery(target, statuses, filter, seen, onlyTodo));
                    if (!residue.isEmpty()) {
                        for (WfExtTask t : residue) {
                            merged.add(toVO(t));
                        }
                        log.info("[blade-workflow] 读源=act 合并 wf_ext_task 扩展任务 {} 条. assignee={}",
                            residue.size(), target);
                    }
                }
                // 合并后统一按时间倒序（待办按接收时间、已办按处理时间），与翻源前观感一致
                merged.sort(java.util.Comparator.comparing(
                    v -> onlyTodo ? v.getReceiveTime() : v.getOperateTime(),
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
                return merged;
            } catch (Exception e) {
                if (ledgerMode.isDirect()) {
                    throw new IllegalStateException(
                        "任务读源=act 读取 ACT_* 失败，direct 模式禁止降级走空 wf_task. assignee=" + target, e);
                }
                log.warn("[blade-workflow] 任务读源=act 读取 ACT_* 失败，降级走 wf_task. assignee={}, {}",
                    target, e.getMessage());
            }
        }
        // 回退 wf_* 路径：与 ACT 路径同口径施加全部筛选，保证翻源 / 降级行为一致。
        // 测试态任务不进生产「待办 / 已办」列表（方案 §6.4 C1 / V1、V3、C14）：
        // 含已办(2)/办结(3)/自动提交(4)/协办(7)/传阅(8)/已读(9) —— 整组排除，
        // 其中「已办」最易漏：代跑会在真人名下留一条已办，平时无人细看，漏了就长期存在。
        // 注：wf_task.is_test 为 tinyint NOT NULL DEFAULT 0，不存在 NULL 漏网。
        List<WfTask> tasks = taskMapper.selectList(wfTaskQuery(target, statuses, filter, null, false));
        List<WfTaskVO> result = new ArrayList<>(tasks.size());
        for (WfTask t : tasks) {
            result.add(toVO(t));
        }
        return result;
    }

    /**
     * 构造 wf_task 列表查询（回退路径 + ACT 路径的兜底残留合并共用）。
     * 统一施加：办理人、状态集、测试态排除，以及筛选条件中的<b>实例维度</b>（标题/流程定义/表单）
     * 与<b>时间范围</b>（待办=接收时间、已办=处理时间）。
     *
     * @param excludeIds 需排除的 ACT 已覆盖业务任务ID（兜底合并时传入，避免重复；null 不排）
     * @param extOnly    true=只取「非引擎任务本体」的行（blade 扩展任务 + 无引擎任务的合成待办）；
     *                   false=全量（读源=wf 的<b>回退路径必须全量</b>，否则降级后列表缺行）
     */
    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfTask> wfTaskQuery(
        Long target, List<Integer> statuses, WfTaskListFilter filter, java.util.Set<Long> excludeIds,
        boolean extOnly) {
        boolean onlyTodo = statuses != null && statuses.size() == 1
            && statuses.get(0) != null && statuses.get(0) == WfTask.STATUS_TODO;
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfTask> qw =
            Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getAssignee, target)
                .in(WfTask::getStatus, statuses)
                .eq(WfTask::getIsTest, 0);
        // 【精确判据 · ④ blade 6 态定案】extOnly=true 时只取「非引擎任务本体」的行：
        //   ① blade 扩展任务（协办7/抄送8/传阅11）—— 引擎无原生物，ACT 表达不了；
        //   ② 无引擎任务的合成待办（engineTaskId 为空）—— 草稿待办 / 退回发起人重提交。
        //
        // <p><b>与旧「兜底」判据的区别（关键）</b>：旧判据是「ACT 未覆盖」
        // （{@code id NOT IN (SELECT BIZ_TASK_ID_ FROM ACT_*)}），那会把<b>任何</b>漏双写的行
        // 一起补进来 → 既掩盖真实覆盖率缺口，又让开关语义变成"应急兜底"。
        // 精确判据只取<b>结构性</b>无法由 ACT 表达的行 → 不掩盖缺口、可长期维护。</p>
        if (extOnly) {
            // @deprecated 2026-11-09：扩展任务段已改由 extTaskQuery 读 wf_ext_task。
            // 本分支保留仅为读源=wf 全量回退路径的追溯/回滚，主流程恒传 extOnly=false。
            qw.and(w -> w.in(WfTask::getStatus, EXT_TASK_STATUSES)
                .or().isNull(WfTask::getEngineTaskId)
                .or().eq(WfTask::getEngineTaskId, ""));
        }
        // 【草稿移出待办 · 方案 A】排除草稿实例（status=5）的待办行。
        //
        // <p>草稿「已保存未提交：不占引擎，关联发起人待办」（WfInstance.STATUS_DRAFT 注释）——
        // 它靠一条 engineTaskId 为空的 wf_task 合成待办出现在「待办」里，而 ACT 侧永远没有这行。
        // 方案 A 把草稿改由「我的请求」的草稿箱承载（后端 /instance/mine 已能回退查出草稿），
        // 目的是让「待办」列表 100% 来自 ACT_*，从而移除 P6 退役 wf_task 的最后一个读源依赖。</p>
        //
        // <p><b>判据必须用「所属实例 status=5」而非「engineTaskId 为空」</b>：后者会误伤
        // 「退回发起人重提交」的合成待办 —— 那是<b>运行中</b>实例的正常待办，必须留在待办列表。</p>
        //
        // <p><b>为什么用 NOT EXISTS 而不是 NOT IN (子查询)</b>：{@code NOT IN} 遇 {@code inst_id IS NULL}
        // 会因三值逻辑（UNKNOWN）把该行<b>误排</b>；NOT EXISTS 语义正确（无匹配即保留）。
        // 子查询里的 {@code inst_id} 故意<b>不限定外层表名</b>：MyBatis-Plus 生成的外层查询
        // （{@code FROM wf_task}）默认无别名，不限定才能同时兼容「有/无别名」两种形态。</p>
        qw.notExists("SELECT 1 FROM wf_instance i WHERE i.id = inst_id "
            + "AND i.status = " + WfInstance.STATUS_DRAFT + " AND i.is_deleted = 0");
        // 前端列「状态」筛选下推：与 ACT 路径的 appendStatusFilter 同口径，保证 total 与记录一致
        if (filter != null && filter.getStatus() != null) {
            qw.eq(WfTask::getStatus, filter.getStatus());
        }
        // 显式实例范围（公文维度等）：instIds 直接给定（已从 wf_odoc_request 等算出），无需再查 wf_instance
        if (filter != null && filter.getInstIds() != null && !filter.getInstIds().isEmpty()) {
            qw.in(WfTask::getInstId, filter.getInstIds());
        }
        // 实例维度筛选：先取命中实例ID，再约束 wf_task.inst_id IN (...)。无命中则整体为空。
        if (filter != null && filter.hasInstanceFilter()) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfInstance> iq =
                Wrappers.<WfInstance>lambdaQuery();
            if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
                iq.like(WfInstance::getTitle, filter.getTitle());
            }
            if (filter.getDefId() != null) {
                iq.eq(WfInstance::getDefId, filter.getDefId());
            }
            if (filter.getFormId() != null) {
                iq.eq(WfInstance::getFormId, filter.getFormId());
            }
            List<Object> ids = instanceMapper.selectObjs(iq);
            List<Long> instIds = ids.stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
            if (instIds.isEmpty()) {
                // 没有任何实例命中筛选 → 直接返回「恒不成立」条件，避免全表扫描出无关任务
                qw.eq(WfTask::getId, -1L);
                return qw;
            }
            qw.in(WfTask::getInstId, instIds);
        }
        // 时间范围：待办按接收时间、已办按处理时间
        if (filter != null) {
            if (filter.getBeginTime() != null) {
                if (onlyTodo) {
                    qw.ge(WfTask::getReceiveTime, filter.getBeginTime());
                } else {
                    qw.ge(WfTask::getOperateTime, filter.getBeginTime());
                }
            }
            if (filter.getEndTime() != null) {
                if (onlyTodo) {
                    qw.le(WfTask::getReceiveTime, filter.getEndTime());
                } else {
                    qw.le(WfTask::getOperateTime, filter.getEndTime());
                }
            }
        }
        if (excludeIds != null && !excludeIds.isEmpty()) {
            qw.notIn(WfTask::getId, excludeIds);
        }
        qw.orderByDesc(WfTask::getCreateTime);
        return qw;
    }

    /**
     * 构造 <b>扩展任务段</b>的查询（去 wf_ 表 · 2026-11-09）。
     *
     * <p>替代原 {@code wfTaskQuery(extOnly=true)}：数据源由 {@code wf_task} 换成
     * {@code wf_ext_task}，判据由 {@code status IN (7,8,11) OR engine_task_id 为空}
     * 换成不可变的 {@code type} 集合 + 自身 {@code status}，与
     * {@code WfTaskActReader#countFromAct} 的两段 SQL <b>严格同口径</b>（角标必须与列表一致）：</p>
     * <ul>
     *   <li>待办段：{@code type IN (2,6) AND status=0}；</li>
     *   <li>已办段：{@code type IN (1,3,4,5)}（未读也进已办侧，与原 status=7/8/11 一致）；</li>
     *   <li>两段都排除 {@code type=7}（自由流程步骤名装饰行，非列表条目）。</li>
     * </ul>
     *
     * <p>实例维度/时间/状态筛选与 {@link #wfTaskQuery} 保持同口径，避免 ACT 段与扩展段
     * 筛选条件不一致导致「total 与记录不符」。租户与 {@code is_deleted=0} 由 MyBatis-Plus
     * 插件自动追加（本方法走 Mapper，非裸 SQL）。</p>
     */
    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfExtTask> extTaskQuery(
        Long target, List<Integer> statuses, WfTaskListFilter filter, java.util.Set<Long> excludeIds,
        boolean onlyTodo) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfExtTask> qw =
            Wrappers.<WfExtTask>lambdaQuery()
                .eq(WfExtTask::getAssignee, target)
                .eq(WfExtTask::getIsTest, 0)
                .ne(WfExtTask::getType, WfExtTask.TYPE_DECORATION);
        if (onlyTodo) {
            qw.in(WfExtTask::getType, WfExtTask.TYPE_ADD_SIGN, WfExtTask.TYPE_RESUBMIT)
                .eq(WfExtTask::getStatus, WfExtTask.STATUS_PENDING);
        } else {
            qw.in(WfExtTask::getType, WfExtTask.TYPE_COADJUTANT, WfExtTask.TYPE_CIRCULATE,
                WfExtTask.TYPE_READ, WfExtTask.TYPE_READED);
        }
        // 草稿实例的待办已由「我的请求」承载，与 wfTaskQuery / countFromAct 同口径排除
        qw.notExists("SELECT 1 FROM wf_instance i WHERE i.id = inst_id "
            + "AND i.status = " + WfInstance.STATUS_DRAFT + " AND i.is_deleted = 0");
        // 前端列「状态」筛选：按原 WfTask status 语义映射到 type（保证标签筛选零改动）
        if (filter != null && filter.getStatus() != null) {
            int st = filter.getStatus();
            if (st == WfTask.STATUS_COADJUTANT) {
                qw.eq(WfExtTask::getType, WfExtTask.TYPE_COADJUTANT);
            } else if (st == WfTask.STATUS_CIRCULATE) {
                qw.eq(WfExtTask::getType, WfExtTask.TYPE_CIRCULATE);
            } else if (st == WfTask.STATUS_READ) {
                qw.in(WfExtTask::getType, WfExtTask.TYPE_READ, WfExtTask.TYPE_READED);
            } else if (st == WfTask.STATUS_TODO) {
                qw.in(WfExtTask::getType, WfExtTask.TYPE_ADD_SIGN, WfExtTask.TYPE_RESUBMIT)
                    .eq(WfExtTask::getStatus, WfExtTask.STATUS_PENDING);
            } else {
                // 2/4/6 为引擎任务本体状态，扩展任务不可能命中 → 恒不成立
                qw.eq(WfExtTask::getId, -1L);
            }
        }
        if (filter != null && filter.getInstIds() != null && !filter.getInstIds().isEmpty()) {
            qw.in(WfExtTask::getInstId, filter.getInstIds());
        }
        if (filter != null && filter.hasInstanceFilter()) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfInstance> iq =
                Wrappers.<WfInstance>lambdaQuery();
            if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
                iq.like(WfInstance::getTitle, filter.getTitle());
            }
            if (filter.getDefId() != null) {
                iq.eq(WfInstance::getDefId, filter.getDefId());
            }
            if (filter.getFormId() != null) {
                iq.eq(WfInstance::getFormId, filter.getFormId());
            }
            List<Object> ids = instanceMapper.selectObjs(iq);
            List<Long> instIds = ids.stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
            if (instIds.isEmpty()) {
                qw.eq(WfExtTask::getId, -1L);
                return qw;
            }
            qw.in(WfExtTask::getInstId, instIds);
        }
        if (filter != null) {
            if (filter.getBeginTime() != null) {
                if (onlyTodo) {
                    qw.ge(WfExtTask::getReceiveTime, filter.getBeginTime());
                } else {
                    qw.ge(WfExtTask::getOperateTime, filter.getBeginTime());
                }
            }
            if (filter.getEndTime() != null) {
                if (onlyTodo) {
                    qw.le(WfExtTask::getReceiveTime, filter.getEndTime());
                } else {
                    qw.le(WfExtTask::getOperateTime, filter.getEndTime());
                }
            }
        }
        if (excludeIds != null && !excludeIds.isEmpty()) {
            qw.notIn(WfExtTask::getId, excludeIds);
        }
        qw.orderByDesc(WfExtTask::getCreateTime);
        return qw;
    }

    /**
     * 扩展任务 → 列表 VO。{@code status} 走 {@link WfExtTask#toWfTaskStatus()} 映射回
     * 原 WfTask status 语义（7协办/8抄送/11传阅/0待办/2已办），前端标签零改动。
     */
    private WfTaskVO toVO(WfExtTask t) {
        WfTaskVO vo = new WfTaskVO();
        vo.setId(t.getId());
        vo.setInstId(t.getInstId());
        vo.setNodeKey(t.getNodeKey());
        vo.setAssignee(t.getAssignee());
        vo.setStatus(t.toWfTaskStatus());
        vo.setReceiveTime(t.getReceiveTime());
        vo.setOperateTime(t.getOperateTime());
        vo.setDueTime(t.getDueTime());
        vo.setIsTest(t.getIsTest());
        WfInstance inst = t.getInstId() == null ? null : instanceMapper.selectById(t.getInstId());
        if (inst != null) {
            vo.setTitle(inst.getTitle());
            vo.setDefId(inst.getDefId());
            vo.setFormId(inst.getFormId());
            vo.setDataId(inst.getDataId());
            vo.setStarter(inst.getStarter());
            vo.setStartTime(inst.getStartTime());
            vo.setUrgency(inst.getUrgency());
            vo.setInstStatus(inst.getStatus());
            WfProcessNode node = loadNode(inst.getDefId(), t.getNodeKey());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
        }
        if (vo.getTitle() == null && t.getTitle() != null) {
            vo.setTitle(t.getTitle());
        }
        return vo;
    }

    private WfTaskVO toVO(WfTask t) {
        WfTaskVO vo = new WfTaskVO();
        vo.setId(t.getId());
        vo.setInstId(t.getInstId());
        vo.setNodeKey(t.getNodeKey());
        vo.setAssignee(t.getAssignee());
        vo.setStatus(t.getStatus());
        vo.setReceiveTime(t.getReceiveTime());
        vo.setOperateTime(t.getOperateTime());
        vo.setDueTime(t.getDueTime());
        // 测试态标记透出（C3）：前端据此识别「这是测试任务」并置只读 / 加标识
        vo.setIsTest(t.getIsTest());

        WfInstance inst = instanceMapper.selectById(t.getInstId());
        if (inst != null) {
            vo.setTitle(inst.getTitle());
            vo.setDefId(inst.getDefId());
            vo.setFormId(inst.getFormId());
            vo.setDataId(inst.getDataId());
            vo.setStarter(inst.getStarter());
            vo.setStartTime(inst.getStartTime());
            vo.setUrgency(inst.getUrgency());
            vo.setInstStatus(inst.getStatus());
            // ⚠️ 必须带 defId：bpmn-js 的 id（Activity_xxx 等）在不同流程间会重复，
            //    只按 nodeKey 查会命中别的流程的节点，待办/已办列表节点名串味。
            // 复用 loadNode：定义源开关（BPMN / wf_process_node）在此统一收口，避免两处读法漂移
            WfProcessNode node = loadNode(inst.getDefId(), t.getNodeKey());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
            // 自由流程：优先用待办上冗余的自定义步骤名（首人/其余人由 Driver 统一写入），
            // 否则回退实例步骤表，避免「首人显示静态名、其他人显示自定义名」。
            String displayName = freeDisplayName(t.getExtJson());
            if (displayName == null && node != null) {
                WfProcessNode shown = freeNodeResolver.decorate(freeFlowDriver.context(inst), node, t.getNodeKey());
                if (shown != null && shown.getNodeName() != null) {
                    displayName = shown.getNodeName();
                }
            }
            if (displayName != null && !displayName.isBlank()) {
                vo.setNodeName(displayName);
            }
        }
        return vo;
    }

    /** 从 wf_task.ext_json 解析自由流程自定义步骤名；无配置/解析失败返回 null */
    private String freeDisplayName(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode j =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(extJson);
            return j.hasNonNull("nodeName") ? j.get("nodeName").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 自由流程步骤显示名回填（ACT 读源路径专用）。
     *
     * <p>ACT→VO 映射里 {@code nodeName} 取自引擎 {@code NAME_}（静态节点池名），而自由流程的
     * 步骤显示名（如「部门经理审批-张三自定义」）由 {@code WfFreeFlowDriver.stampTaskExtJson}
     * 统一写入 {@code wf_task.ext_json.nodeName}（ACT 无对应列）。开关=act 时待办/已办若直接从 ACT 取，
     * 会丢失步骤名、退化为引擎节点名。</p>
     *
     * <p><b>【去 wf_ 表 · 2026-11-09】</b>步骤显示名已迁至 {@code wf_ext_task} 的
     * {@code type=7} 装饰行：按 {@code biz_task_id}（即宿主 {@code wf_task.id} / 列表 VO 的 id）
     * 批量回读 {@code ext_json}。自由流程默认关闭、非自由流程任务无装饰行 → 覆盖不生效，零行为变化。
     * 装饰行由 {@code WfFreeFlowDriver#stampTaskExtJson} 写入，不是列表条目。</p>
     */
    private void decorateFreeFlowNames(List<WfTaskVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>(vos.size());
        for (WfTaskVO v : vos) {
            if (v != null && v.getId() != null) {
                ids.add(v.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        List<WfExtTask> rows = extTaskMapper.selectList(Wrappers.<WfExtTask>lambdaQuery()
            .select(WfExtTask::getBizTaskId, WfExtTask::getExtJson)
            .eq(WfExtTask::getType, WfExtTask.TYPE_FREE_FLOW)
            .in(WfExtTask::getBizTaskId, ids));
        if (rows.isEmpty()) {
            return;
        }
        Map<Long, String> extMap = new HashMap<>(rows.size());
        for (WfExtTask r : rows) {
            if (r.getBizTaskId() != null) {
                extMap.put(r.getBizTaskId(), r.getExtJson());
            }
        }
        for (WfTaskVO v : vos) {
            if (v == null || v.getId() == null) {
                continue;
            }
            String name = freeDisplayName(extMap.get(v.getId()));
            if (name != null && !name.isBlank()) {
                v.setNodeName(name);
            }
        }
    }

    // ------------------------------------------------------------------ 节点信息运行时消费

    /** 加载节点（拿不到返回 null，按「未配置」处理，不阻断流转） */
    private WfProcessNode loadNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        // 定义源=BPMN：节点维度（nodeType/signOrder/allowReject/extJson…）改从 wf:node 扩展读。
        // BPMN 里没有该节点（或读取异常）时回退 wf_process_node，保证流转不被阻断。
        if (definitionFromBpmn) {
            try {
                WfProcessNode fromBpmn = bpmnReader.node(defId, nodeKey);
                if (fromBpmn != null) {
                    return fromBpmn;
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 定义源=BPMN 读取节点失败，回退 wf_process_node. "
                    + "defId={}, nodeKey={}, {}", defId, nodeKey, e.getMessage());
            }
        }
        // T-14 退役：主开关开启时禁用 wf_process_node 回退读（BPMN 为唯一源；草稿/缺扩展返回 null）。
        if (!retirement.isEnabled()) {
            return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
        }
        return null;
    }

    /** 加载定义全部节点（只读收口）：开关开启时读 BPMN wf:node，回退 wf_process_node；退役开启时禁用回退 */
    private List<WfProcessNode> loadNodes(Long defId) {
        if (defId == null) {
            return new ArrayList<>();
        }
        if (definitionFromBpmn) {
            try {
                List<WfProcessNode> nodes = bpmnReader.nodes(defId);
                if (nodes != null && !nodes.isEmpty()) {
                    return nodes;
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 定义读源=BPMN 取节点失败，回退 wf_process_node. defId={}, {}", defId, e.getMessage());
            }
        }
        if (!retirement.isEnabled()) {
            return nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
        }
        return new ArrayList<>();
    }

    /**
     * 校验该节点的「操作菜单」是否允许某操作。
     * 未配置菜单（null）表示不限制，保持既有行为。
     */
    private void requireOperate(WfProcessNode node, String menu) {
        if (!WfNodeSettingsUtil.allowOperate(node, menu)) {
            throw new ServiceException("当前节点未开放该操作：" + menu);
        }
    }

    /**
     * 富文本意见的「空」判定：编辑器里的空内容是 {@code <p><br></p>} 这类标签，
     * 直接 trim 会判成非空，使「意见必填」形同虚设（前端同口径，见 isRichTextEmpty）。
     */
    private static boolean isBlankOpinion(String opinion) {
        if (opinion == null) {
            return true;
        }
        return opinion
            .replaceAll("(?i)<br\\s*/?>", "")
            .replace("&nbsp;", "")
            .replaceAll("<[^>]*>", "")
            .trim()
            .isEmpty();
    }

    /** 意见处理：必填校验 + 缺省套用「签字意见设置」的默认模板 */
    private String resolveOpinion(WfProcessNode node, String opinion) {
        boolean blank = isBlankOpinion(opinion);
        if (blank) {
            if (WfNodeSettingsUtil.opinionRequired(node)) {
                throw new ServiceException("该节点必须填写签字意见");
            }
            String tpl = WfNodeSettingsUtil.opinionTemplate(node);
            return tpl == null ? "" : tpl;
        }
        return opinion;
    }

    /**
     * 「直达本节点」退回的重新提交跳转：实例带有退回标记（引擎变量
     * {@link #VAR_RESUBMIT_DIRECT_NODE} = 执行退回的节点Key）时，把引擎当前节点
     * 的 token 直接移动到该节点（跳过中间节点），并清除标记。
     *
     * <p>与 {@link #reject} 的回退同一机制（{@code moveActivity} + {@code advance}），
     * 中间节点不产生待办、不触发附加操作 —— 即 ecology「退回后提交直达本节点」语义。</p>
     *
     * @return true=已执行直达跳转（调用方跳过常规 advance）；false=无标记或不适用（逐级审批）
     */
    private boolean jumpDirectIfMarked(WfInstance inst, Long operator) {
        Object mark = processService.getVariable(inst.getEngineInstId(), VAR_RESUBMIT_DIRECT_NODE);
        if (mark == null || String.valueOf(mark).isBlank()) {
            return false;
        }
        String directNodeKey = String.valueOf(mark);
        // 先清标记再跳转：无论后续是否成功，绝不让标记残留到再下一次提交
        processService.removeVariables(inst.getEngineInstId(), List.of(VAR_RESUBMIT_DIRECT_NODE));
        if (loadNode(inst.getDefId(), directNodeKey) == null) {
            log.warn("[blade-workflow] 直达本节点跳转失败：目标节点已不存在，降级为逐级审批. "
                + "instId={}, directNodeKey={}", inst.getId(), directNodeKey);
            return false;
        }
        // complete 之后 token 落在下一节点；多 token（并行网关）或已到终点时无法安全直达，
        // 一律降级为逐级审批（标记已清，不会影响后续提交）
        List<TaskVO> current = processService.currentTasks(inst.getEngineInstId());
        if (current.size() != 1) {
            log.info("[blade-workflow] 直达本节点不适用（当前引擎活动任务数={}），按逐级审批继续. instId={}",
                current.size(), inst.getId());
            return false;
        }
        String fromKey = current.get(0).getTaskDefinitionKey();
        if (directNodeKey.equals(fromKey)) {
            // 已直达目标节点（如退回目标就是下一节点），无需跳转
            return false;
        }
        // 与退回同口径：moveActivity + advance(REJECT)（退回链路不做「流程异常处理」兜底）
        processService.moveActivity(inst.getEngineInstId(), fromKey, directNodeKey, null);
        instanceService.advance(inst.getId(), operator, null, null, AdvanceSrc.REJECT);
        log.info("[blade-workflow] 退回后重新提交直达原节点. instId={}, from={}, to={}, operator={}",
            inst.getId(), fromKey, directNodeKey, operator);
        return true;
    }

    /**
     * 实例是否为测试态（{@code wf_instance.is_test=1}）。
     *
     * <p>测试态实例与正式实例<b>同库同表</b>，仅靠 {@code is_test} 区分；任何会「外溢到生产」
     * 的写操作（给真人建任务、写生产可见留痕、触发外部接口）都必须先过这一层。</p>
     */
    private boolean isTestInst(WfInstance inst) {
        return inst != null && inst.getIsTest() != null && inst.getIsTest() == 1;
    }

    /** 任务所属实例是否为测试态（见 {@link #isTestInst}） */
    private boolean isTestTask(WfTask task) {
        if (task == null || task.getInstId() == null) {
            return false;
        }
        return isTestInst(instanceMapper.selectById(task.getInstId()));
    }

    /**
     * 测试态守卫：拒绝在测试实例上执行「会外溢到生产」的动作（方案 §6.4 C16）。
     *
     * <p><b>为什么必须在后端兜底</b>：前端已在测试面板禁用这些按钮（C6），但
     * 「前端禁用 ≠ 后端拒绝」——直接调接口照样能触发。而这几类动作的共同点是
     * <b>会向生产用户产生东西</b>：转办/加签给真人新建待办、传阅给真人建已办条目、
     * 催办在真人名下写留痕；且新建的任务还存在「标记断层」（V10/C2）。</p>
     *
     * <p>注：退回（{@link #reject}）不走本守卫——退回是流程测试需要验证的路径，
     * 改为「允许执行 + 跳过副作用 + 留痕用接收人」。</p>
     */
    private void assertNotTestTask(WfTask task, String action) {
        if (isTestTask(task)) {
            throw new ServiceException("测试流程不支持「" + action
                + "」：该操作会给真实用户产生任务或留痕，请在正式流程中使用");
        }
    }

    /**
     * 解析任务：先按业务主键（wf_task.id 雪花）查，查不到再按引擎任务ID（engine_task_id）回退一次。
     *
     * <p><b>为什么需要回退</b>：消息中心 / 流程通知（{@code WfProcessNoticeListener} 监听 Flowable
     * TASK_CREATED 事件）写入的 bizRefId 取自 Flowable 引擎 {@code Task.getId()}，即 engine_task_id
     * （如 453214），而本系统一切任务操作均以 wf_task.id 为入口。若不兼容，用户点开「待办通知」会
     * 因 selectById(engine_task_id) 查不到而全程报「任务不存在」。</p>
     * <p>回退安全：engine_task_id 为 Flowable 自增整数，与 19 位雪花主键无碰撞可能。</p>
     */
    private WfTask resolveTask(Long taskId) {
        if (taskId == null) {
            return null;
        }
        // 批次 1：改按 BIZ_TASK_ID_ 反查 ACT（模式 A），未命中按引擎 ID_ 兜底
        // （模式 B：消息中心 bizRefId / 超时通知传入的 Flowable Task.getId()）。
        // 前端与消息中心的 id 契约保持不变。
        return bizResolver.resolve(taskId);
    }

    private WfTask requireTodoTask(Long taskId) {
        WfTask task = resolveTask(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在（可能该流程已被删除），请刷新页面后重试");
        }
        if (!Integer.valueOf(WfTask.STATUS_TODO).equals(task.getStatus())) {
            throw new ServiceException(staleTaskReason(task));
        }
        return task;
    }

    /**
     * 任务已办结时的拒绝原因（「界面状态与实例状态不一致」规则在办理入口的口径）。
     *
     * <p>浏览器里已打开的办理页若未刷新，流程可能已被退回、被他人流转、被撤回或归档，
     * 此时本节点任务已办结。这里把笼统的「任务非待办状态」翻译成用户能据以行动的原因：
     * 说清流程现在停在哪，并提示刷新页面，避免用户看到「不可处理」后反复重试。</p>
     */
    private String staleTaskReason(WfTask task) {
        WfInstance inst = instanceMapper.selectById(task.getInstId());
        if (inst == null) {
            return "本页已过期：该流程已不存在（可能已被删除），请刷新页面后重试";
        }
        Integer st = inst.getStatus();
        if (st != null && st == WfInstance.STATUS_APPROVED) {
            return "本页已过期：该流程已归档结束，无法再办理，请刷新页面查看最新结果";
        }
        if (st != null && st == WfInstance.STATUS_CANCELED) {
            return "本页已过期：该流程已被撤回/撤销，无法再办理，请刷新页面查看最新结果";
        }
        if (st != null && st == WfInstance.STATUS_REJECTED) {
            return "本页已过期：该流程已不通过结束，无法再办理，请刷新页面查看最新结果";
        }
        String cur = inst.getCurrentNodeKey();
        if (cur == null || cur.isEmpty()) {
            return "本页已过期：该任务已办结，流程已结束，请刷新页面查看最新结果";
        }
        return "本页已过期：该任务已办结，流程当前节点已变更为「" + nodeNameOf(inst.getDefId(), cur)
            + "」（可能已被退回或被他人流转），请刷新页面后重新办理";
    }

    /** 节点Key → 节点名称（查不到或异常时回退节点Key，本方法只用于提示语，绝不抛错） */
    private String nodeNameOf(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null || nodeKey.isEmpty()) {
            return nodeKey;
        }
        try {
            // T-10 B4：经 gated loadNode（definitionFromBpmn 走 BPMN、retirement 关时回退 wf_process_node）
            WfProcessNode node = loadNode(defId, nodeKey);
            return (node != null && node.getNodeName() != null && !node.getNodeName().isEmpty())
                ? node.getNodeName() : nodeKey;
        } catch (Exception e) {
            log.warn("[blade-workflow] 取节点名称失败，回退节点Key. defId={}, nodeKey={}", defId, nodeKey, e);
            return nodeKey;
        }
    }

    /**
     * 解析节点的签批方式（{@link #SIGN_ANY 或签} / {@link #SIGN_ALL 会签} / {@link #SIGN_SEQUENCE 依次}）。
     *
     * <p>⚠️ 必须同时看「操作组级会签属性」（{@code wf_node_operator.sign_order}）：
     * 「节点信息 → 操作者 → 添加操作组」里选的会签/依次**只写 wf_node_operator**，
     * 而节点级「审批方式」（{@code wf_process_node.sign_order}）往往仍是默认 0（或签）。
     * 若只读节点级，就会出现「配了会签，却一人通过即推进引擎/归档」。</p>
     *
     * <p>合并规则（两层取更严格者）：任一层为会签 → 会签；否则任一层为依次 → 依次；否则或签。</p>
     */
    /**
     * 判定某节点在运行期是否由引擎多实例驱动（与 WfDefinitionServiceImpl 部署期注入口径一致）。
     * 开关关闭 → 永不；开关开启 → 仅审批/提交类节点（nodeType ∈ {1,2}）走引擎多实例。
     */
    private boolean isEngineMultiInstance(WfInstance inst, String nodeKey) {
        if (!multiInstanceEnabled || inst == null || inst.getDefId() == null || nodeKey == null) {
            return false;
        }
        WfProcessNode node = loadNode(inst.getDefId(), nodeKey);
        if (node == null || node.getNodeType() == null) {
            return false;
        }
        return Integer.valueOf(1).equals(node.getNodeType()) || Integer.valueOf(2).equals(node.getNodeType());
    }

    /**
     * 把全部 MI 节点的集合变量 {@code wfMiAssignees_<nodeKey> = List<Long>} 注入流转变量，
     * 保证进入任一多实例节点前集合已就绪（引擎据此逐人展开并行/串行循环）。
     * 幂等、持久（流程变量跨节点保留），每个流转点重算一次即可覆盖后续所有 MI 节点。
     *
     * <p>⚠️ 首节点即多实例且操作者依赖实例上下文（instId）时，发起阶段 instId 可能尚为空，
     * 此时按静态节点配置解析（多数场景足够）；动态（依赖表单/实例）首节点多实例为边界情况，需回归验证。</p>
     */
    private void injectMiCollectionVars(Long defId, WfInstance inst, Map<String, Object> vars, Long currentOperator) {
        if (!multiInstanceEnabled || vars == null || defId == null) {
            return;
        }
        try {
            // T-10 B4：经 gated loadNodes（definitionFromBpmn 走 BPMN、retirement 关时回退 wf_process_node）
            List<WfProcessNode> nodes = loadNodes(defId);
            for (WfProcessNode n : nodes) {
                if (n.getNodeType() == null || (n.getNodeType() != 1 && n.getNodeType() != 2)) {
                    continue;
                }
                String nk = n.getNodeKey();
                if (nk == null || nk.isBlank()) {
                    continue;
                }
                Long instId = (inst == null) ? null : inst.getId();
                Long starter = (inst == null) ? null : inst.getStarter();
                List<Long> assignees = operatorResolver.resolve(defId, nk, instId, starter, currentOperator);
                if (assignees == null) {
                    assignees = List.of();
                }
                // D17/V6 补偿：驳回/重入不得抹掉运行期加签人。
                // 若引擎中该集合变量已存在（含 addSign 经 appendMiAssignee 追加的人），以「已有运行期
                // 办理人」为基准并集「配置操作者」写回，而非按配置覆盖——
                // 否则 reject/doApprove 重入时 injectMiCollectionVars 会把 wfMiAssignees_<nodeKey>
                // 重算成纯配置操作者，丢掉 addSign 写回的人，致「加签→驳回→重提」丢加签人
                // （WfAddSignMiSpikeTest.writeBackCollectionKeepsAddedAssigneeAfterReject 的假阳性即源于
                // 测试桩 moveActivity 传空 vars 未触发该覆盖）。变量尚不存在（发起首入）时退化为纯配置。
                String miVar = "wfMiAssignees_" + nk;
                Object existing = (inst != null && inst.getEngineInstId() != null)
                    ? runtimeService.getVariable(inst.getEngineInstId(), miVar) : null;
                vars.put(miVar, mergeMiAssignees(existing, assignees));
            }
        } catch (Exception e) {
            log.warn("[WfTaskServiceImpl] 注入多实例集合变量失败（忽略，节点进入时可能缺集合而失败）: {}", e.getMessage());
        }
    }

    /**
     * D17/V6 补偿核心：合并「已有运行期办理人」与「配置操作者」，避免驳回/重入按配置覆盖丢加签人。
     *
     * <p>已有引擎集合变量（含 addSign 经 {@link #appendMiAssignee} 追加的人）优先保留；
     * 配置操作者中尚未出现者补入（覆盖「发起后新增配置操作者」的场景）。变量不存在（发起首入）时
     * 退化为纯配置。元素按 {@code String} 值判等，兼容引擎变量 Long / String 两种形态。</p>
     *
     * <p>纯函数、无外部依赖，便于单测（见 {@code WfTaskServiceImplMiMergeTest}）。</p>
     */
    public static java.util.List<Object> mergeMiAssignees(Object existingVar, java.util.List<Long> configAssignees) {
        java.util.List<Object> merged = new java.util.ArrayList<>();
        if (existingVar instanceof java.util.Collection) {
            for (Object o : (java.util.Collection<?>) existingVar) {
                if (o != null) {
                    merged.add(o);
                }
            }
        }
        if (configAssignees != null) {
            for (Long a : configAssignees) {
                boolean present = false;
                for (Object o : merged) {
                    if (String.valueOf(o).equals(String.valueOf(a))) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    merged.add(a);
                }
            }
        }
        return merged;
    }

    private int resolveSignOrder(Long defId, String nodeKey) {
        WfProcessNode node = loadNode(defId, nodeKey);
        int nodeLevel = (node == null || node.getSignOrder() == null) ? SIGN_ANY : node.getSignOrder();
        boolean all = nodeLevel == SIGN_ALL;
        boolean sequence = nodeLevel == SIGN_SEQUENCE;
        if (node != null && node.getId() != null) {
            // wf_ 源：节点来自 wf_process_node，id 非空，按 nodeId 读操作组签序
            List<WfNodeOperator> ops = operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
                .eq(WfNodeOperator::getNodeId, node.getId()));
            if (ops != null) {
                for (WfNodeOperator op : ops) {
                    Integer so = op.getSignOrder();
                    if (so == null) {
                        continue;
                    }
                    if (so == SIGN_ALL) {
                        all = true;
                    } else if (so == SIGN_SEQUENCE) {
                        sequence = true;
                    }
                }
            }
        } else if (definitionFromBpmn) {
            // T-10 B4 #3：BPMN 源 node.id 恒为 null，旧逻辑 node.getId()!=null 分支永远不进
            // → 操作组级会签/依次被静默丢失。改为按 nodeKey 从 BPMN 扩展读取操作者签序（修复 bug）
            try {
                List<BpmnExtensionUtil.WfOperatorExt> ops = bpmnReader.operators(defId, nodeKey);
                if (ops != null) {
                    for (BpmnExtensionUtil.WfOperatorExt op : ops) {
                        Integer so = parseSignOrder(op.signOrder);
                        if (so == null) {
                            continue;
                        }
                        if (so == SIGN_ALL) {
                            all = true;
                        } else if (so == SIGN_SEQUENCE) {
                            sequence = true;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[WfTaskServiceImpl] 解析操作组级签序(BPMN)失败，忽略. defId={}, nodeKey={}", defId, nodeKey, e);
            }
        }
        if (all) {
            return SIGN_ALL;
        }
        return sequence ? SIGN_SEQUENCE : SIGN_ANY;
    }

    private static Integer parseSignOrder(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long countPending(Long instId, String nodeKey) {
        // direct 模式：自研会签已退役（Mi 化由引擎多实例门禁处理），且 wf_task 为空；
        // 本方法仅在 !isEngineMultiInstance 守卫内调用，direct 下直接返回 0 避免读空台账。
        if (ledgerMode.isDirect()) {
            return 0L;
        }
        Long count = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        return count == null ? 0L : count;
    }

    private WfTask nextPending(Long instId, String nodeKey) {
        if (ledgerMode.isDirect()) {
            return null;
        }
        return taskMapper.selectOne(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .orderByAsc(WfTask::getId)
            .last("LIMIT 1"));
    }

    private void closeSiblings(Long instId, String nodeKey, Long excludeTaskId) {
        // direct 模式：自研会签退役，wf_task 为空；或签关兄弟任务在 Mi 化下由引擎删除事件
        // (WfStateProjector.onTaskDeleted) 覆盖，此处 no-op 即可，避免读空台账。
        if (ledgerMode.isDirect()) {
            return;
        }
        List<WfTask> siblings = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .ne(WfTask::getId, excludeTaskId));
        for (WfTask s : siblings) {
            s.setStatus(WfTask.STATUS_FINISHED);
            s.setOperateTime(new Date());
            // 批次 1：不再更新 wf_task 台账；或签办结同节点其余待办 → 子状态直接同步到 ACT_*
            taskActWriter.sync(s);
        }
    }

    private void appendLog(Long instId, Long taskId, String nodeKey, Long operator,
                           String logType, String opinion) {
        // 委托 WfWriteHelper 唯一实现：wf_approval_log + ACT_HI_COMMENT 审批意见双写（P3-4）。
        // 此前本类自带 logMapper.insert 直写、绕过双写，导致引擎侧评论恒为空、读源=act 时流转意见无数据。
        writeHelper.appendLog(instId, taskId, nodeKey, operator, logType, opinion);
    }

    // ------------------------------------------------------------------ 二次认证 + 字段校验

    /**
     * 节点信息 → 运行时消费：同意前校验「二次认证」与「节点字段校验」。失败即抛异常拒绝。
     */
    private void validateBeforeApprove(WfProcessNode node, WfInstance inst, Long operator, ApproveDTO dto, boolean system) {
        // 二次认证：需重新校验密码（系统自动通过跳过）
        if (!system && WfNodeSettingsUtil.secondAuth(node)) {
            String password = dto == null ? null : dto.getPassword();
            if (password == null || password.isEmpty()) {
                throw new ServiceException("该节点需要二次认证，请重新输入密码");
            }
            R<Boolean> r;
            try {
                r = userClient.verifyPassword(operator, password);
            } catch (Exception e) {
                throw new ServiceException("二次认证服务不可用，请稍后重试");
            }
            if (r == null || !r.isSuccess() || !Boolean.TRUE.equals(r.getData())) {
                throw new ServiceException("二次认证失败：密码错误");
            }
        }
        // 字段校验：按规则校验最新表单快照
        List<String> rules = WfNodeSettingsUtil.fieldCheckRules(node);
        if (!rules.isEmpty()) {
            Map<String, Object> data = loadLatestFormData(inst.getId());
            for (String rule : rules) {
                checkFieldRule(data, rule);
            }
        }
    }

    /** 从 Flowable 的 "Unknown property used in expression: ${xxx <= 3000}" 里提取首个变量名，用于中文报错 */
    private static String extractUelVar(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_]*)").matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 网关出口条件（UEL）变量兜底：仅作用于「本次引擎提交的 variables」。
     *
     * <p><b>问题</b>：正式办理页提交时只上传「渲染包快照 + 本节点表单改动」。若某出口条件引用的字段
     * 从未下发（脚本/接口发起的实例、该节点字段只读未回传、前序节点才填的值），Flowable 在排他网关
     * 求值时抛 {@code Unknown property used in expression: ${eeramt <= 3000}}，整个提交 400 且流程卡死。</p>
     *
     * <p><b>口径（与测试态严格区分）</b>：测试态 {@code WfTestServiceImpl.deriveValues} 会反推「满足条件」
     * 的样本值（如 eeramt=5000）只为跑分支覆盖；正式态绝不能注入这种假值去决定真实审批走向。
     * 这里按以下优先级补齐，保证 UEL 可求值即可：</p>
     * <ol>
     *   <li>前端本次已提交的变量 → 原样保留（真实值优先，绝不覆盖）；</li>
     *   <li>引擎实例上已有的持久变量 → 取真实值回填；</li>
     *   <li>仍缺失 → 补中性默认值 0（让比较可求值；金额类条件会落到最低档/默认分支），并 warn 留痕。</li>
     * </ol>
     *
     * <p><b>安全边界</b>：本方法在 {@code validateBeforeApprove} 之后调用，只修改 {@code vars}，
     * 不写回 {@code wf_form_snapshot} / 业务表、不参与必填校验，因此不存在「样本值绕过金额必填」问题。</p>
     */
    private void supplyMissingConditionVars(Long defId, String engineInstId, Map<String, Object> vars) {
        if (defId == null || vars == null) {
            return;
        }
        List<WfNodeLink> links;
        try {
            links = linkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
                .eq(WfNodeLink::getDefId, defId)
                .isNotNull(WfNodeLink::getConditionExpr));
        } catch (Exception e) {
            log.warn("[blade-workflow] 读取出口条件失败，跳过 UEL 变量兜底. defId={}, {}", defId, e.getMessage());
            return;
        }
        if (links == null || links.isEmpty()) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (WfNodeLink l : links) {
            names.addAll(WfConditionVarUtil.extract(l.getConditionExpr()));
        }
        for (String name : names) {
            if (vars.containsKey(name)) {
                continue;
            }
            Object existing = processService.getVariable(engineInstId, name);
            if (existing != null) {
                vars.put(name, existing);
                continue;
            }
            vars.put(name, 0L);
            log.warn("[blade-workflow] 出口条件变量未随提交下发，已补中性默认值 0（仅引擎求值，不写回表单）. "
                    + "defId={}, engineInstId={}, var={}", defId, engineInstId, name);
            WfNoticeContext.add("出口条件引用的字段【" + name + "】未随本次表单提交下发，"
                + "已按默认值 0 参与条件判断，审批路径可能与预期不符；请检查："
                + "①该字段是否已在表单中配置 ②该字段是否设为必填 ③出口条件引用的字段名是否正确");
        }
    }

    /** 取实例最新表单快照数据（无则空 map） */
    private Map<String, Object> loadLatestFormData(Long instId) {
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .orderByDesc(WfFormSnapshot::getId)
            .last("LIMIT 1"));
        if (snap == null || snap.getDataJson() == null || snap.getDataJson().isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> data = JsonUtil.parse(snap.getDataJson(), Map.class);
            return data == null ? Map.of() : data;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 校验单条规则：字段名:required / regex= / min= / max= */
    private void checkFieldRule(Map<String, Object> data, String rule) {
        int idx = rule.indexOf(':');
        if (idx <= 0) {
            return;
        }
        String field = rule.substring(0, idx).trim();
        String expr = rule.substring(idx + 1).trim();
        Object val = data.get(field);
        String sval = val == null ? null : String.valueOf(val);
        boolean blank = sval == null || sval.isEmpty();
        if ("required".equals(expr)) {
            if (blank) {
                throw new ServiceException("字段校验失败：" + field + " 不能为空");
            }
        } else if (expr.startsWith("regex=")) {
            String pattern = expr.substring("regex=".length());
            if (!blank && !sval.matches(pattern)) {
                throw new ServiceException("字段校验失败：" + field + " 格式不正确");
            }
        } else if (expr.startsWith("min=")) {
            if (!blank && toNumber(sval) < parseDouble(expr.substring("min=".length()))) {
                throw new ServiceException("字段校验失败：" + field + " 小于最小值");
            }
        } else if (expr.startsWith("max=")) {
            if (!blank && toNumber(sval) > parseDouble(expr.substring("max=".length()))) {
                throw new ServiceException("字段校验失败：" + field + " 大于最大值");
            }
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

    /** 把对象（字符串/数字）解析为 Long ID；非法返回 null */
    private static Long parseId(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static double toNumber(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

}
