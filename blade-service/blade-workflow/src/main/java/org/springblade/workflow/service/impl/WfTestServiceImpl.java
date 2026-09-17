package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.history.HistoricActivityInstance;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.mapper.WfTestLogMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.service.IWfTestService;
import org.springblade.workflow.vo.WfTestResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程测试服务实现（设计期校验）
 *
 * <p><b>实现方式：真实引擎 + 真实表单 + 测试态标记</b>（对齐 ecology
 * {@code workflow_requestbase.deleted=1}）。与早期「配置走查」的区别：
 * <ul>
 *   <li>草稿流程通过 {@link IWfDefinitionService#deployForTest} 临时部署到 Flowable（不改发布状态）；</li>
 *   <li>用 {@link IWfInstanceService#start} <b>真实发起</b>，实例/待办打 {@code is_test=1}；</li>
 *   <li>表单字段值作为流程变量下发引擎，驱动排他网关按真实条件选分支；</li>
 *   <li>逐节点以「系统自动通过」推进到归档，基于历史活动统计覆盖率；</li>
 *   <li>测试态下 {@code WfInstanceServiceImpl}/{@code WfTaskServiceImpl} 跳过节点附加操作与子流程副作用；</li>
 *   <li>测试数据可经 {@link #cleanupTestData} 一键清理（删实例/待办/日志/快照 + 级联卸载测试部署）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfTestServiceImpl implements IWfTestService {

    private static final int MAX_STEPS = 1000;
    private static final int LIST_LIMIT = 100;
    private static final int MAX_OPERATORS = 5;

    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfTestLogMapper testLogMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfApprovalLogMapper approvalLogMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    private final IUserClient userClient;
    private final IWfDefinitionService definitionService;
    private final IWfInstanceService instanceService;
    private final IWfTaskService taskService;
    private final IProcessService processService;
    private final WfNodeLinkMapper linkMapper;
    private final WfNodeOperatorMapper operatorMapper;

    @Override
    public WfTestResultVO run(WfTestRunDTO dto) {
        if (dto == null || dto.getDefId() == null) {
            throw new ServiceException("流程定义ID不能为空");
        }
        if (dto.getTestUserId() == null) {
            throw new ServiceException("请选择测试发起人");
        }
        Long defId = dto.getDefId();
        Long testUserId = dto.getTestUserId();

        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        if (def.getStatus() != null && def.getStatus() == 2) {
            throw new ServiceException("已停用的流程不支持测试");
        }
        if (def.getFormId() == null) {
            throw new ServiceException("该流程未关联表单，无法渲染测试表单（请先给流程绑定表单）");
        }

        long begin = System.currentTimeMillis();
        List<String> logLines = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 节点清单（贯穿预校验/正常/异常路径，保证结果列表一致）
        List<WfProcessNode> nodeList = nodeMapper.selectList(
            Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
        List<WfNodeLink> links = linkMapper.selectList(
            Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, defId));
        List<Long> nodeIds = nodeList.stream().map(WfProcessNode::getId).collect(Collectors.toList());
        List<WfNodeOperator> operators = nodeIds.isEmpty() ? Collections.emptyList()
                : operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
                    .in(WfNodeOperator::getNodeId, nodeIds));

        // 预校验：节点配置是否完整/合法（在部署与真实发起前拦截，避免运行期炸出原始引擎异常）
        Map<String, String> issues = validateBeforeRun(links, nodeList, operators);
        WfTestResultVO result;
        if (!issues.isEmpty()) {
            logLines.add(fmt.format(new Date()) + " 预校验发现 " + issues.size()
                + " 个节点配置问题，已终止测试：");
            for (Map.Entry<String, String> en : issues.entrySet()) {
                logLines.add("    - 节点【" + nodeName(nodeList, en.getKey()) + "】" + en.getValue());
            }
            result = buildResult(nodeList, issues, null,
                Collections.emptyMap(), Collections.emptySet(), logLines, begin, false, true);
            saveTestLog(def, dto, result, logLines);
            return result;
        }

        try {
            String deploymentId = definitionService.deployForTest(defId);
            logLines.add(fmt.format(new Date()) + " 已将草稿流程临时部署到引擎（deploymentId=" + deploymentId + "）");

            StartProcessDTO startDto = new StartProcessDTO();
            startDto.setDefId(defId);
            startDto.setFormId(def.getFormId());
            startDto.setStarter(testUserId);
            startDto.setFieldValues(dto.getFormData());
            startDto.setTestFlag(true);
            startDto.setTestDeploymentId(deploymentId);
            startDto.setTitle("【测试】" + (def.getName() == null ? "" : def.getName()));
            // 测试态无真实业务数据行：构造唯一 dataId，否则 data_id NOT NULL 校验失败，
            // 且 uk_biz_key(formId:dataId) 会在多次测试同一流程时重复。
            if (startDto.getDataId() == null) {
                startDto.setDataId(IdWorker.getId());
            }
            Long instId = instanceService.start(startDto);
            WfInstance inst = instanceMapper.selectById(instId);
            logLines.add(fmt.format(new Date()) + " 已真实发起测试实例 instId=" + instId
                + "（engineInstId=" + (inst == null ? "" : inst.getEngineInstId()) + "）");

            int steps = 0;
            while (inst != null && WfInstance.STATUS_RUNNING == inst.getStatus() && steps < MAX_STEPS) {
                steps++;
                List<WfTask> todos = pendingTestTasks(instId);
                if (todos.isEmpty()) {
                    instanceService.advance(instId);
                    inst = instanceMapper.selectById(instId);
                    if (pendingTestTasks(instId).isEmpty()) {
                        if (inst != null && WfInstance.STATUS_RUNNING == inst.getStatus()) {
                            logLines.add(fmt.format(new Date())
                                + " 引擎无待办但实例仍运行中，可能存在未同步节点，已中止驱动");
                        }
                        break;
                    }
                    continue;
                }
                for (WfTask t : todos) {
                    try {
                        taskService.autoApprove(t.getId(), "测试自动通过");
                        logLines.add(fmt.format(new Date()) + " 节点【" + t.getNodeKey() + "】已自动通过（办理人="
                            + t.getAssignee() + "）");
                    } catch (Exception e) {
                        // 节点配置/引擎执行异常：记入问题节点，优雅终止（不再向外抛原始异常）
                        String msg = "节点配置或引擎执行异常，已中断测试：" + e.getMessage();
                        logLines.add(fmt.format(new Date()) + " " + msg);
                        issues = new LinkedHashMap<>();
                        issues.put(t.getNodeKey(), msg);
                        WfTestResultVO failed = buildResult(nodeList, issues, instId,
                            Collections.emptyMap(), Collections.emptySet(), logLines, begin, false, true);
                        saveTestLog(def, dto, failed, logLines);
                        return failed;
                    }
                }
                inst = instanceMapper.selectById(instId);
            }

            boolean reachedEnd = inst != null && WfInstance.STATUS_APPROVED == inst.getStatus();
            boolean aborted = steps >= MAX_STEPS
                || (inst != null && WfInstance.STATUS_RUNNING == inst.getStatus());

            Map<String, Integer> nodeTimes = new LinkedHashMap<>();
            Set<String> visited = new LinkedHashSet<>();
            List<WfTestResultVO.TestStepVO> path = new ArrayList<>();
            try {
                String engineInstId = inst == null ? null : inst.getEngineInstId();
                if (engineInstId != null) {
                    List<HistoricActivityInstance> acts = processService.historicActivities(engineInstId);
                    String prev = null;
                    for (HistoricActivityInstance a : acts) {
                        String aid = a.getActivityId();
                        if (aid == null) {
                            continue;
                        }
                        nodeTimes.merge(aid, 1, Integer::sum);
                        visited.add(aid);
                        if (prev != null && !prev.equals(aid)) {
                            WfTestResultVO.TestStepVO step = new WfTestResultVO.TestStepVO();
                            step.setFromNodeKey(prev);
                            step.setToNodeKey(aid);
                            path.add(step);
                        }
                        prev = aid;
                    }
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 流程测试读取历史活动失败. instId={}, err={}", instId, e.getMessage());
            }

            result = buildResult(nodeList, issues, instId, nodeTimes, visited, logLines, begin, reachedEnd, aborted);
            result.setPath(path);
        } catch (Exception e) {
            // 部署/发起阶段异常（如 BPMN 解析、条件表达式非法等）：优雅终止，定位首个业务节点
            String msg = "测试在部署/发起阶段中断（流程配置存在问题）：" + e.getMessage();
            logLines.add(fmt.format(new Date()) + " " + msg);
            issues = new LinkedHashMap<>();
            String firstNode = nodeList.isEmpty() ? null : nodeList.get(0).getNodeKey();
            if (firstNode != null) {
                issues.put(firstNode, msg);
            }
            result = buildResult(nodeList, issues, null,
                Collections.emptyMap(), Collections.emptySet(), logLines, begin, false, true);
        }
        saveTestLog(def, dto, result, logLines);
        return result;
    }

    /**
     * 测试前静态校验：在部署/真实发起前拦截明显非法或未设置的节点，避免运行期炸出原始引擎异常。
     * 返回 nodeKey -> 问题描述（空表示通过）。
     */
    private Map<String, String> validateBeforeRun(List<WfNodeLink> links,
            List<WfProcessNode> nodeList, List<WfNodeOperator> operators) {
        Map<String, String> issues = new LinkedHashMap<>();
        // 1. 出口条件表达式含被 HTML 转义的运算符（XSS 过滤器把 > < & 转义成实体），引擎无法解析
        if (links != null) {
            for (WfNodeLink l : links) {
                String expr = l.getConditionExpr();
                if (expr != null && !expr.isBlank()
                        && (expr.contains("&gt;") || expr.contains("&lt;") || expr.contains("&amp;")
                            || expr.contains("&GT;") || expr.contains("&LT;") || expr.contains("&AMP;"))) {
                    issues.put(l.getToNodeKey(),
                        "出口条件表达式非法：含被转义的运算符（&gt; &lt; &amp;），请在「流转设置」重新保存条件后再测试");
                }
            }
        }
        // 2. 审批(1)/提交(2)节点未设置操作者
        Set<Long> opNodeIds = operators == null ? Collections.emptySet()
                : operators.stream().map(WfNodeOperator::getNodeId).collect(Collectors.toSet());
        for (WfProcessNode n : nodeList) {
            Integer t = n.getNodeType();
            if (t != null && (t == 1 || t == 2) && !opNodeIds.contains(n.getId())) {
                issues.put(n.getNodeKey(),
                    "未设置操作者，请在「节点信息-操作者」中配置办理人后再测试");
            }
        }
        return issues;
    }

    /**
     * 组装测试结果：逐节点判定（问题=2/走通=1/未走到=0），并给出结论与状态。
     */
    private WfTestResultVO buildResult(List<WfProcessNode> nodeList,
            Map<String, String> issues, Long instId, Map<String, Integer> nodeTimes,
            Set<String> visited, List<String> logLines, long begin, boolean reachedEnd, boolean aborted) {
        WfTestResultVO result = new WfTestResultVO();
        int nodeTotal = nodeList.size();
        List<WfTestResultVO.TestNodeVO> nodeVos = new ArrayList<>();
        int passed = 0;
        for (WfProcessNode n : nodeList) {
            WfTestResultVO.TestNodeVO vo = new WfTestResultVO.TestNodeVO();
            vo.setNodeKey(n.getNodeKey());
            vo.setNodeName(n.getNodeName());
            vo.setNodeType(n.getNodeType());
            int times = nodeTimes.getOrDefault(n.getNodeKey(), 0);
            vo.setPassTimes(times);
            String issue = issues.get(n.getNodeKey());
            boolean hit = visited.contains(n.getNodeKey());
            if (issue != null) {
                vo.setStatus(2);
                vo.setMessage(issue);
            } else if (hit) {
                vo.setStatus(1);
                vo.setMessage("走通");
                passed++;
            } else {
                vo.setStatus(0);
                vo.setMessage("未走到该节点（与起点不连通或网关条件未命中）");
            }
            vo.setOperators(toOperatorVos(instId, n.getNodeKey()));
            nodeVos.add(vo);
        }
        boolean allPassed = !aborted && reachedEnd && passed == nodeTotal;
        result.setNodeTotal(nodeTotal);
        result.setNodePassed(passed);
        result.setReachedEnd(reachedEnd);
        result.setCostMs(System.currentTimeMillis() - begin);
        result.setNodeTimes(new LinkedHashMap<>(nodeTimes));
        result.setNodes(nodeVos);
        result.setLog(logLines);
        int issueCount = issues.size();
        if (issueCount > 0) {
            result.setTestStatus(WfTestLog.TEST_FAILED);
            result.setSummary("测试未通过：发现 " + issueCount + " 个节点配置问题（详见节点列表与日志），请修正后重试。");
        } else if (aborted) {
            result.setTestStatus(WfTestLog.TEST_ABORTED);
            result.setSummary(buildSummary(passed, nodeTotal, reachedEnd, true));
        } else if (allPassed) {
            result.setTestStatus(WfTestLog.TEST_PASSED);
            result.setSummary(buildSummary(passed, nodeTotal, reachedEnd, false));
        } else {
            result.setTestStatus(WfTestLog.TEST_FAILED);
            result.setSummary(buildSummary(passed, nodeTotal, reachedEnd, false));
        }
        return result;
    }

    /** 持久化测试日志（与历史逻辑一致） */
    private void saveTestLog(WfProcessDefinition def, WfTestRunDTO dto, WfTestResultVO result, List<String> logLines) {
        String testUserName = resolveName(dto.getTestUserId());
        WfTestLog entity = new WfTestLog();
        entity.setDefId(def.getId());
        entity.setDefVersion(def.getVersion());
        entity.setProcKey(def.getProcKey());
        entity.setDefName(def.getName());
        entity.setTestUserId(dto.getTestUserId());
        entity.setTestUserName(testUserName);
        entity.setTestTime(new Date());
        entity.setCostMs(result.getCostMs());
        entity.setTestStatus(result.getTestStatus());
        entity.setNodeTotal(result.getNodeTotal());
        entity.setNodePassed(result.getNodePassed());
        entity.setReachedEnd(Boolean.TRUE.equals(result.getReachedEnd()) ? 1 : 0);
        entity.setSummary(result.getSummary());
        entity.setLogContent(String.join("\n", logLines));
        entity.setResultJson(JsonUtil.toJson(result));
        entity.setCreateUser(SecureUtil.getUserId());
        testLogMapper.insert(entity);
        result.setLogId(entity.getId());
    }

    /** 用 nodeKey 取节点显示名（无名称时回退为 key） */
    private String nodeName(List<WfProcessNode> nodeList, String nodeKey) {
        if (nodeKey == null) {
            return "-";
        }
        for (WfProcessNode n : nodeList) {
            if (nodeKey.equals(n.getNodeKey())) {
                return (n.getNodeName() == null || n.getNodeName().isBlank()) ? nodeKey : n.getNodeName();
            }
        }
        return nodeKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanupTestData(Long defId) {
        LambdaQueryWrapper<WfInstance> q = Wrappers.<WfInstance>lambdaQuery().eq(WfInstance::getIsTest, 1);
        if (defId != null) {
            q.eq(WfInstance::getDefId, defId);
        }
        List<WfInstance> insts = instanceMapper.selectList(q);
        Set<String> deployments = new LinkedHashSet<>();
        for (WfInstance inst : insts) {
            taskMapper.delete(Wrappers.<WfTask>lambdaQuery().eq(WfTask::getInstId, inst.getId()));
            approvalLogMapper.delete(Wrappers.<WfApprovalLog>lambdaQuery().eq(WfApprovalLog::getInstId, inst.getId()));
            snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery().eq(WfFormSnapshot::getInstId, inst.getId()));
            if (StringUtil.isNotBlank(inst.getTestDeploymentId())) {
                deployments.add(inst.getTestDeploymentId());
            }
        }
        for (String dep : deployments) {
            try {
                processService.deleteDeployment(dep);
            } catch (Exception e) {
                log.warn("[blade-workflow] 测试部署卸载失败（可能已手动删除）. deploymentId={}, err={}", dep, e.getMessage());
            }
        }
        instanceMapper.delete(q);
        log.info("[blade-workflow] 已清理测试数据. defId={}, 实例数={}, 卸载部署数={}", defId, insts.size(), deployments.size());
        return insts.size();
    }

    @Override
    public List<WfTestLog> list(Long defId) {
        return testLogMapper.selectList(Wrappers.<WfTestLog>lambdaQuery()
            .eq(defId != null, WfTestLog::getDefId, defId)
            .orderByDesc(WfTestLog::getTestTime)
            .last("LIMIT " + LIST_LIMIT));
    }

    @Override
    public WfTestLog detail(Long id) {
        WfTestLog entity = testLogMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("测试记录不存在");
        }
        return entity;
    }

    @Override
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return testLogMapper.deleteBatchIds(ids) > 0;
    }

    private List<WfTask> pendingTestTasks(Long instId) {
        return taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getIsTest, 1)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
    }

    private List<WfTestResultVO.TestOperatorVO> toOperatorVos(Long instId, String nodeKey) {
        // instId 为空（预校验/异常终止路径）时直接返回空，避免 MP 把 null 的 eq 条件忽略而误查全表
        if (instId == null) {
            return new ArrayList<>();
        }
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .last("LIMIT " + (MAX_OPERATORS + 1)));
        List<WfTestResultVO.TestOperatorVO> vos = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (WfTask t : tasks) {
            if (t.getAssignee() == null || !seen.add(t.getAssignee())) {
                continue;
            }
            WfTestResultVO.TestOperatorVO vo = new WfTestResultVO.TestOperatorVO();
            vo.setUserId(t.getAssignee());
            vo.setUserName(resolveName(t.getAssignee()));
            vo.setSource("测试态待办办理人");
            vos.add(vo);
            if (vos.size() >= MAX_OPERATORS) {
                break;
            }
        }
        return vos;
    }

    private String resolveName(Long id) {
        if (id == null) {
            return "-";
        }
        try {
            org.springblade.core.tool.api.R<org.springblade.system.user.entity.UserInfo> r = userClient.userInfo(id);
            if (r != null && r.isSuccess() && r.getData() != null && r.getData().getUser() != null) {
                org.springblade.system.user.entity.User u = r.getData().getUser();
                String n = u.getRealName() != null ? u.getRealName() : u.getName();
                if (n != null && !n.isBlank()) {
                    return n;
                }
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 流程测试查询用户名失败，已降级为ID. userId={}, err={}", id, e.getMessage());
        }
        return String.valueOf(id);
    }

    private String buildSummary(int passed, int total, boolean reachedEnd, boolean aborted) {
        if (aborted) {
            return "测试中断：自动驱动步数达到上限或流程仍停留在运行中（可能存在未同步节点/成环），请检查流程配置。";
        }
        if (reachedEnd && passed == total) {
            return "测试通过：流程从创建节点真实走到归档节点，共 " + total + " 个节点全部经过，各节点操作者可正常解析。";
        }
        StringBuilder sb = new StringBuilder("测试未通过：");
        if (!reachedEnd) {
            sb.append("未能走到归档节点；");
        }
        if (passed < total) {
            sb.append((total - passed)).append(" 个节点未走到（连通性或网关条件未命中）；");
        }
        sb.append("详见测试日志。");
        return sb.toString();
    }
}
