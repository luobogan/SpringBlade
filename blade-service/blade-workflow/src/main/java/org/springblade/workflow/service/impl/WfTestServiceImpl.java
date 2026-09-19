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
import org.springblade.workflow.dto.ValidateDTO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfTestLog;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
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
import org.springblade.workflow.service.IWfFormRenderService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.vo.WfTestResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final WfNodeFieldPermMapper fieldPermMapper;
    private final IWfFormRenderService formRenderService;

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
        Map<String, String> issues = validateBeforeRun(defId, links, nodeList, operators);
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

            // 分支覆盖：按各分支出口条件反推变量取值，为每个分支额外真跑一个实例，合并覆盖率。
            // 否则一组表单数据只能命中网关的一条分支，其余分支被判「未走到」（假阴性）。
            List<Scenario> scenarios = buildScenarios(dto, links);
            List<WfTestResultVO.TestScenarioVO> scenarioVos = new ArrayList<>();
            Map<String, Integer> nodeTimesUnion = new LinkedHashMap<>();
            Set<String> visitedUnion = new LinkedHashSet<>();
            Map<String, Integer> linkTimesUnion = new LinkedHashMap<>();
            boolean anyReachedEnd = false;
            boolean anyAborted = false;
            Long sampleInstId = null;
            logLines.add(fmt.format(new Date()) + " 共 " + scenarios.size() + " 个测试场景（开启分支覆盖="
                + Boolean.TRUE.equals(dto.getCoverBranches()) + "）");

            int idx = 0;
            for (Scenario sc : scenarios) {
                idx++;
                SingleRun sr = runSingle(def, deploymentId, testUserId, sc, fmt, logLines, idx);
                sr.nodeTimes.forEach((k, v) -> nodeTimesUnion.merge(k, v, Integer::sum));
                visitedUnion.addAll(sr.visitedNodes);
                sr.linkTimes.forEach((k, v) -> linkTimesUnion.merge(k, v, Integer::sum));
                if (sr.reachedEnd) {
                    anyReachedEnd = true;
                }
                if (sr.aborted) {
                    anyAborted = true;
                }
                if (sampleInstId == null) {
                    sampleInstId = sr.instId;
                }
                scenarioVos.add(toScenarioVo(idx, sc, sr));
                if (sr.fatal != null) {
                    issues = new LinkedHashMap<>();
                    issues.put(sr.fatal.getKey(), sr.fatal.getValue());
                    break;
                }
            }

            result = buildResult(nodeList, issues, sampleInstId, nodeTimesUnion, visitedUnion,
                logLines, begin, anyReachedEnd, anyAborted);
            // 回传测试态实例 ID：供前端用 /form/render 渲染「真实流程表单界面」
            // （对齐 ecology 自动测试页右侧的流程表单）。预校验未过（未发起）时为 null。
            result.setInstId(sampleInstId);
            result.setPath(buildPathFromLinkTimes(links, linkTimesUnion));
            result.setScenarios(scenarioVos);
            result.setScenarioCount(scenarioVos.size());
            fillLinkCoverage(result, links, nodeList, linkTimesUnion);
            saveTestLog(def, dto, result, logLines);
            return result;
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

    /** 单个场景：真实发起一个测试态实例并自动驱动到结束，返回该场景的覆盖数据 */
    private SingleRun runSingle(WfProcessDefinition def, String deploymentId, Long testUserId,
            Scenario sc, SimpleDateFormat fmt, List<String> logLines, int idx) {
        SingleRun sr = new SingleRun();
        Long defId = def.getId();
        try {
            StartProcessDTO startDto = new StartProcessDTO();
            startDto.setDefId(defId);
            startDto.setFormId(def.getFormId());
            startDto.setStarter(testUserId);
            startDto.setFieldValues(sc.formData);
            startDto.setTestFlag(true);
            startDto.setTestDeploymentId(deploymentId);
            startDto.setTitle("【测试】" + (def.getName() == null ? "" : def.getName()));
            // 测试态无真实业务数据行：构造唯一 dataId，否则 data_id NOT NULL 校验失败，
            // 且 uk_biz_key(formId:dataId) 会在多次测试同一流程时重复。
            if (startDto.getDataId() == null) {
                startDto.setDataId(IdWorker.getId());
            }
            Long instId = instanceService.start(startDto);
            sr.instId = instId;
            WfInstance inst = instanceMapper.selectById(instId);
            logLines.add(fmt.format(new Date()) + " [场景" + idx + "] 已真实发起测试实例 instId=" + instId
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
                        // 模拟真实审批提交：节点必填矩阵校验（与 ExcelPreviewPage 提交前 validateForm 同款口径）。
                        // 否则表单必填项未填写也能被系统自动通过推进，测试会「假通过」。
                        ValidateDTO vdto = new ValidateDTO();
                        vdto.setInstanceId(instId);
                        vdto.setDefId(defId);
                        vdto.setNodeKey(t.getNodeKey());
                        vdto.setFormData(sc.formData);
                        try {
                            formRenderService.validate(vdto);
                        } catch (ServiceException ve) {
                            String msg = "节点【" + t.getNodeKey() + "】必填校验未通过（表单必填项未填写），已终止该场景："
                                + ve.getMessage();
                            logLines.add(fmt.format(new Date()) + " " + msg);
                            sr.fatal = new AbstractMap.SimpleEntry<>(t.getNodeKey(), msg);
                            return sr;
                        }
                        taskService.autoApprove(t.getId(), "测试自动通过");
                        logLines.add(fmt.format(new Date()) + " 节点【" + t.getNodeKey() + "】已自动通过（办理人="
                            + t.getAssignee() + "）");
                    } catch (Exception e) {
                        // 节点配置/引擎执行异常：记入问题节点，优雅终止（不再向外抛原始异常）
                        String msg = "节点配置或引擎执行异常，已中断测试：" + e.getMessage();
                        logLines.add(fmt.format(new Date()) + " " + msg);
                        sr.fatal = new AbstractMap.SimpleEntry<>(t.getNodeKey(), msg);
                        return sr;
                    }
                }
                inst = instanceMapper.selectById(instId);
            }

            sr.reachedEnd = inst != null && WfInstance.STATUS_APPROVED == inst.getStatus();
            sr.aborted = steps >= MAX_STEPS
                || (inst != null && WfInstance.STATUS_RUNNING == inst.getStatus());

            try {
                String engineInstId = inst == null ? null : inst.getEngineInstId();
                if (engineInstId != null) {
                    List<HistoricActivityInstance> acts =
                        new ArrayList<>(processService.historicActivities(engineInstId));
                    // 同一毫秒内完成的相邻活动（开始事件 / 顺序流 / 自动通过的任务）开始时间完全相同，
                    // 只按 startTime 查询出来的相对顺序不稳定 → 用自增主键 ID_ 做二级排序，
                    // 否则「相邻节点对」会还原成错误的流转，出口覆盖率恒为 0。
                    if (acts.size() > 1) {
                        Comparator<HistoricActivityInstance> byStart = Comparator.comparing(
                            HistoricActivityInstance::getStartTime,
                            Comparator.nullsLast(Date::compareTo));
                        acts.sort(byStart.thenComparingLong(a -> histOrderKey(a.getId())));
                    }
                    String prev = null;
                    for (HistoricActivityInstance a : acts) {
                        String aid = a.getActivityId();
                        if (aid == null) {
                            continue;
                        }
                        // 引擎把「顺序流」也写进历史活动（ACT_TYPE_=sequenceFlow，如 Flow_1svagre）。
                        // 它不是节点，却会插在相邻节点之间把 prev→cur 打断（开始→Flow_x→业务领导），
                        // 使「相邻节点对」对不上任何一条物理出口 → 出口覆盖率恒为 0。这里直接跳过。
                        if ("sequenceFlow".equals(a.getActivityType())) {
                            continue;
                        }
                        sr.nodeTimes.merge(aid, 1, Integer::sum);
                        sr.visitedNodes.add(aid);
                        sr.pathNodes.add(aid);
                        if (prev != null && !prev.equals(aid)) {
                            // 相邻的两个活动即一次真实流转；网关也是活动，
                            // 故「prev→cur」可直接对应到一条物理出口（wf_node_link）
                            sr.linkTimes.merge(prev + "→" + aid, 1, Integer::sum);
                        }
                        prev = aid;
                    }
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 流程测试读取历史活动失败. instId={}, err={}", instId, e.getMessage());
            }
        } catch (Exception e) {
            String msg = "场景" + idx + "在部署/发起阶段中断（流程配置存在问题）：" + e.getMessage();
            logLines.add(fmt.format(new Date()) + " " + msg);
            sr.fatal = new AbstractMap.SimpleEntry<>(null, msg);
        }
        return sr;
    }

    /**
     * 构造测试场景：主场景（用户填写的表单值）+ 每个带条件的分支出口各一个场景。
     *
     * <p>分支场景的变量取值由出口条件<b>反推</b>（尽力而为），目的是让该分支真的被引擎选中，
     * 从而把「每条线」都真实走到；无法反推的分支会被跳过，最终以「未覆盖」如实报出。</p>
     */
    private List<Scenario> buildScenarios(WfTestRunDTO dto, List<WfNodeLink> links) {
        List<Scenario> list = new ArrayList<>();
        Map<String, Object> base = dto.getFormData() == null
            ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getFormData());
        list.add(new Scenario("主场景（表单填写值）", base));
        if (!Boolean.TRUE.equals(dto.getCoverBranches())) {
            return list;
        }
        Set<String> seen = new LinkedHashSet<>();
        seen.add(JsonUtil.toJson(base));
        for (WfNodeLink l : links) {
            String expr = l.getConditionExpr();
            if (expr == null || expr.isBlank()) {
                continue;
            }
            Map<String, Object> derived = deriveValues(expr);
            if (derived.isEmpty()) {
                continue;
            }
            Map<String, Object> merged = new LinkedHashMap<>(base);
            merged.putAll(derived);
            if (seen.add(JsonUtil.toJson(merged))) {
                list.add(new Scenario("分支覆盖：" + abbreviate(expr), merged));
            }
        }
        return list;
    }

    /** 条件表达式取值样本：从「变量 运算符 字面量」反推一组能满足该条件的变量值 */
    private static final Pattern COND_PATTERN =
        Pattern.compile("([A-Za-z_][\\w]*)\\s*(>=|<=|==|!=|>|<)\\s*('[^']*'|-?\\d+(?:\\.\\d+)?|true|false)");

    private Map<String, Object> deriveValues(String expr) {
        Map<String, Object> vals = new LinkedHashMap<>();
        Matcher m = COND_PATTERN.matcher(expr);
        while (m.find()) {
            Object lit = parseLiteral(m.group(3));
            Object sat = satisfy(m.group(2), lit);
            if (sat != null) {
                vals.put(m.group(1), sat);
            }
        }
        return vals;
    }

    private Object parseLiteral(String lit) {
        if (lit.startsWith("'")) {
            return lit.substring(1, lit.length() - 1);
        }
        if ("true".equalsIgnoreCase(lit) || "false".equalsIgnoreCase(lit)) {
            return Boolean.valueOf(lit);
        }
        try {
            return lit.contains(".") ? (Object) Double.valueOf(lit) : (Object) Long.valueOf(lit);
        } catch (Exception e) {
            return lit;
        }
    }

    /** 按运算符给出一个「满足条件」的取值 */
    private Object satisfy(String op, Object v) {
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            switch (op) {
                case ">": return d + 1;
                case ">=": return v;
                case "<": return d - 1;
                case "<=": return v;
                case "==": return v;
                case "!=": return d + 1;
                default: return v;
            }
        }
        if (v instanceof Boolean) {
            return "!=".equals(op) ? Boolean.valueOf(!((Boolean) v)) : v;
        }
        String s = String.valueOf(v);
        return "!=".equals(op) ? s + "_其他" : s;
    }

    private String abbreviate(String s) {
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= 40 ? t : t.substring(0, 40) + "…";
    }

    /**
     * 历史活动排序键：ACT_HI_ACTINST.ID_ 是自增主键，数值大小即真实发生顺序。
     * 非数字型 ID（换用 UUID 生成器时）统一排到末尾，退化为按开始时间排序。
     */
    private static long histOrderKey(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ignore) {
            return Long.MAX_VALUE;
        }
    }

    private String linkKey(WfNodeLink l) {
        return l.getFromNodeKey() + "→" + l.getToNodeKey();
    }

    /** 由「被引擎真实走过的出口」还原流转路径 */
    private List<WfTestResultVO.TestStepVO> buildPathFromLinkTimes(List<WfNodeLink> links,
            Map<String, Integer> linkTimes) {
        Map<String, WfNodeLink> byKey = new LinkedHashMap<>();
        for (WfNodeLink l : links) {
            byKey.putIfAbsent(linkKey(l), l);
        }
        List<WfTestResultVO.TestStepVO> path = new ArrayList<>();
        for (String k : linkTimes.keySet()) {
            int i = k.indexOf('→');
            if (i <= 0) {
                continue;
            }
            WfTestResultVO.TestStepVO step = new WfTestResultVO.TestStepVO();
            step.setFromNodeKey(k.substring(0, i));
            step.setToNodeKey(k.substring(i + 1));
            WfNodeLink l = byKey.get(k);
            if (l != null) {
                step.setConditionCn(l.getConditionCn());
            }
            path.add(step);
        }
        return path;
    }

    /** 出口级覆盖率：逐条出口标注是否被真实走到（合并所有场景） */
    private void fillLinkCoverage(WfTestResultVO result, List<WfNodeLink> links,
            List<WfProcessNode> nodeList, Map<String, Integer> linkTimes) {
        List<WfTestResultVO.TestLinkVO> vos = new ArrayList<>();
        int passed = 0;
        for (WfNodeLink l : links) {
            Integer times = linkTimes.get(linkKey(l));
            int t = times == null ? 0 : times;
            WfTestResultVO.TestLinkVO vo = new WfTestResultVO.TestLinkVO();
            vo.setFromNodeKey(l.getFromNodeKey());
            vo.setToNodeKey(l.getToNodeKey());
            vo.setFromNodeName(nodeName(nodeList, l.getFromNodeKey()));
            vo.setToNodeName(nodeName(nodeList, l.getToNodeKey()));
            vo.setConditionExpr(l.getConditionExpr());
            vo.setConditionCn(l.getConditionCn());
            vo.setPassTimes(t);
            vo.setStatus(t > 0 ? 1 : 0);
            if (t > 0) {
                passed++;
            }
            vos.add(vo);
        }
        result.setLinks(vos);
        result.setLinkTotal(vos.size());
        result.setLinkPassed(passed);
    }

    private WfTestResultVO.TestScenarioVO toScenarioVo(int idx, Scenario sc, SingleRun sr) {
        WfTestResultVO.TestScenarioVO vo = new WfTestResultVO.TestScenarioVO();
        vo.setIndex(idx);
        vo.setLabel(sc.label);
        vo.setFormData(sc.formData);
        vo.setReachedEnd(sr.reachedEnd);
        vo.setPath(new ArrayList<>(sr.pathNodes));
        if (sr.fatal != null) {
            vo.setTestStatus(WfTestLog.TEST_ABORTED);
            vo.setSummary(sr.fatal.getValue());
        } else if (sr.reachedEnd) {
            vo.setTestStatus(WfTestLog.TEST_PASSED);
            vo.setSummary("真实走通到归档，经过 " + sr.visitedNodes.size() + " 个节点");
        } else {
            vo.setTestStatus(WfTestLog.TEST_FAILED);
            vo.setSummary(sr.aborted ? "驱动中断（步数超限或实例仍运行中）" : "未走到归档节点");
        }
        return vo;
    }

    /** 一个测试场景（一组表单变量 + 说明） */
    private static class Scenario {
        final String label;
        final Map<String, Object> formData;
        Scenario(String label, Map<String, Object> formData) {
            this.label = label;
            this.formData = formData;
        }
    }

    /** 单个场景真实跑完后的覆盖数据 */
    private static class SingleRun {
        Long instId;
        boolean reachedEnd;
        boolean aborted;
        Map.Entry<String, String> fatal;
        final Map<String, Integer> nodeTimes = new LinkedHashMap<>();
        final Set<String> visitedNodes = new LinkedHashSet<>();
        final List<String> pathNodes = new ArrayList<>();
        final Map<String, Integer> linkTimes = new LinkedHashMap<>();
    }

    /**
     * 测试前静态校验：在部署/真实发起前拦截明显非法或未设置的节点，避免运行期炸出原始引擎异常。
     * 返回 nodeKey -> 问题描述（空表示通过）。
     */
    private Map<String, String> validateBeforeRun(Long defId, List<WfNodeLink> links,
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
        // 2. 人工节点未设置操作者：审批(1) / 提交(2)
        Set<Long> opNodeIds = operators == null ? Collections.emptySet()
                : operators.stream().map(WfNodeOperator::getNodeId).collect(Collectors.toSet());
        for (WfProcessNode n : nodeList) {
            Integer t = n.getNodeType();
            if (t != null && (t == 1 || t == 2) && !opNodeIds.contains(n.getId())) {
                issues.put(n.getNodeKey(),
                    "未设置操作者，请在「节点信息-操作者」中配置办理人后再测试");
            }
        }
        // 3. 创建(0) / 归档(3)：必须有操作者 + 必须有表单内容（字段权限）。
        //    与「模拟运行」的校验口径保持一致；此前两条路径都只校验审批/提交的操作者，
        //    导致创建 / 归档节点即使操作者与表单内容全空也被判「通过」。
        Set<String> fieldPermNodeKeys = new LinkedHashSet<>();
        if (fieldPermMapper != null) {
            for (WfNodeFieldPerm p : fieldPermMapper.selectList(
                    Wrappers.<WfNodeFieldPerm>lambdaQuery().eq(WfNodeFieldPerm::getDefId, defId))) {
                if (p.getNodeKey() != null) {
                    fieldPermNodeKeys.add(p.getNodeKey());
                }
            }
        }
        for (WfProcessNode n : nodeList) {
            Integer t = n.getNodeType();
            if (t == null || (t != 0 && t != 3)) {
                continue;
            }
            String what = t == 0 ? "创建" : "归档";
            if (!opNodeIds.contains(n.getId())) {
                issues.putIfAbsent(n.getNodeKey(),
                    what + "节点未设置操作者，请在「节点信息-操作者」中配置后再测试");
                continue;
            }
        }
        // 4. 表单内容：会渲染表单的节点（0创建/1审批/2提交/3归档）必须设置为「节点布局」。
        //    本系统已屏蔽「普通模式」，故「是否设置了表单内容」= ext_json.settings.formContent.mode
        //    是否为 custom；未设置（含存量的 normal）一律视为未配置。
        for (WfProcessNode n : nodeList) {
            Integer t = n.getNodeType();
            if (t == null || t < 0 || t > 3) {
                continue;
            }
            if (!"custom".equals(WfNodeSettingsUtil.str(n.getExtJson(), "formContent", "mode"))) {
                issues.putIfAbsent(n.getNodeKey(),
                    "未设置表单内容，请在「节点信息-表单内容」选择「节点布局」后再测试");
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
