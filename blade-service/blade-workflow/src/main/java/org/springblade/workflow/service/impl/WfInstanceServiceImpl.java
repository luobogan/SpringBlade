package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.resolver.WfOperatorResolver;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.TaskVO;
import org.springblade.workflow.vo.WfNodeOperatorVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程实例语义服务实现
 *
 * <p>wf_instance / wf_form_snapshot / wf_approval_log 的写入均在此完成；
 * 引擎侧一律通过 {@link IProcessService} 调用，保证语义层与引擎解耦。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfInstanceServiceImpl implements IWfInstanceService {

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfApprovalLogMapper logMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final IProcessService processService;
    private final WfOperatorResolver operatorResolver;
    private final WfNodeLinkMapper linkMapper;
    private final NodeActionExecutor nodeActionExecutor;
    private final IFormmodeClient formmodeClient;

    /**
     * 表单直发（发起流程页）时创建业务数据行，返回其 id 作为 dataId。
     *
     * <p>跨服务调 blade-formmode 的 {@code POST /form-data/save-by-form}：表名取
     * {@code workflow_bill.table_name}（迁移表单表名与表单ID不同，如 2064530495200337922 →
     * {@code formtable_main_5}），字段值按字段名写入真实存在的列。</p>
     *
     * <p>失败返回 null（由调用方回退占位 dataId）：流程仍可正常发起/审批/归档，只是业务表里
     * 没有对应行，异常记 warn 日志，避免表单建模服务抖动把发起流程一起拖垮。</p>
     */
    private Long createBusinessData(WfProcessDefinition def, StartProcessDTO dto) {
        Long formId = resolveFormId(def, dto);
        if (formId == null) {
            log.warn("[blade-workflow] 发起未携带表单ID，无法创建业务数据，回退占位 dataId");
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>(16);
        if (dto.getFieldValues() != null) {
            values.putAll(dto.getFieldValues());
        }
        if (dto.getVariables() != null) {
            values.putAll(dto.getVariables());
        }
        try {
            FormDataSaveDTO saveDto = new FormDataSaveDTO();
            saveDto.setFormId(formId);
            saveDto.setFieldValues(values);
            R<Long> r = formmodeClient.saveBusinessData(saveDto);
            if (r != null && r.isSuccess() && r.getData() != null) {
                log.info("[blade-workflow] 已创建业务数据行. formId={}, dataId={}", formId, r.getData());
                return r.getData();
            }
            log.warn("[blade-workflow] 创建业务数据失败，回退占位 dataId. formId={}, msg={}",
                formId, r == null ? "null" : r.getMsg());
        } catch (Exception e) {
            log.warn("[blade-workflow] 创建业务数据异常，回退占位 dataId. formId={}", formId, e);
        }
        return null;
    }

    /**
     * 把流程实例ID回填到业务数据行的 {@code request_id}（列必须 BIGINT，装雪花ID）。
     *
     * <p>顺序上必须在实例 insert 之后（此刻才有 instId）；用带 dataId 的保存请求 → 服务端走
     * UPDATE，只更新 request_id 与修改审计列。失败仅告警，不影响已发起的流程。</p>
     */
    private void bindRequestId(WfProcessDefinition def, StartProcessDTO dto, Long dataId, Long instId) {
        Long formId = resolveFormId(def, dto);
        if (formId == null || dataId == null || instId == null) {
            return;
        }
        try {
            FormDataSaveDTO saveDto = new FormDataSaveDTO();
            saveDto.setFormId(formId);
            saveDto.setDataId(dataId);
            saveDto.setRequestId(instId);
            R<Long> r = formmodeClient.saveBusinessData(saveDto);
            if (r == null || !r.isSuccess()) {
                log.warn("[blade-workflow] 回填 request_id 失败. formId={}, dataId={}, msg={}",
                    formId, dataId, r == null ? "null" : r.getMsg());
            } else {
                log.info("[blade-workflow] 已回填 request_id={} → 业务数据 dataId={}", instId, dataId);
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 回填 request_id 异常. formId={}, dataId={}", formId, dataId, e);
        }
    }

    /** 表单ID：优先调用方传入，缺失时取流程定义绑定的表单 */
    private static Long resolveFormId(WfProcessDefinition def, StartProcessDTO dto) {
        if (dto.getFormId() != null) {
            return dto.getFormId();
        }
        return def == null ? null : def.getFormId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(StartProcessDTO dto) {
        if (dto == null) {
            throw new ServiceException("发起流程参数不能为空");
        }
        WfProcessDefinition def = resolveDefinition(dto);
        // 业务数据ID 语义 = 业务表 formtable_main_N 的行 id（对齐 ecology：流程与单据可互相反查）。
        // 由「单据」发起的调用方会显式传入；「表单直发」（发起流程页）则现场建一条业务数据行。
        // 注意 wf_instance.data_id 为 NOT NULL 且无默认值、uk_biz_key(formId:dataId) 唯一，
        // 故必须有真实行或唯一占位，否则 insert 直接失败/第二次发起撞唯一键。
        Long formId = resolveFormId(def, dto);
        Long dataId = dto.getDataId();
        // 本次是否由本方法现场建的业务行：决定要不要回填 request_id
        // （单据发起的关联关系由单据侧维护，这里不越权改动）
        boolean ownBusinessRow = false;
        if (dataId == null) {
            Long created = createBusinessData(def, dto);
            if (created != null) {
                dataId = created;
                ownBusinessRow = true;
            } else {
                // 兜底唯一占位（业务行创建失败时仍要满足 data_id NOT NULL 与 uk_biz_key 唯一）
                dataId = IdWorker.getId();
            }
        }
        String bizKey = buildBizKey(formId, dataId);

        Map<String, Object> vars = new HashMap<>(16);
        if (dto.getFieldValues() != null) {
            vars.putAll(dto.getFieldValues());
        }
        if (dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }

        Long starter = (dto.getStarter() != null) ? dto.getStarter() : SecureUtil.getUserId();
        String engineInstId = processService.startInstance(def.getProcKey(), bizKey, vars);

        // 首节点先算出来：实例标题模板取自首节点的「标题显示设置」
        String firstNodeKey = resolveFirstNodeKey(def.getId());

        WfInstance inst = new WfInstance();
        inst.setEngineInstId(engineInstId);
        inst.setDefId(def.getId());
        inst.setFormId(formId);
        inst.setDataId(dataId);
        inst.setTitle(resolveTitle(dto.getTitle(), def.getName(), def.getId(), firstNodeKey, starter));
        inst.setBizKey(bizKey);
        inst.setStarter(starter);
        inst.setParentId(dto.getParentId());
        inst.setStartTime(new Date());
        inst.setUrgency(dto.getUrgency() == null ? 0 : dto.getUrgency());
        // 测试态标记：测试产生的实例打 is_test=1，并记下临时部署ID，便于一键清理且不污染正常数据
        boolean test = Boolean.TRUE.equals(dto.getTestFlag());
        inst.setIsTest(test ? 1 : 0);
        inst.setTestDeploymentId(dto.getTestDeploymentId());
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.insert(inst);

        // 表单数据快照（决策 3：数据与布局解耦）
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(inst.getId());
        snap.setNodeKey(firstNodeKey);
        snap.setDataJson(JsonUtil.toJson(dto.getFieldValues() == null ? Map.of() : dto.getFieldValues()));
        snapshotMapper.insert(snap);

        // 申请人签字意见（发起页填写）写进第一条「提交」流转记录；为空则记空串
        String startOpinion = (dto.getOpinion() == null) ? "" : dto.getOpinion();
        appendLog(inst.getId(), null, firstNodeKey, starter, WfApprovalLog.LOG_SUBMIT, startOpinion);

        // 业务数据回填流程实例ID（request_id，BIGINT）：仅本次现场建的业务行才写
        if (ownBusinessRow) {
            bindRequestId(def, dto, dataId, inst.getId());
        }

        advance(inst.getId());
        log.info("[blade-workflow] 发起流程成功. instId={}, defId={}, bizKey={}", inst.getId(), def.getId(), bizKey);
        return inst.getId();
    }

    @Override
    public InstanceVO detail(Long id) {
        WfInstance inst = instanceMapper.selectById(id);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        return toInstanceVO(inst);
    }

    @Override
    public InstanceVO getByBiz(Long formId, Long dataId) {
        WfInstance inst = instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getBizKey, buildBizKey(formId, dataId))
            .last("LIMIT 1"));
        return inst == null ? null : toInstanceVO(inst);
    }

    @Override
    public int countByForm(Long formId) {
        if (formId == null) {
            return 0;
        }
        Long count = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getFormId, formId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<ApprovalLogVO> logs(Long instId) {
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getOperateTime));

        // 节点信息 → 运行时消费：按「当前节点」的「表单日志查看范围」过滤可见节点的日志。
        // null = 不限制（保持既有行为）。
        WfInstance inst = instanceMapper.selectById(instId);
        WfProcessNode curNode = (inst == null || inst.getCurrentNodeKey() == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, inst.getDefId())
            .eq(WfProcessNode::getNodeKey, inst.getCurrentNodeKey())
            .last("LIMIT 1"));
        List<String> visibleNodeKeys = WfNodeSettingsUtil.formLogVisibleNodeKeys(curNode);

        // 预取「出口 from→to」一次，用于算每条日志的「下一节点办理人」（接收人）。
        // 聚合所有出口；网关多出口时合并各目标节点的办理人（近似，避免逐条走运行时网关分支）。
        Map<String, List<String>> fromTo = new LinkedHashMap<>();
        if (inst != null) {
            for (WfNodeLink lk : linkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
                .eq(WfNodeLink::getDefId, inst.getDefId()))) {
                if (lk.getFromNodeKey() == null) {
                    continue;
                }
                fromTo.computeIfAbsent(lk.getFromNodeKey(), k -> new ArrayList<>()).add(lk.getToNodeKey());
            }
        }
        Long starter = (inst != null) ? inst.getStarter() : null;

        // 开始节点（nodeType=0）的「提交」日志：仅在流程仍停在开始节点（申请人尚未提交给下一节点）时
        // 不展示；一旦提交到下一节点，就开始展示开始节点的流转意见（作为第一条，含申请人意见）。
        String startNodeKey = null;
        if (inst != null) {
            WfProcessNode startNode = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 0)
                .last("LIMIT 1"));
            if (startNode != null) {
                startNodeKey = startNode.getNodeKey();
            }
        }
        String curNodeKey = (inst != null) ? inst.getCurrentNodeKey() : null;
        boolean atStartNode = startNodeKey != null && startNodeKey.equals(curNodeKey);

        List<ApprovalLogVO> result = new ArrayList<>(logs.size());
        for (WfApprovalLog l : logs) {
            // 开始节点（发起/填表）在仍停在自己节点时不计入流转意见；流转到下一节点后再展示
            if (atStartNode && startNodeKey != null && startNodeKey.equals(l.getNodeKey())) {
                continue;
            }
            if (visibleNodeKeys != null && !visibleNodeKeys.contains(l.getNodeKey())) {
                continue;
            }
            ApprovalLogVO vo = new ApprovalLogVO();
            vo.setId(l.getId());
            vo.setNodeKey(l.getNodeKey());
            vo.setNodeName(resolveNodeName(inst == null ? null : inst.getDefId(), l.getNodeKey()));
            vo.setOperator(l.getOperator());
            vo.setLogType(l.getLogType());
            vo.setOpinion(l.getOpinion());
            vo.setOperateTime(l.getOperateTime());
            // 「接收人」＝本节点出口指向的下一节点的操作者（聚合；网关多出口合并）。
            // 用已有的 WfOperatorResolver 把部门/角色/人员/创建人等配置解析成具体的办理人用户ID，
            // 交由前端用人员字典显示姓名（后端只给 ID，避免额外批量查姓名）。
            Set<Long> handlerIds = new LinkedHashSet<>();
            if (inst != null) {
                List<String> tos = fromTo.get(l.getNodeKey());
                if (tos != null) {
                    for (String to : tos) {
                        try {
                            List<Long> ids = operatorResolver.resolve(
                                inst.getDefId(), to, instId, starter, l.getOperator());
                            if (ids != null) {
                                handlerIds.addAll(ids);
                            }
                        } catch (Exception e) {
                            log.warn("[blade-workflow] 解析下一节点办理人失败，已跳过. nodeKey={}", to, e);
                        }
                    }
                }
            }
            vo.setNextHandlerIds(handlerIds.isEmpty() ? "" :
                handlerIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            result.add(vo);
        }
        return result;
    }

    @Override
    public String snapshot(Long instId, String nodeKey) {
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .eq(WfFormSnapshot::getNodeKey, nodeKey)
            .orderByDesc(WfFormSnapshot::getCreateTime)
            .last("LIMIT 1"));
        return snap == null ? null : snap.getDataJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long instId, String opinion) {
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stop(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(WfInstance.STATUS_SUSPENDED);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "暂停流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "恢复流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long instId, String opinion) {
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        advance(instId, inst.getStarter());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator) {
        advance(instId, currentOperator, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        List<TaskVO> engineTasks = processService.currentTasks(inst.getEngineInstId());

        // 引擎无活动任务 → 流程结束
        if (engineTasks.isEmpty()) {
            String lastNodeKey = inst.getCurrentNodeKey();
            inst.setStatus(WfInstance.STATUS_APPROVED);
            inst.setEndTime(new Date());
            inst.setCurrentNodeKey("");
            instanceMapper.updateById(inst);
            // 归档补一条流转意见：否则「流转意见」里只有各办理节点，看不到最终归档这一步。
            appendArchiveLog(inst, lastNodeKey);
            // 节点信息 → 运行时消费：归档后子流程触发（settings.subflow.trigger=afterArchive）
            // 测试态：跳过附加操作/子流程副作用，避免污染真实业务数据（对齐 ecology istest）
            if (inst.getIsTest() == null || inst.getIsTest() != 1) {
                if (lastNodeKey != null && !lastNodeKey.isEmpty()) {
                    nodeActionExecutor.triggerSubflow(inst, loadNode(inst.getDefId(), lastNodeKey),
                        NodeActionExecutor.TRIGGER_AFTER_ARCHIVE, inst.getStarter());
                }
            }
            return;
        }

        String nodeKey = engineTasks.get(0).getTaskDefinitionKey();
        boolean nodeChanged = !nodeKey.equals(inst.getCurrentNodeKey());
        if (nodeChanged) {
            inst.setCurrentNodeKey(nodeKey);
        }
        instanceMapper.updateById(inst);
        if (nodeChanged && (inst.getIsTest() == null || inst.getIsTest() != 1)) {
            // 新节点激活 → 执行「节点前附加操作」（受「流程异常处理」策略保护）
            // 测试态：跳过附加操作副作用
            nodeActionExecutor.execute(inst, loadNode(inst.getDefId(), nodeKey),
                NodeActionExecutor.PHASE_PRE, inst.getStarter());
        }

        for (TaskVO t : engineTasks) {
            String tk = t.getTaskDefinitionKey();
            // ① 优先按「节点操作者」展开：一个节点可生成多条待办（或签/会签/依次由 WfTaskServiceImpl 推进门禁控制）
            // ⓪ 「指定流转」：用户手工指定的下一节点，其操作者以用户选择为准（模式1）
            if (overrideNodeKey != null && overrideNodeKey.equals(tk) && overrideAssignee != null) {
                if (!existsTask(instId, t.getTaskId(), overrideAssignee)) {
                    insertTask(inst, t, overrideAssignee, tk);
                }
                continue;
            }
            // ① 优先按「节点操作者」展开：一个节点可生成多条待办（或签/会签/依次由 WfTaskServiceImpl 推进门禁控制）
            List<Long> assignees = operatorResolver.resolve(inst.getDefId(), tk, instId, inst.getStarter(), currentOperator);
            if (assignees.isEmpty()) {
                // ② 未配置操作者或类型无法解析 → 回退原有行为（沿用引擎 assignee 单条待办）
                if (existsTask(instId, t.getTaskId(), null)) {
                    continue;
                }
                insertTask(inst, t, parseAssignee(t.getAssignee()), tk);
                continue;
            }
            for (Long uid : assignees) {
                // 幂等键改为 (engineTaskId, assignee)，否则同节点第二人会被 engineTaskId 去重掉
                if (existsTask(instId, t.getTaskId(), uid)) {
                    continue;
                }
                insertTask(inst, t, uid, tk);
            }
        }
    }

    /** 待办是否已存在（assignee 为 null 时只按引擎任务判重） */
    private boolean existsTask(Long instId, String engineTaskId, Long assignee) {
        LambdaQueryWrapper<WfTask> q = Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getEngineTaskId, engineTaskId);
        if (assignee != null) {
            q.eq(WfTask::getAssignee, assignee);
        }
        Long count = taskMapper.selectCount(q);
        return count != null && count > 0;
    }

    /** 生成一条待办：顺带按节点「超时设置」写入 dueTime（供超时任务扫描） */
    private void insertTask(WfInstance inst, TaskVO t, Long assignee, String nodeKey) {
        WfTask task = new WfTask();
        task.setInstId(inst.getId());
        task.setEngineTaskId(t.getTaskId());
        task.setNodeKey(nodeKey);
        task.setAssignee(assignee);
        task.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        task.setStatus(WfTask.STATUS_TODO);
        task.setReceiveTime(new Date());
        Date due = resolveDueTime(inst.getDefId(), nodeKey, task.getReceiveTime());
        if (due != null) {
            task.setDueTime(due);
        }
        taskMapper.insert(task);
    }

    /**
     * 实例标题：调用方指定则优先；否则套用首节点「标题显示设置」的模板；模板缺失则回退流程名。
     *
     * <p>支持占位符：{@code {流程名称}} / {@code {创建人}} / {@code {发起人}} / {@code {日期}}。
     * ⚠️ 本模块无用户中心 Feign，{@code {创建人}} 只能落到用户ID（待接入用户中心后可换成姓名）。</p>
     */
    private String resolveTitle(String given, String defName, Long defId, String firstNodeKey, Long starter) {
        if (given != null && !given.isEmpty()) {
            return given;
        }
        WfProcessNode node = (defId == null || firstNodeKey == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, firstNodeKey)
                .last("LIMIT 1"));
        String tpl = WfNodeSettingsUtil.titleTemplate(node);
        if (tpl == null || tpl.isBlank()) {
            return defName;
        }
        String starterText = starter == null ? "" : String.valueOf(starter);
        String title = tpl
            .replace("{流程名称}", defName == null ? "" : defName)
            .replace("{创建人}", starterText)
            .replace("{发起人}", starterText)
            .replace("{日期}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        return title.isBlank() ? defName : title;
    }

    /** 按节点 settings.timeout.hours 计算截止时间；未配置返回 null */
    private Date resolveDueTime(Long defId, String nodeKey, Date from) {
        int hours = WfNodeSettingsUtil.timeoutHours(loadNode(defId, nodeKey));
        if (hours <= 0) {
            return null;
        }
        return new Date(from.getTime() + hours * 60L * 60L * 1000L);
    }

    /** 按 defId + nodeKey 取节点（取不到返回 null） */
    private WfProcessNode loadNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 私有方法

    private boolean terminate(Long instId, int status, String opinion, String action) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        inst.setStatus(status);
        inst.setEndTime(new Date());
        instanceMapper.updateById(inst);

        // 关闭所有未完成任务
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        for (WfTask t : tasks) {
            t.setStatus(WfTask.STATUS_FINISHED);
            t.setOperateTime(new Date());
            taskMapper.updateById(t);
        }
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, action + "：" + (opinion == null ? "" : opinion));
        return true;
    }

    private WfProcessDefinition resolveDefinition(StartProcessDTO dto) {
        if (dto.getDefId() != null) {
            WfProcessDefinition def = defMapper.selectById(dto.getDefId());
            if (def != null) {
                return def;
            }
        }
        if (dto.getProcKey() == null || dto.getProcKey().isEmpty()) {
            throw new ServiceException("流程定义Key(procKey)与定义ID(defId)不能同时为空");
        }
        WfProcessDefinition def = defMapper.selectOne(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, dto.getProcKey())
            .eq(WfProcessDefinition::getStatus, 1)
            .orderByDesc(WfProcessDefinition::getVersion)
            .last("LIMIT 1"));
        if (def == null) {
            throw new ServiceException("未找到已发布的流程定义: " + dto.getProcKey());
        }
        return def;
    }

    private String resolveFirstNodeKey(Long defId) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .orderByAsc(WfProcessNode::getSortOrder)
            .last("LIMIT 1"));
        return node == null ? "" : node.getNodeKey();
    }

    /**
     * 节点名解析（必须带 defId）。
     *
     * <p>⚠️ 不能只按 nodeKey 查：bpmn-js 生成的 id（Activity_xxx / Gateway_xxx / Event_xxx）
     * 在不同流程间会重复，同一个 nodeKey 会命中别的流程的节点，导致流转意见/实例头显示
     * 串味节点名（例：A 流程的「业务领导」显示成 B 流程里同名 key 的节点名）。</p>
     */
    private String resolveNodeName(Long defId, String nodeKey) {
        if (nodeKey == null || nodeKey.isEmpty()) {
            return "";
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(defId != null, WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        return node == null ? nodeKey : node.getNodeName();
    }

    private InstanceVO toInstanceVO(WfInstance inst) {
        InstanceVO vo = new InstanceVO();
        vo.setId(inst.getId());
        vo.setDefId(inst.getDefId());
        vo.setFormId(inst.getFormId());
        vo.setDataId(inst.getDataId());
        vo.setTitle(inst.getTitle());
        vo.setBizKey(inst.getBizKey());
        vo.setStatus(inst.getStatus());
        vo.setCurrentNodeKey(inst.getCurrentNodeKey());
        vo.setCurrentNodeName(resolveNodeName(inst.getDefId(), inst.getCurrentNodeKey()));
        vo.setStarter(inst.getStarter());
        vo.setStartTime(inst.getStartTime());
        vo.setEndTime(inst.getEndTime());
        vo.setUrgency(inst.getUrgency());
        WfProcessDefinition def = defMapper.selectById(inst.getDefId());
        if (def != null) {
            vo.setDefName(def.getName());
        }
        return vo;
    }

    /**
     * 流程归档时补一条流转意见。
     *
     * <p>nodeKey 取归档节点（nodeType=3）；未配置归档节点时退回「最后经过的节点」。
     * 操作人记 0（系统）、意见固定「流程归档」，与前端「流转意见」的节点名/动作标签展示对应。</p>
     */
    /**
     * 节点操作者情况（流程图节点悬浮「操作者」面板 + 节点下方「谁办了」）。
     *
     * <p>口径见 {@link WfNodeOperatorVO}：已操作 / 已查看（待办打开过）/ 未操作。</p>
     */
    @Override
    public Map<String, WfNodeOperatorVO> nodeOperators(Long instId) {
        Map<String, WfNodeOperatorVO> result = new LinkedHashMap<>();
        if (instId == null) {
            return result;
        }
        WfInstance inst = instanceMapper.selectById(instId);

        // ① 待办任务：按状态归组
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .orderByAsc(WfTask::getId));
        for (WfTask t : tasks) {
            if (t.getNodeKey() == null || t.getNodeKey().isEmpty() || t.getAssignee() == null) {
                continue;
            }
            WfNodeOperatorVO vo = result.computeIfAbsent(t.getNodeKey(), k -> new WfNodeOperatorVO());
            int st = (t.getStatus() == null) ? WfTask.STATUS_TODO : t.getStatus();
            if (st == WfTask.STATUS_TODO) {
                // 待办：打开过待办 → 已查看；从未打开 → 未操作
                if (t.getViewTime() != null) {
                    addOperatorId(vo.getViewed(), t.getAssignee());
                } else {
                    addOperatorId(vo.getTodo(), t.getAssignee());
                }
            } else if (st == WfTask.STATUS_DONE || st == WfTask.STATUS_FINISHED
                || st == WfTask.STATUS_AUTO_SUBMIT || st == WfTask.STATUS_COADJUTANT) {
                addOperatorId(vo.getHandled(), t.getAssignee());
            }
            // 抄送(8) / 传阅(11) 属知会性质，不计入「操作者」面板
        }

        // ② 审批日志：真实办理人（加签/转办/退回等不一定留下本人 task 行，靠日志补齐）
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getId));
        for (WfApprovalLog l : logs) {
            Long op = l.getOperator();
            // 0 = 系统（引擎自动推进/归档），不是人，不进分组
            if (op == null || op == 0L || l.getNodeKey() == null || l.getNodeKey().isEmpty()) {
                continue;
            }
            WfNodeOperatorVO vo = result.computeIfAbsent(l.getNodeKey(), k -> new WfNodeOperatorVO());
            addOperatorId(vo.getHandled(), op);
            // 已操作的人从「已查看 / 未操作」里摘掉（或签时其余人待办会被强制办结）
            vo.getTodo().remove(op);
            vo.getViewed().remove(op);
        }

        // ③ 开始节点：引擎在发起时自动完成、不生成待办，其办理人即发起人（申请人）
        if (inst != null && inst.getStarter() != null) {
            WfProcessNode startNode = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(inst.getDefId() != null, WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 0)
                .last("LIMIT 1"));
            if (startNode != null && startNode.getNodeKey() != null && !startNode.getNodeKey().isEmpty()) {
                WfNodeOperatorVO vo = result.computeIfAbsent(startNode.getNodeKey(), k -> new WfNodeOperatorVO());
                addOperatorId(vo.getHandled(), inst.getStarter());
                vo.getTodo().remove(inst.getStarter());
                vo.getViewed().remove(inst.getStarter());
            }
        }
        return result;
    }

    /** 去重追加（保持首次出现顺序） */
    private static void addOperatorId(List<Long> ids, Long id) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private void appendArchiveLog(WfInstance inst, String lastNodeKey) {
        String nodeKey = (lastNodeKey == null) ? "" : lastNodeKey;
        WfProcessNode archive = null;
        try {
            archive = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 3)
                .last("LIMIT 1"));
            if (archive != null && archive.getNodeKey() != null && !archive.getNodeKey().isEmpty()) {
                nodeKey = archive.getNodeKey();
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 查询归档节点失败，流转意见退回最后经过节点. instId={}", inst.getId(), e);
        }
        // 归档人：按归档节点（nodeType=3）配置的「操作者」解析（典型配置为「创建人本人」→ 发起人）。
        // ⚠️ 不能写死 0（系统）：归档节点上明明配置了办理人（上一节点的「接收人」就是解析它得到的），
        //    若这里固定记系统，流转意见的归档那行会显示「系统」，与节点配置的归档人不一致。
        //    解析不到（未配操作者 / 类型不可解析）才回退 0。
        Long operator = 0L;
        if (archive != null) {
            try {
                List<Long> ids = operatorResolver.resolve(
                    inst.getDefId(), nodeKey, inst.getId(), inst.getStarter(), inst.getStarter());
                if (ids != null && !ids.isEmpty()) {
                    operator = ids.get(0);
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 解析归档节点办理人失败，回退系统. instId={}", inst.getId(), e);
            }
        }
        appendLog(inst.getId(), null, nodeKey, operator, WfApprovalLog.LOG_APPROVE, "流程归档");
    }

    private void appendLog(Long instId, Long taskId, String nodeKey, Long operator,
                           String logType, String opinion) {        WfApprovalLog log = new WfApprovalLog();
        log.setInstId(instId);
        log.setTaskId(taskId);
        log.setNodeKey(nodeKey == null ? "" : nodeKey);
        log.setOperator(operator);
        log.setLogType(logType);
        log.setOpinion(opinion == null ? "" : opinion);
        log.setOperateTime(new Date());
        logMapper.insert(log);
    }

    private static Long parseAssignee(String assignee) {
        if (assignee == null || assignee.isEmpty()) {
            return 0L;
        }
        try {
            return Long.valueOf(assignee);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String buildBizKey(Long formId, Long dataId) {
        return formId + ":" + dataId;
    }

}
