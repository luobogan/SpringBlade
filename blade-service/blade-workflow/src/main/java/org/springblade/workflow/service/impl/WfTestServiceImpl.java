package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.history.HistoricActivityInstance;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.dto.WfTestRunDTO;
import org.springblade.workflow.dto.WfTestStepDTO;
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
import org.springblade.workflow.vo.WfTaskVO;
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
import java.util.HashSet;
import java.util.Iterator;
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
    /** 交互式测试默认签字意见 */
    private static final String DEFAULT_TEST_OPINION = "流程测试提交";
    /** 解析 wf_form_snapshot.data_json 用 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    private final IFormmodeClient formmodeClient;

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
            // 开始节点必填校验（发起前）：开始节点在 start 时即自动完成，不会进入待办循环，
            // 必须在此用场景表单值校验，否则必填未填会被引擎直接放过（假通过）。
            String startErr = startNodeRequiredError(def, sc.formData);
            if (startErr != null) {
                WfProcessNode sn = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                    .eq(WfProcessNode::getDefId, defId)
                    .orderByAsc(WfProcessNode::getSortOrder)
                    .last("LIMIT 1"));
                String snKey = sn == null ? "start" : sn.getNodeKey();
                logLines.add(fmt.format(new Date()) + " " + startErr);
                sr.fatal = new AbstractMap.SimpleEntry<>(snKey, startErr);
                return sr;
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
                            // 布局级必填校验（表单设计器里配置的「必填」）：权限矩阵校验(formRenderService.validate)
                            // 只覆盖「字段权限=必填」，不覆盖「布局字段必填」，必须单独读布局 JSON 核对，
                            // 否则开始节点/审批节点表单必填未填也会被系统自动通过推进，测试「假通过」。
                            // 开始节点强制校验；其余节点仅当场景带表单值时校验（自动测试不填后续节点表单，空表单不误杀）。
                            boolean startNode = isStartNode(defId, t.getNodeKey());
                            if (startNode || (sc.formData != null && !sc.formData.isEmpty())) {
                                List<String> layoutMissing = new ArrayList<>();
                                collectLayoutRequired(def.getFormId(), t.getNodeKey(), sc.formData, layoutMissing);
                                if (!layoutMissing.isEmpty()) {
                                    throw new ServiceException("以下字段为必填： " + String.join("、", layoutMissing));
                                }
                            }
                        } catch (ServiceException ve) {
                            String msg = "节点【" + t.getNodeKey() + "】必填校验未通过（表单必填项未填写），已终止该场景："
                                + ve.getMessage();
                            logLines.add(fmt.format(new Date()) + " " + msg);
                            sr.fatal = new AbstractMap.SimpleEntry<>(t.getNodeKey(), msg);
                            return sr;
                        }
                        taskService.autoApprove(t.getId(), "测试自动通过", t.getAssignee());
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

            Coverage cov = collectCoverage(inst == null ? null : inst.getEngineInstId());
            sr.nodeTimes.putAll(cov.nodeTimes);
            sr.visitedNodes.addAll(cov.visitedNodes);
            sr.pathNodes.addAll(cov.pathNodes);
            sr.linkTimes.putAll(cov.linkTimes);
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
        saveTestLog(def, dto, result, logLines, null);
    }

    /**
     * 持久化测试日志。
     *
     * @param instId 交互式测试的实例ID（一次性测试传 null）；用于「测试历史」关联与去重
     */
    private void saveTestLog(WfProcessDefinition def, WfTestRunDTO dto, WfTestResultVO result,
            List<String> logLines, Long instId) {
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
        entity.setInstId(instId);
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

    // ==================================================================
    // 交互式测试：对齐 ecology「流程测试」——
    // 「开始测试」发起实例 → 「开始自动测试 / 暂停」逐节点驱动，或「手动提交」单步办理。
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfTestResultVO start(WfTestRunDTO dto) {
        if (dto == null || dto.getDefId() == null) {
            throw new ServiceException("流程定义ID不能为空");
        }
        if (dto.getTestUserId() == null) {
            throw new ServiceException("请选择测试发起人");
        }
        Long defId = dto.getDefId();
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
        List<WfProcessNode> nodeList = nodeMapper.selectList(
            Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
        List<WfNodeLink> links = linkMapper.selectList(
            Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, defId));
        List<Long> nodeIds = nodeList.stream().map(WfProcessNode::getId).collect(Collectors.toList());
        List<WfNodeOperator> operators = nodeIds.isEmpty() ? Collections.emptyList()
            : operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
                .in(WfNodeOperator::getNodeId, nodeIds));

        // 预校验：配置不完整直接拦截（与「一键测试」run 同口径）
        Map<String, String> issues = validateBeforeRun(defId, links, nodeList, operators);
        if (!issues.isEmpty()) {
            logLines.add(fmt.format(new Date()) + " 预校验发现 " + issues.size()
                + " 个节点配置问题，无法发起测试：");
            for (Map.Entry<String, String> en : issues.entrySet()) {
                logLines.add("    - 节点【" + nodeName(nodeList, en.getKey()) + "】" + en.getValue());
            }
            WfTestResultVO result = buildResult(nodeList, issues, null,
                Collections.emptyMap(), Collections.emptySet(), logLines, begin, false, true);
            saveTestLog(def, dto, result, logLines);
            return result;
        }

        try {
            String deploymentId = definitionService.deployForTest(defId);
            StartProcessDTO startDto = new StartProcessDTO();
            startDto.setDefId(defId);
            startDto.setFormId(def.getFormId());
            startDto.setStarter(dto.getTestUserId());
            startDto.setFieldValues(dto.getFormData());
            startDto.setTestFlag(true);
            startDto.setTestDeploymentId(deploymentId);
            startDto.setTitle("【测试】" + (def.getName() == null ? "" : def.getName()));
            startDto.setDataId(IdWorker.getId());
            // 说明：开始节点（创建/申请人）在 instanceService.start 时即被 advance() 自动完成、不生成待办。
            // 这里**不拦截**发起，目的是让实例先建出来 → 右侧面板直接显示真实实例表单，用户可在表单里
            // 补齐必填后手动「提交」/或点「开始自动测试」。开始节点的必填改在「本实例首次提交」时校验
            // （见 step()），既不会假通过，也不会出现「有必填项就连实例都建不出来」的死路。
            Long instId = instanceService.start(startDto);

            // 起始表单值落一份快照：后续「自动/手动测试」在未改表单时可据此做必填校验与网关变量
            if (dto.getFormData() != null && !dto.getFormData().isEmpty()) {
                saveSnapshot(instId, null, dto.getFormData());
            }

            WfTestResultVO result = state(instId);
            result.setCostMs(System.currentTimeMillis() - begin);
            result.setTestStatus(WfTestLog.TEST_RUNNING);
            result.setSummary("测试已发起：当前节点【" + result.getCurrentNodeName()
                + "】，可「开始自动测试」逐节点推进，或手动提交。");
            List<String> lines = new ArrayList<>();
            lines.add(fmt.format(new Date()) + " 已将草稿流程临时部署到引擎（deploymentId=" + deploymentId + "）");
            lines.add(fmt.format(new Date()) + " 已真实发起测试实例 instId=" + instId + "，等待办理");
            if (result.getLog() != null) {
                lines.addAll(result.getLog());
            }
            result.setLog(lines);
            return result;
        } catch (Exception e) {
            // 部署/发起阶段异常：优雅终止，定位首个业务节点
            String msg = "测试在部署/发起阶段中断（流程配置存在问题）：" + e.getMessage();
            logLines.add(fmt.format(new Date()) + " " + msg);
            issues = new LinkedHashMap<>();
            String firstNode = nodeList.isEmpty() ? null : nodeList.get(0).getNodeKey();
            if (firstNode != null) {
                issues.put(firstNode, msg);
            }
            WfTestResultVO result = buildResult(nodeList, issues, null,
                Collections.emptyMap(), Collections.emptySet(), logLines, begin, false, true);
            saveTestLog(def, dto, result, logLines);
            return result;
        }
    }

    @Override
    public WfTestResultVO state(Long instId) {
        if (instId == null) {
            throw new ServiceException("测试实例ID不能为空");
        }
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("测试实例不存在（可能已被清理）");
        }
        List<WfProcessNode> nodeList = nodeMapper.selectList(
            Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, inst.getDefId()));
        List<WfNodeLink> links = linkMapper.selectList(
            Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, inst.getDefId()));

        Coverage cov = collectCoverage(inst.getEngineInstId());
        List<WfTask> todos = pendingTestTasks(instId);
        boolean running = inst.getStatus() == null || WfInstance.STATUS_RUNNING == inst.getStatus();
        boolean reachedEnd = inst.getStatus() != null && WfInstance.STATUS_APPROVED == inst.getStatus();
        // 运行中且无待办 = 引擎未同步出待办（异常）
        boolean aborted = running && todos.isEmpty();

        long begin = inst.getStartTime() == null ? System.currentTimeMillis() : inst.getStartTime().getTime();
        List<String> logLines = buildProgressLog(inst, nodeList);

        WfTestResultVO result = buildResult(nodeList, new LinkedHashMap<>(), instId,
            cov.nodeTimes, cov.visitedNodes, logLines, begin, reachedEnd, aborted);
        result.setInstId(instId);
        result.setPath(buildPathFromLinkTimes(links, cov.linkTimes));
        result.setScenarioCount(1);
        fillLinkCoverage(result, links, nodeList, cov.linkTimes);
        result.setInstanceStatus(inst.getStatus());
        result.setCurrentNodeKey(inst.getCurrentNodeKey());
        result.setCurrentNodeName(nodeName(nodeList, inst.getCurrentNodeKey()));
        result.setHasPending(!todos.isEmpty());
        result.setCurrentTaskId(todos.isEmpty() ? null : todos.get(0).getId());

        if (running) {
            // 运行中：结论标记为「进行中」，未走到的节点提示为「等待推进」而非配置错误
            result.setTestStatus(WfTestLog.TEST_RUNNING);
            result.setSummary("测试进行中：当前节点【" + result.getCurrentNodeName() + "】"
                + (todos.isEmpty() ? "（引擎正在推进…）" : "，等待提交") + "。");
            if (result.getNodes() != null) {
                for (WfTestResultVO.TestNodeVO n : result.getNodes()) {
                    if (n.getStatus() != null && n.getStatus() == 0) {
                        n.setMessage("尚未走到（等待流程推进）");
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfTestResultVO step(WfTestStepDTO dto) {
        if (dto == null || dto.getInstId() == null) {
            throw new ServiceException("测试实例ID不能为空");
        }
        Long instId = dto.getInstId();
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("测试实例不存在（可能已被清理）");
        }
        // 已结束：原样返回当前状态（幂等）
        if (inst.getStatus() != null && WfInstance.STATUS_RUNNING != inst.getStatus()) {
            return state(instId);
        }

        // 手动测试：流程变量 =「实例最新快照」+「本次提交值」**合并**（累加，而非覆盖），并落一份快照。
        // ⚠️ 必须合并：测试者按节点逐个填写提交，前序节点已填的值（如申请人字段）不能丢——
        // 历史 bug：覆盖式导致「在业务领导节点提交时开始节点必填被反复报缺失」类问题，
        // 也会让后续排他网关取不到前面填的字段而选错分支。
        Map<String, Object> submitted = dto.getFormData();
        boolean hasSubmitted = submitted != null && !submitted.isEmpty();
        Map<String, Object> variables = new LinkedHashMap<>(loadLatestFormData(instId));
        if (hasSubmitted) {
            variables.putAll(submitted);
            saveSnapshot(instId, inst.getCurrentNodeKey(), variables);
        }

        List<WfTask> todos = pendingTestTasks(instId);
        if (todos.isEmpty()) {
            instanceService.advance(instId);
            todos = pendingTestTasks(instId);
        }
        if (todos.isEmpty()) {
            // 无待办可推进（多已到归档）：返回最新状态
            WfInstance after = instanceMapper.selectById(instId);
            WfTestResultVO r = state(instId);
            persistInteractiveLogIfFinished(after, r);
            return r;
        }

        // 当前待办所在节点：一次提交只办理「一个节点」的待办（交互式测试＝逐节点推进）
        String curNodeKey = todos.get(0).getNodeKey();

        // 「开始节点（申请人）」表单的提交：只做必填校验 + 落快照，**不推进引擎**。
        // 原因：开始节点在 instanceService.start 时已被 advance() 自动完成、不生成待办，引擎本就停在
        // 下一个待办节点上；若在这里顺手把那个待办办掉，测试者就再没机会办理它
        //（现象：「开始节点提交后，任务非待办状态，不可处理」）。
        // 返回的 currentNodeKey 会让前端面板自动切到该节点，继续办理。
        WfProcessNode firstNode = firstNodeOf(inst.getDefId());
        boolean fromStartNode = firstNode != null
            && firstNode.getNodeKey().equals(dto.getFormNodeKey())
            && todos.stream().noneMatch(x -> firstNode.getNodeKey().equals(x.getNodeKey()));
        if (fromStartNode) {
            ValidateDTO startVdto = new ValidateDTO();
            startVdto.setInstanceId(instId);
            startVdto.setDefId(inst.getDefId());
            startVdto.setNodeKey(firstNode.getNodeKey());
            startVdto.setFormData(variables);
            formRenderService.validate(startVdto);
            List<String> startMissing = new ArrayList<>();
            collectLayoutRequired(inst.getFormId(), firstNode.getNodeKey(), variables, startMissing);
            if (!startMissing.isEmpty()) {
                throw new ServiceException("开始节点【" + firstNode.getNodeName()
                    + "】表单必填未填： " + String.join("、", startMissing));
            }
            // 申请人在开始节点填的签字意见，补写到发起时那条「提交」日志上（发起时记的是空意见）。
            // 开始节点停在自己节点时该日志不展示；流转到下一节点后，logs() 会把它作为第一条展示，
            // 此时需要包含申请人的意见内容。
            if (StringUtil.isNotBlank(dto.getOpinion())) {
                WfApprovalLog submitLog = approvalLogMapper.selectOne(Wrappers.<WfApprovalLog>lambdaQuery()
                    .eq(WfApprovalLog::getInstId, instId)
                    .eq(WfApprovalLog::getLogType, WfApprovalLog.LOG_SUBMIT)
                    .orderByDesc(WfApprovalLog::getId)
                    .last("LIMIT 1"));
                if (submitLog != null) {
                    submitLog.setOpinion(dto.getOpinion());
                    approvalLogMapper.updateById(submitLog);
                }
            }
            WfInstance afterStart = instanceMapper.selectById(instId);
            WfTestResultVO r = state(instId);
            r.setSummary("已提交开始节点（申请人）表单，流程停在【" + r.getCurrentNodeName() + "】等待办理。");
            persistInteractiveLogIfFinished(afterStart, r);
            return r;
        }

        String opinion = StringUtil.isBlank(dto.getOpinion()) ? DEFAULT_TEST_OPINION : dto.getOpinion();
        for (WfTask t : todos) {
            // 一次提交只办理「当前待办节点」的待办（多节点并存时同样逐节点推进）
            if (!curNodeKey.equals(t.getNodeKey())) {
                continue;
            }
            // 逐条重新读库：或签办结首条时会 closeSiblings 关闭同节点其余待办并推进引擎，
            // 继续办理这些已关闭的待办会自动办理接口抛「任务非待办状态，不可处理」→
            // 整笔事务回滚（现象：提交报错、什么也没发生）。这里直接跳过已非待办的记录。
            WfTask fresh = taskMapper.selectById(t.getId());
            if (fresh == null || !Integer.valueOf(WfTask.STATUS_TODO).equals(fresh.getStatus())) {
                continue;
            }
            // 模拟真实提交：节点必填矩阵校验（未填则抛 ServiceException，前端提示补填后再提交）
            ValidateDTO vdto = new ValidateDTO();
            vdto.setInstanceId(instId);
            vdto.setDefId(inst.getDefId());
            vdto.setNodeKey(t.getNodeKey());
            vdto.setFormData(variables);
            formRenderService.validate(vdto);
            // 布局级必填（权限矩阵为空时，必填定义在布局 fieldMeta；与前端 ExcelPreview 红标一致）。
            // ① 开始节点（创建/申请人，nodeType=0）：无论自动还是手动都必须校验——否则「开始自动测试」
            //    在开始节点表单必填未填时会被引擎直接放过（假通过）。此处强制拦截，前端据此暂停并提示补填
            //    （对齐 ecology 自动测试的「必填阻塞」：填完再点继续）。
            // ② 其余节点：仅「手动提交且携带了表单值」时校验（交互式自动测试是系统空表单走查，
            //    必填由一键测试带场景变量反推核查），避免空表单被布局必填误杀成 400。
            boolean startNode = isStartNode(inst.getDefId(), t.getNodeKey());
            // 校验对象＝**本次提交值所属节点**（前端右侧表单当前所在节点，dto.formNodeKey）：
            // 一个表单在不同节点各有一份布局、必填各不相同；若固定按「待办节点」校验，就会出现
            // 「在开始节点表单填完提交，却被首个待办节点自己那份布局的必填拦住」的死锁
            // （那些字段不在开始节点表单里，测试者根本填不到）。未传 formNodeKey 时回退待办节点，
            // 兼容「开始自动测试」这类不带表单值的空表单走查。
            String checkNodeKey = StringUtil.isNotBlank(dto.getFormNodeKey()) ? dto.getFormNodeKey() : t.getNodeKey();
            if (startNode || hasSubmitted) {
                List<String> layoutMissing = new ArrayList<>();
                collectLayoutRequired(inst.getFormId(), checkNodeKey, variables, layoutMissing);
                if (!layoutMissing.isEmpty()) {
                    // 必须带「节点名」：否则用户只看到字段名，不知道是哪个节点的布局在要求必填。
                    WfProcessNode curNode = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                        .eq(WfProcessNode::getDefId, inst.getDefId())
                        .eq(WfProcessNode::getNodeKey, checkNodeKey)
                        .last("LIMIT 1"));
                    String curName = (curNode != null && curNode.getNodeName() != null && !curNode.getNodeName().isEmpty())
                        ? curNode.getNodeName() : checkNodeKey;
                    throw new ServiceException("节点【" + curName + "】以下字段为必填： "
                        + String.join("、", layoutMissing));
                }
            }
            // 首节点（创建/申请人）必填兜底：开始节点在 instanceService.start 时即被 advance() 自动完成、
            // 不生成待办，其表单只在本实例「首次提交」时才有机会校验——否则开始节点必填未填会被静默放过（假通过）。
            // 判据：该实例尚无「已办」任务（即当前是首次提交）；校验值用本次提交的表单值（含快照兜底）。
            // 注：firstNode 已在外层取好（同一提交内复用）。
            if (firstNode != null && !firstNode.getNodeKey().equals(t.getNodeKey())) {
                Long doneCount = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
                    .eq(WfTask::getInstId, instId)
                    .eq(WfTask::getStatus, WfTask.STATUS_DONE));
                if (doneCount == null || doneCount == 0) {
                    List<String> firstMissing = new ArrayList<>();
                    collectLayoutRequired(inst.getFormId(), firstNode.getNodeKey(), variables, firstMissing);
                    if (!firstMissing.isEmpty()) {
                        throw new ServiceException("开始节点【" + firstNode.getNodeName()
                            + "】表单必填未填： " + String.join("、", firstMissing));
                    }
                }
            }
            // 以节点「接收人」身份审批（skip 操作菜单），但保留「意见必填」「字段校验」等业务规则
            taskService.autoApprove(t.getId(), opinion, variables, t.getAssignee());
        }

        WfInstance after = instanceMapper.selectById(instId);
        WfTestResultVO r = state(instId);
        persistInteractiveLogIfFinished(after, r);
        return r;
    }

    @Override
    public List<WfTaskVO> todo(Long instId) {
        List<WfTaskVO> vos = new ArrayList<>();
        if (instId == null) {
            return vos;
        }
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return vos;
        }
        List<WfProcessNode> nodeList = nodeMapper.selectList(
            Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, inst.getDefId()));
        for (WfTask t : pendingTestTasks(instId)) {
            WfTaskVO vo = new WfTaskVO();
            vo.setId(t.getId());
            vo.setInstId(t.getInstId());
            vo.setNodeKey(t.getNodeKey());
            vo.setNodeName(nodeName(nodeList, t.getNodeKey()));
            vo.setAssignee(t.getAssignee());
            vo.setStatus(t.getStatus());
            vo.setReceiveTime(t.getReceiveTime());
            vo.setDueTime(t.getDueTime());
            vo.setTitle(inst.getTitle());
            vo.setFormId(inst.getFormId());
            vo.setDataId(inst.getDataId());
            vo.setStarter(inst.getStarter());
            vo.setStartTime(inst.getStartTime());
            vo.setUrgency(inst.getUrgency());
            vos.add(vo);
        }
        return vos;
    }

    /** 是否开始节点（创建/申请人，nodeType=0）——其表单必填在自动测试中也要强制校验 */
    private boolean isStartNode(Long defId, String nodeKey) {
        if (defId == null || StringUtil.isBlank(nodeKey)) {
            return false;
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        return node != null && node.getNodeType() != null && node.getNodeType() == 0;
    }

    /**
     * 首节点（{@code sortOrder} 最小）＝ {@code instanceService.start} 内 {@code advance()} 自动完成的
     * 「创建/申请人」节点；其表单在发起时提交，不会生成待办，因此必填需单独在首次提交时兜底校验。
     */
    private WfProcessNode firstNodeOf(Long defId) {
        if (defId == null) {
            return null;
        }
        return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .orderByAsc(WfProcessNode::getSortOrder)
            .last("LIMIT 1"));
    }

    /**
     * 开始节点（nodeType=0）必填校验：在 instanceService.start 发起之前调用。
     * <p>关键点：开始节点在 {@code instanceService.start} 内部 {@code advance()} 时被<strong>自动完成</strong>，
     * 不会生成待办任务，因此永远不会进入「自动测试」的待办循环或「手动提交」的 step 校验分支。
     * 若不在发起前单独校验，开始节点表单必填未填会被引擎直接放过，测试「假通过」。</p>
     * <p>同时覆盖两类必填：①「字段权限=必填」（权限矩阵，formRenderService.validate）；
     * ②「布局字段必填」（formmodeClient 布局 JSON 的 fieldMeta.required / fieldAttr==3）。</p>
     *
     * @return 错误信息（含缺失字段）；为 null 表示校验通过
     */
    private String startNodeRequiredError(WfProcessDefinition def, Map<String, Object> formData) {
        if (def == null) {
            return null;
        }
        // 首节点（sortOrder 最小）＝ advance() 在发起时自动完成的那个节点，其表单（sc.formData）
        // 正是测试提交的开始节点表单。注意：首节点未必是 nodeType=0，故按 sortOrder 取而非按 nodeType。
        WfProcessNode startNode = firstNodeOf(def.getId());
        if (startNode == null) {
            return null;
        }
        Map<String, Object> fd = formData == null ? Collections.emptyMap() : formData;
        // ① 权限矩阵必填（字段权限=必填）
        try {
            ValidateDTO vdto = new ValidateDTO();
            vdto.setDefId(def.getId());
            vdto.setNodeKey(startNode.getNodeKey());
            vdto.setFormData(fd);
            formRenderService.validate(vdto);
        } catch (ServiceException ve) {
            return "开始节点【" + startNode.getNodeName() + "】必填校验未通过（字段权限）：" + ve.getMessage();
        }
        // ② 布局级必填（表单设计器配置的「必填」）
        List<String> layoutMissing = new ArrayList<>();
        collectLayoutRequired(def.getFormId(), startNode.getNodeKey(), fd, layoutMissing);
        if (!layoutMissing.isEmpty()) {
            return "开始节点【" + startNode.getNodeName() + "】必填校验未通过（布局必填）：" + String.join("、", layoutMissing);
        }
        return null;
    }

    /**
     * 布局级必填校验：读取当前节点布局，解析每个 sheet 的 cellData，
     * 凡 {@code fieldMeta.fieldAttr==3}（必填）或 {@code fieldMeta.required==true} 的字段，
     * 校验 formData 是否已有值。
     *
     * <p>兼容两种表单值 key（与前端 ExcelPreview 对齐）：
     * ① 单元格 key：{@code {sheetId}__{row}__{col}}（测试页手动提交使用）；
     * ② 字段名（一键测试变量反推场景可能使用）。</p>
     *
     * <p>布局 JSON 兼容三种形态：{@code sheets} 对象、{@code layout[sheetName].sheets}、
     * 顶层直接承载 {@code cellData}（老格式）。解析失败则降级跳过（不阻塞测试），仅打 warn。</p>
     */
    private void collectLayoutRequired(Long formId, String nodeKey, Map<String, Object> formData, List<String> missing) {
        if (formId == null || formData == null || missing == null) {
            return;
        }
        try {
            org.springblade.core.tool.api.R<org.springblade.formmode.vo.FormLayoutVO> layoutResult =
                formmodeClient.getFormLayout(formId, 0, nodeKey);
            if (layoutResult == null || !layoutResult.isSuccess() || layoutResult.getData() == null) {
                return;
            }
            String layoutJson = layoutResult.getData().getLayoutJson();
            if (layoutJson == null || layoutJson.isEmpty()) {
                return;
            }
            JsonNode root = OBJECT_MAPPER.readTree(layoutJson);
            List<JsonNode> sheets = new ArrayList<>();
            JsonNode sheetsNode = root.get("sheets");
            if (sheetsNode != null && sheetsNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = sheetsNode.fields();
                while (it.hasNext()) {
                    sheets.add(it.next().getValue());
                }
            } else if (sheetsNode != null && sheetsNode.isArray()) {
                sheetsNode.forEach(sheets::add);
            } else if (root.get("cellData") != null) {
                sheets.add(root);
            }
            // 按「字段」聚合判定，而不是按「单元格」逐个判定：
            // 同一字段在布局里通常占两个格——标签格（cellType=label）与值格（cellType=field），
            // 且两格都可能带必填标记；值只可能落在值格上。若按格逐个判定并用 checked 去重，
            // 先遍历到的标签格（没有值）就会把该字段判为缺失，随后值格被去重跳过
            // → 用户明明填了值仍报「必填未填」（历史 bug）。
            // 正确口径：必填 = 该字段任一方格标了必填；已填 = 任一方格的单元格 key 或字段名在表单值里有值。
            Map<String, Boolean> fieldRequired = new LinkedHashMap<>();
            Map<String, Boolean> fieldFilled = new LinkedHashMap<>();
            Map<String, String> fieldLabel = new LinkedHashMap<>();
            for (JsonNode sheet : sheets) {
                String sheetId = sheetIdOf(sheet, root);
                JsonNode cellData = sheet.get("cellData");
                if (cellData == null || !cellData.isObject()) {
                    continue;
                }
                Iterator<Map.Entry<String, JsonNode>> rowIt = cellData.fields();
                while (rowIt.hasNext()) {
                    Map.Entry<String, JsonNode> rowEntry = rowIt.next();
                    JsonNode rowObj = rowEntry.getValue();
                    if (!rowObj.isObject()) {
                        continue;
                    }
                    Iterator<Map.Entry<String, JsonNode>> colIt = rowObj.fields();
                    while (colIt.hasNext()) {
                        Map.Entry<String, JsonNode> colEntry = colIt.next();
                        JsonNode cell = colEntry.getValue();
                        if (!cell.isObject()) {
                            continue;
                        }
                        JsonNode fieldMeta = cell.get("fieldMeta");
                        if (fieldMeta == null || !fieldMeta.isObject()) {
                            continue;
                        }
                        JsonNode cellType = fieldMeta.get("cellType");
                        // 只统计「输入格」：cellType 为空（老布局）或 =field；
                        // 标签格（label）/元素格/明细表标记等只是展示，不承载值。
                        // 与前端 ExcelPreview 的提交校验口径一致（它仅对 cellType==='field' 的格做必填判定）。
                        if (cellType != null && !cellType.asText().isEmpty() && !"field".equals(cellType.asText())) {
                            continue;
                        }
                        JsonNode fieldNameNode = fieldMeta.get("fieldName");
                        String fieldName = fieldNameNode != null ? fieldNameNode.asText() : null;
                        if (fieldName == null || fieldName.isEmpty()) {
                            continue;
                        }
                        boolean required = false;
                        JsonNode fieldAttr = fieldMeta.get("fieldAttr");
                        if (fieldAttr != null && fieldAttr.asInt(0) == 3) {
                            required = true;
                        }
                        JsonNode requiredNode = fieldMeta.get("required");
                        if (requiredNode != null && requiredNode.asBoolean(false)) {
                            required = true;
                        }
                        String cellKeyStr = sheetId + "__" + rowEntry.getKey() + "__" + colEntry.getKey();
                        boolean filled = !isBlank(formData.get(cellKeyStr)) || !isBlank(formData.get(fieldName));
                        fieldRequired.merge(fieldName, required, Boolean::logicalOr);
                        fieldFilled.merge(fieldName, filled, Boolean::logicalOr);
                        if (!fieldLabel.containsKey(fieldName)) {
                            // 提示信息优先用「字段标签(字段名)」：标签可读，字段名便于定位/程序处理
                            JsonNode labelNode = fieldMeta.get("fieldLabel");
                            if (labelNode == null || labelNode.asText().isEmpty()) {
                                labelNode = fieldMeta.get("label");
                            }
                            String label = (labelNode != null && !labelNode.asText().isEmpty())
                                ? labelNode.asText() : fieldName;
                            fieldLabel.put(fieldName, label);
                        }
                    }
                }
            }
            // 汇总缺失：必填 且 所有格都没有值
            for (Map.Entry<String, Boolean> en : fieldRequired.entrySet()) {
                if (!Boolean.TRUE.equals(en.getValue())) {
                    continue;
                }
                String fieldName = en.getKey();
                if (Boolean.TRUE.equals(fieldFilled.get(fieldName))) {
                    continue;
                }
                String label = fieldLabel.getOrDefault(fieldName, fieldName);
                missing.add(label.equals(fieldName) ? fieldName : (label + "(" + fieldName + ")"));
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 布局必填校验解析失败，已降级跳过. formId={}, nodeKey={}", formId, nodeKey, e);
        }
    }

    private static String sheetIdOf(JsonNode sheet, JsonNode root) {
        JsonNode id = sheet.get("id");
        if (id != null && !id.asText().isEmpty()) {
            return id.asText();
        }
        JsonNode sheetName = root.get("sheetName");
        if (sheetName != null && !sheetName.asText().isEmpty()) {
            return sheetName.asText();
        }
        return "sheet1";
    }

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.trim().isEmpty();
        }
        return false;
    }

    /** 采集引擎历史活动 → 覆盖数据（节点经过次数 / 访问集合 / 节点序列 / 相邻流转）。 */
    private Coverage collectCoverage(String engineInstId) {
        Coverage cov = new Coverage();
        if (engineInstId == null) {
            return cov;
        }
        try {
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
                cov.nodeTimes.merge(aid, 1, Integer::sum);
                cov.visitedNodes.add(aid);
                cov.pathNodes.add(aid);
                if (prev != null && !prev.equals(aid)) {
                    // 相邻的两个活动即一次真实流转；网关也是活动，
                    // 故「prev→cur」可直接对应到一条物理出口（wf_node_link）
                    cov.linkTimes.merge(prev + "→" + aid, 1, Integer::sum);
                }
                prev = aid;
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 流程测试读取历史活动失败. engineInstId={}, err={}", engineInstId, e.getMessage());
        }
        return cov;
    }

    /** 由审批日志还原「测试进度日志」（交互式测试逐次提交的痕迹） */
    private List<String> buildProgressLog(WfInstance inst, List<WfProcessNode> nodeList) {
        List<String> lines = new ArrayList<>();
        if (inst == null || inst.getId() == null) {
            return lines;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<WfApprovalLog> logs = approvalLogMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, inst.getId())
            .orderByAsc(WfApprovalLog::getOperateTime)
            .orderByAsc(WfApprovalLog::getId));
        for (WfApprovalLog l : logs) {
            String op = stripHtml(l.getOpinion());
            lines.add(fmt.format(l.getOperateTime() == null ? new Date() : l.getOperateTime())
                + " 节点【" + nodeName(nodeList, l.getNodeKey()) + "】" + logTypeCn(l.getLogType())
                + (StringUtil.isBlank(op) ? "" : "：" + op));
        }
        if (inst.getStatus() != null && WfInstance.STATUS_RUNNING != inst.getStatus()) {
            lines.add(fmt.format(inst.getEndTime() == null ? new Date() : inst.getEndTime())
                + " 流程已结束（" + instanceStatusCn(inst.getStatus()) + "）");
        }
        return lines;
    }

    /**
     * 交互式测试跑到终态时落一条测试日志（同一实例只落一条，重复调用跳过）,
     * 使其在「测试历史」中可见。
     */
    private void persistInteractiveLogIfFinished(WfInstance inst, WfTestResultVO result) {
        if (inst == null || inst.getStatus() == null || WfInstance.STATUS_RUNNING == inst.getStatus()) {
            return;
        }
        try {
            Long existed = testLogMapper.selectCount(Wrappers.<WfTestLog>lambdaQuery()
                .eq(WfTestLog::getInstId, inst.getId()));
            if (existed != null && existed > 0) {
                return;
            }
            WfProcessDefinition def = defMapper.selectById(inst.getDefId());
            if (def == null) {
                return;
            }
            WfTestRunDTO dto = new WfTestRunDTO();
            dto.setDefId(def.getId());
            dto.setTestUserId(inst.getStarter());
            saveTestLog(def, dto, result, result.getLog() == null ? new ArrayList<>() : result.getLog(), inst.getId());
        } catch (Exception e) {
            // 容错：wf_test_log.inst_id 未执行迁移时不影响测试推进
            log.warn("[blade-workflow] 交互式测试日志落库失败（请确认已执行 V2026.09.19_002 迁移）. instId={}, err={}",
                inst.getId(), e.getMessage());
        }
    }

    /** 取实例最新一份表单快照的值（无快照时返回空 Map） */
    private Map<String, Object> loadLatestFormData(Long instId) {
        if (instId == null) {
            return new LinkedHashMap<>();
        }
        List<WfFormSnapshot> snaps = snapshotMapper.selectList(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .orderByDesc(WfFormSnapshot::getId)
            .last("LIMIT 1"));
        if (snaps == null || snaps.isEmpty() || StringUtil.isBlank(snaps.get(0).getDataJson())) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> m = OBJECT_MAPPER.readValue(snaps.get(0).getDataJson(),
                new TypeReference<LinkedHashMap<String, Object>>() { });
            return m == null ? new LinkedHashMap<>() : m;
        } catch (Exception e) {
            log.warn("[blade-workflow] 流程测试解析表单快照失败. instId={}, err={}", instId, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /** 落一份表单数据快照（交互式测试手动提交时留痕） */
    private void saveSnapshot(Long instId, String nodeKey, Map<String, Object> formData) {
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(instId);
        snap.setNodeKey(nodeKey);
        snap.setDataJson(JsonUtil.toJson(formData));
        snapshotMapper.insert(snap);
    }

    private String logTypeCn(String type) {
        if (type == null) {
            return "已办理";
        }
        switch (type) {
            case WfApprovalLog.LOG_APPROVE: return "已批准";
            case WfApprovalLog.LOG_SUBMIT: return "已提交";
            case WfApprovalLog.LOG_REJECT: return "已退回";
            case WfApprovalLog.LOG_FORWARD: return "已转发";
            case WfApprovalLog.LOG_COMMENT: return "已批注";
            case WfApprovalLog.LOG_TRANSFER: return "已转办";
            case WfApprovalLog.LOG_SUPERVISE: return "已督办";
            case WfApprovalLog.LOG_CIRCULATE: return "已抄送";
            case WfApprovalLog.LOG_INSTRUCTION: return "已批示";
            default: return "已办理";
        }
    }

    private String instanceStatusCn(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case WfInstance.STATUS_RUNNING: return "运行中";
            case WfInstance.STATUS_APPROVED: return "已通过";
            case WfInstance.STATUS_REJECTED: return "已退回";
            case WfInstance.STATUS_CANCELED: return "已撤销";
            case WfInstance.STATUS_SUSPENDED: return "已暂停";
            default: return "状态" + status;
        }
    }

    /** 去掉富文本标签，用于日志单行展示 */
    private String stripHtml(String html) {
        if (StringUtil.isBlank(html)) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "")
            .replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .trim();
    }

    /** 一次测试的覆盖采集结果 */
    private static class Coverage {
        final Map<String, Integer> nodeTimes = new LinkedHashMap<>();
        final Set<String> visitedNodes = new LinkedHashSet<>();
        final List<String> pathNodes = new ArrayList<>();
        final Map<String, Integer> linkTimes = new LinkedHashMap<>();
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
