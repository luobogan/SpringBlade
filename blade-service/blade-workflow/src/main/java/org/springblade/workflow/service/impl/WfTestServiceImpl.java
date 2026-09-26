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
import org.flowable.engine.history.HistoricProcessInstance;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.exception.WfAccessDeniedException;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.system.user.feign.IUserClient;
import org.springblade.workflow.constant.WorkflowConstant;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
    /**
     * 代跑签字意见统一前缀（方案 C15「留痕需自证」）。
     *
     * <p>代跑是「以节点接收人身份」提交的，留痕落在真人名下；而 {@code wf_task} 无
     * {@code operate_user} 列、Blade 的审计列自动填充在本项目未生效、Flowable 历史的
     * {@code ASSIGNEE_}/{@code COMPLETED_BY_} 也全为 NULL。若意见文本不带标识，
     * 这条留痕与真实审批<b>完全无法区分</b>（实测库里是手填的 '2'/'3'/'23'）。
     * 统一加前缀后，即便数据从任何入口泄出，也能一眼识别为测试。</p>
     */
    private static final String TEST_OPINION_PREFIX = "[测试] ";
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
                SingleRun sr = runSingle(def, deploymentId, testUserId, sc, fmt, logLines, idx, nodeList, links);
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
            Scenario sc, SimpleDateFormat fmt, List<String> logLines, int idx,
            List<WfProcessNode> nodeList, List<WfNodeLink> links) {
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
            // 引擎用「测试独立 key」启动：与 deployForTest 成对，避免测试部署顶替正式版本
            startDto.setEngineKey(def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX);
            // 同 start()：按「测试版本」的定义ID启动，与正式版本物理隔离（方案 §3.4）
            startDto.setProcDefId(
                processService.latestProcDefId(def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX));
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

            // 叙述：创建（开始）节点（引擎在 start 时已自动办结，不在待办循环里）
            WfProcessNode createNode = null;
            for (WfProcessNode n : nodeList) {
                if (n.getNodeType() != null && n.getNodeType() == 0) {
                    createNode = n;
                    break;
                }
            }
            String lastNodeKey = null;
            boolean[] started = { false };
            if (createNode != null) {
                List<Long> createOps = new ArrayList<>();
                createOps.add(testUserId);
                appendArrival(logLines, fmt, new Date(),
                    nodeName(nodeList, createNode.getNodeKey()), createOps, started);
                appendNodeSubmit(logLines, fmt, new Date(),
                    nodeName(nodeList, createNode.getNodeKey()), createOps);
                lastNodeKey = createNode.getNodeKey();
            }

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
                String curNodeKey = todos.get(0).getNodeKey();
                // 执行出口（离开上一节点）
                if (lastNodeKey != null && !lastNodeKey.equals(curNodeKey)) {
                    String condCn = findCondCn(links, nodeList, lastNodeKey, curNodeKey);
                    logLines.add(fmt.format(new Date()) + " 执行出口\"" + condCn + "\"");
                    if (isCreateNode(nodeList, lastNodeKey)) {
                        logLines.add(fmt.format(new Date()) + " 出口\"" + condCn + "\"生成流程编号");
                    }
                }
                // 聚合本节点办理人（会签 / 多任务时合并展示，用于「到达节点」列出完整名单）
                List<Long> ops = new ArrayList<>();
                for (WfTask t : todos) {
                    if (t.getAssignee() != null && !ops.contains(t.getAssignee())) {
                        ops.add(t.getAssignee());
                    }
                }
                // 本次实际办理人（while 每轮只 approve 掉 todos 的第一人，避免会签其余人被重复列出）
                List<Long> approvedOps = new ArrayList<>();
                if (todos.get(0).getAssignee() != null) {
                    approvedOps.add(todos.get(0).getAssignee());
                }
                appendArrival(logLines, fmt, new Date(), nodeName(nodeList, curNodeKey), ops, started);
                appendNodeSubmit(logLines, fmt, new Date(), nodeName(nodeList, curNodeKey), approvedOps);
                lastNodeKey = curNodeKey;
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
                        // 一键测试的自动通过意见同样加统一前缀（C15）
                        taskService.autoApprove(t.getId(), testOpinion("测试自动通过"), t.getAssignee());
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
        // 交互式测试（instId 非空）：同一实例复用同一条「测试历史」（方案 C15 逐步可追溯）——
        // 每次提交都刷新进度与执行人，而不是等到跑到归档才落一条（进行中即 TEST_RUNNING）。
        WfTestLog entity = (instId == null) ? null : testLogMapper.selectOne(
            Wrappers.<WfTestLog>lambdaQuery()
                .eq(WfTestLog::getInstId, instId)
                .orderByDesc(WfTestLog::getId)
                .last("LIMIT 1"));
        if (entity == null) {
            entity = new WfTestLog();
            // 显式落「谁在执行本次测试」：Blade 的审计列自动填充在本项目未生效
            // （实测 create_user 全为 NULL），不显式设置就永远答不上「谁点的提交」；
            // 更新分支沿用首次执行人，不覆盖
            entity.setCreateUser(SecureUtil.getUserId());
        }
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
        if (entity.getId() == null) {
            testLogMapper.insert(entity);
        } else {
            testLogMapper.updateById(entity);
        }
        result.setLogId(entity.getId());
    }

    /**
     * 给代跑产生的签字意见加统一前缀（方案 C15，幂等：已带前缀不重复加；空值用默认意见）。
     */
    private static String testOpinion(String opinion) {
        String body = StringUtil.isBlank(opinion) ? DEFAULT_TEST_OPINION : opinion;
        return body.startsWith(TEST_OPINION_PREFIX) ? body : TEST_OPINION_PREFIX + body;
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
        // 本次将被删除的实例ID：用于把「测试日志 / 测试部署」与实例对齐清理（方案 C9/C10）
        Set<Long> removingInstIds = new LinkedHashSet<>();
        int bizDeleted = 0;
        for (WfInstance inst : insts) {
            removingInstIds.add(inst.getId());
            // 业务表行：测试期若建过业务行（历史数据，见方案 V13 / C17），这里一并删除，
            // 避免留下「有行、无 request_id」的无主脏行。
            // ⚠️ 安全条件：仅当本实例**未回填过 request_id**（request_id_bound != 1）才删 ——
            //    若测试复用了一张真实单据的 dataId 且已绑定流程，那行是别人的真实业务数据，绝不删。
            if (inst.getFormId() != null && inst.getDataId() != null
                && (inst.getRequestIdBound() == null || inst.getRequestIdBound() != 1)) {
                try {
                    R<Boolean> dr = formmodeClient.deleteBusinessData(inst.getFormId(), inst.getDataId());
                    if (dr != null && Boolean.TRUE.equals(dr.getData())) {
                        bizDeleted++;
                    } else {
                        log.warn("[blade-workflow] 清理测试业务数据失败（业务行可能残留）. instId={}, formId={}, dataId={}, msg={}",
                            inst.getId(), inst.getFormId(), inst.getDataId(), dr == null ? "无响应" : dr.getMsg());
                    }
                } catch (Exception e) {
                    // 跨服务删除失败不能阻断 wf_* 清理：业务行残留有巡检 ⑰⑱ 兜底
                    log.warn("[blade-workflow] 清理测试业务数据异常. instId={}, dataId={}", inst.getId(), inst.getDataId(), e);
                }
            }
            taskMapper.delete(Wrappers.<WfTask>lambdaQuery().eq(WfTask::getInstId, inst.getId()));
            approvalLogMapper.delete(Wrappers.<WfApprovalLog>lambdaQuery().eq(WfApprovalLog::getInstId, inst.getId()));
            snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery().eq(WfFormSnapshot::getInstId, inst.getId()));
            if (StringUtil.isNotBlank(inst.getTestDeploymentId())) {
                deployments.add(inst.getTestDeploymentId());
            }
        }
        // ② 测试日志（wf_test_log）：与实例同源清理（方案 C9/C10 / S12 / V-8）。
        //    原实现不清理，导致「测试历史」越积越多、且指向已被删除的实例ID。
        //    按被清理实例的 inst_id 精确删；defId 明确时再按 defId 补一次
        //    （含「一键测试 run」这类没有实例ID 的日志）。
        int logDeleted = 0;
        if (!removingInstIds.isEmpty()) {
            logDeleted += testLogMapper.delete(Wrappers.<WfTestLog>lambdaQuery()
                .in(WfTestLog::getInstId, removingInstIds));
        }
        if (defId != null) {
            logDeleted += testLogMapper.delete(Wrappers.<WfTestLog>lambdaQuery()
                .eq(WfTestLog::getDefId, defId));
        }

        // ③ 卸载测试部署：本次实例引用的 + 「孤儿」测试部署（方案 C9/C10）。
        //    孤儿的来源：实例被手工删除、或上次清理时卸载失败 —— 它们会长期留在引擎库，
        //    既白占空间，又让巡检 ⑥ 误报「latest 部署被测试顶替」。
        //    判据：扫出全部 __test 部署，去掉「仍被其他测试实例引用」的那些，剩下的都卸。
        Set<String> liveDeployments = new LinkedHashSet<>();
        for (WfInstance left : instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getIsTest, 1)
            .isNotNull(WfInstance::getTestDeploymentId))) {
            if (!removingInstIds.contains(left.getId()) && StringUtil.isNotBlank(left.getTestDeploymentId())) {
                liveDeployments.add(left.getTestDeploymentId());
            }
        }
        Set<String> toUndeploy = new LinkedHashSet<>(deployments);
        try {
            for (String dep : processService.deploymentIdsByKeyLike(
                "%" + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX)) {
                if (!liveDeployments.contains(dep)) {
                    toUndeploy.add(dep);
                }
            }
        } catch (Exception e) {
            // 引擎查询失败不影响实例/日志清理：孤儿部署残留可下次清理或手工卸载
            log.warn("[blade-workflow] 扫描孤儿测试部署失败（跳过，不影响实例清理）", e);
        }
        for (String dep : toUndeploy) {
            try {
                processService.deleteDeployment(dep);
            } catch (Exception e) {
                log.warn("[blade-workflow] 测试部署卸载失败（可能已手动删除）. deploymentId={}, err={}", dep, e.getMessage());
            }
        }
        instanceMapper.delete(q);
        log.info("[blade-workflow] 已清理测试数据. defId={}, 实例数={}, 卸载部署数={}, 清理业务行数={}, 清理测试日志数={}",
            defId, insts.size(), toUndeploy.size(), bizDeleted, logDeleted);
        return insts.size();
    }

    @Override
    public Map<String, Object> shadowCompare(Long defId, Long baseDefId, Long testUserId) {
        if (defId == null) {
            throw new ServiceException("流程定义ID不能为空");
        }
        WfProcessDefinition target = defMapper.selectById(defId);
        if (target == null) {
            throw new ServiceException("流程定义（目标版本）不存在");
        }
        WfProcessDefinition base = baseDefId != null ? defMapper.selectById(baseDefId) : prevVersionOf(target);
        if (base == null) {
            throw new ServiceException("找不到对照的旧版本：请显式传入 baseDefId，或先发布一个历史版本");
        }
        Long starter = testUserId != null ? testUserId
            : (target.getCreateUser() != null ? target.getCreateUser() : SecureUtil.getUserId());
        if (starter == null) {
            throw new ServiceException("无法确定测试发起人：请显式传入 testUserId");
        }

        // 先记下「跑之前已存在的测试实例」，跑完后做差集 —— 这样收尾只清理本次比对产生的实例，
        // 不会误删该流程既有的测试历史与用户手动跑出来的测试数据
        Set<Long> before = shadowExistingInstIds(base.getId(), target.getId());

        // 两边各跑一次：复用 run（临时部署 → 真实发起 → 自动驱动到终态 → 采集节点/出口覆盖）
        WfTestResultVO baseResult = run(shadowDto(base.getId(), starter));
        WfTestResultVO targetResult = run(shadowDto(target.getId(), starter));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseDefId", base.getId());
        out.put("baseVersion", base.getVersion());
        out.put("targetDefId", target.getId());
        out.put("targetVersion", target.getVersion());
        out.put("base", shadowSummary(baseResult));
        out.put("target", shadowSummary(targetResult));

        List<String> diffs = new ArrayList<>();
        if (!Objects.equals(baseResult.getReachedEnd(), targetResult.getReachedEnd())) {
            diffs.add("是否走到归档不一致：base=" + baseResult.getReachedEnd()
                + "，target=" + targetResult.getReachedEnd());
        }
        if (!Objects.equals(baseResult.getTestStatus(), targetResult.getTestStatus())) {
            diffs.add("测试结论不一致：base=" + baseResult.getTestStatus()
                + "，target=" + targetResult.getTestStatus());
        }
        // 走通节点集合差异（新增/消失的节点最需要人工确认）
        Set<String> basePassed = passedNodeKeys(baseResult);
        Set<String> targetPassed = passedNodeKeys(targetResult);
        Set<String> onlyBase = new LinkedHashSet<>(basePassed);
        onlyBase.removeAll(targetPassed);
        Set<String> onlyTarget = new LinkedHashSet<>(targetPassed);
        onlyTarget.removeAll(basePassed);
        if (!onlyBase.isEmpty() || !onlyTarget.isEmpty()) {
            diffs.add("走通节点集合不一致：仅base走通=" + onlyBase + "，仅target走通=" + onlyTarget);
        }
        // 节点经过次数差异（会签/回退/循环路径变化会体现在这里）
        Map<String, Integer> baseTimes = baseResult.getNodeTimes() == null
            ? Collections.emptyMap() : baseResult.getNodeTimes();
        Map<String, Integer> targetTimes = targetResult.getNodeTimes() == null
            ? Collections.emptyMap() : targetResult.getNodeTimes();
        Set<String> allNodes = new LinkedHashSet<>(baseTimes.keySet());
        allNodes.addAll(targetTimes.keySet());
        for (String k : allNodes) {
            int b = baseTimes.get(k) == null ? 0 : baseTimes.get(k);
            int t = targetTimes.get(k) == null ? 0 : targetTimes.get(k);
            if (b != t) {
                diffs.add("节点【" + k + "】经过次数不一致：base=" + b + "，target=" + t);
            }
        }
        // 流转路径顺序差异
        List<String> basePath = pathKeys(baseResult);
        List<String> targetPath = pathKeys(targetResult);
        if (!basePath.equals(targetPath)) {
            diffs.add("流转路径不一致：base=" + basePath + "，target=" + targetPath);
        }
        out.put("diffs", diffs);
        out.put("identical", diffs.isEmpty());

        // 收尾：只清理本次比对新增的测试实例（含其任务/日志/快照与测试部署）
        cleanupShadowInstances(base.getId(), target.getId(), before);
        return out;
    }

    /** 目标版本「上一版」：同 procKey 下 version 小于当前的最大版本（版本组内） */
    private WfProcessDefinition prevVersionOf(WfProcessDefinition def) {
        if (def == null || def.getProcKey() == null || def.getVersion() == null) {
            return null;
        }
        return defMapper.selectOne(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, def.getProcKey())
            .lt(WfProcessDefinition::getVersion, def.getVersion())
            .orderByDesc(WfProcessDefinition::getVersion)
            .last("LIMIT 1"));
    }

    private WfTestRunDTO shadowDto(Long defId, Long starter) {
        WfTestRunDTO dto = new WfTestRunDTO();
        dto.setDefId(defId);
        dto.setTestUserId(starter);
        return dto;
    }

    /** 影子比对的「跑之前」快照：两个定义下已存在的测试实例ID */
    private Set<Long> shadowExistingInstIds(Long... defIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Long defId : defIds) {
            if (defId == null) {
                continue;
            }
            for (WfInstance i : instanceMapper.selectList(Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getIsTest, 1)
                .eq(WfInstance::getDefId, defId)
                .select(WfInstance::getId))) {
                ids.add(i.getId());
            }
        }
        return ids;
    }

    /**
     * 影子比对收尾：只删「本次比对新增」的测试实例及其任务/流转日志/快照与测试部署。
     *
     * <p>为什么不直接调 {@link #cleanupTestData}：后者按 defId 清理，会连带删掉该流程
     * <b>既有的测试历史</b>（用户手动跑过的测试记录）—— 影子比对不能有这种副作用。</p>
     */
    private void cleanupShadowInstances(Long baseDefId, Long targetDefId, Set<Long> before) {
        Set<Long> created = shadowExistingInstIds(baseDefId, targetDefId);
        created.removeAll(before);
        if (created.isEmpty()) {
            return;
        }
        Set<String> deployments = new LinkedHashSet<>();
        for (Long instId : created) {
            WfInstance inst = instanceMapper.selectById(instId);
            if (inst != null && StringUtil.isNotBlank(inst.getTestDeploymentId())) {
                deployments.add(inst.getTestDeploymentId());
            }
            taskMapper.delete(Wrappers.<WfTask>lambdaQuery().eq(WfTask::getInstId, instId));
            approvalLogMapper.delete(Wrappers.<WfApprovalLog>lambdaQuery().eq(WfApprovalLog::getInstId, instId));
            snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery().eq(WfFormSnapshot::getInstId, instId));
        }
        testLogMapper.delete(Wrappers.<WfTestLog>lambdaQuery().in(WfTestLog::getInstId, created));
        instanceMapper.deleteBatchIds(created);
        // 测试部署：仅当已无其他测试实例引用时才卸载（避免影响用户手动测试中的实例）
        for (String dep : deployments) {
            Long refCount = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
                .eq(WfInstance::getIsTest, 1)
                .eq(WfInstance::getTestDeploymentId, dep));
            if (refCount != null && refCount > 0) {
                continue;
            }
            try {
                processService.deleteDeployment(dep);
            } catch (Exception e) {
                log.warn("[blade-workflow] 影子比对收尾：测试部署卸载失败. deploymentId={}, err={}",
                    dep, e.getMessage());
            }
        }
        log.info("[blade-workflow] 影子比对收尾完成. baseDefId={}, targetDefId={}, 清理实例数={}",
            baseDefId, targetDefId, created.size());
    }

    private Map<String, Object> shadowSummary(WfTestResultVO r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("testStatus", r.getTestStatus());
        m.put("reachedEnd", r.getReachedEnd());
        m.put("nodeTotal", r.getNodeTotal());
        m.put("nodePassed", r.getNodePassed());
        m.put("path", pathKeys(r));
        m.put("summary", r.getSummary());
        return m;
    }

    /** 走通的节点Key集合（status=1） */
    private Set<String> passedNodeKeys(WfTestResultVO r) {
        Set<String> set = new LinkedHashSet<>();
        if (r == null || r.getNodes() == null) {
            return set;
        }
        for (WfTestResultVO.TestNodeVO n : r.getNodes()) {
            if (n != null && n.getStatus() != null && n.getStatus() == 1 && n.getNodeKey() != null) {
                set.add(n.getNodeKey());
            }
        }
        return set;
    }

    /** 流转路径的节点Key序列（首段源节点 + 每段目标节点） */
    private List<String> pathKeys(WfTestResultVO r) {
        List<String> keys = new ArrayList<>();
        if (r == null || r.getPath() == null) {
            return keys;
        }
        for (WfTestResultVO.TestStepVO s : r.getPath()) {
            if (s == null) {
                continue;
            }
            if (keys.isEmpty() && s.getFromNodeKey() != null) {
                keys.add(s.getFromNodeKey());
            }
            if (s.getToNodeKey() != null) {
                keys.add(s.getToNodeKey());
            }
        }
        return keys;
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
    // 注意：此处**不能**加 @Transactional。
    // start 的编排意图是「部署/发起失败也要优雅终止、把错误日志落库」（见下方 catch）。
    // 若本方法开事务，则内部 instanceService.start / definitionService.deployForTest
    // （均为 REQUIRED）会加入同一事务；一旦它们抛异常，外层事务被标 rollback-only，
    // 而 catch 又吞掉异常去 saveTestLog 并提交 —— 提交时即抛出
    // 「Transaction rolled back because it has been marked as rollback-only」。
    // 去掉本注解后，内部调用各自跑独立事务，失败只回滚自身；catch 中的 saveTestLog
    // 以自动提交方式正常落库并返回错误结果，不再 500。
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
            // 引擎用「测试独立 key」启动：与 deployForTest 成对，避免测试部署顶替正式版本
            startDto.setEngineKey(def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX);
            // 定义级隔离（方案 §3.4 双重保险）：带上「测试版本」的流程定义ID，
            // 发起走 startProcessInstanceById —— 即便 __test key 因历史原因残留部署，
            // 只要按 ID 启动就不可能跑到正式版本
            startDto.setProcDefId(
                processService.latestProcDefId(def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX));
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
            // 落一条「进行中」的测试历史（C15）：让测试一发起就出现在「测试历史」中，
            // 并记录本次测试的操作人（代跑人）——原实现要等到归档才落库
            persistInteractiveLog(instanceMapper.selectById(instId), result);
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

    /**
     * 测试域守卫：只允许操作测试态实例（{@code is_test=1}），方案 §6.4 **C11**。
     *
     * <p><b>为什么必须在入口断言</b>：{@code /test/**} 各接口都直接接受 {@code instId}，
     * 而整个控制器只有角色门，拿到实例后并不校验它是不是测试实例。传入<b>正式实例</b> id 时：</p>
     * <ul>
     *   <li>{@code step}：{@code pendingTestTasks} 带 {@code .eq(isTest,1)} 查不到任务 →
     *       落入 {@code instanceService.advance(instId)}，**推进正式实例**（V11，最危险）；</li>
     *   <li>{@code state} / {@code todo}：把正式实例的待办与覆盖率当「测试结果」返回（信息泄漏）。</li>
     * </ul>
     */
    private void requireTestInst(WfInstance inst, String action) {
        if (inst == null) {
            throw new ServiceException("测试实例不存在（可能已被清理）");
        }
        if (inst.getIsTest() == null || inst.getIsTest() != 1) {
            log.warn("[blade-workflow] 越权拦截：/test/{} 目标非测试实例. instId={}, isTest={}, current={}",
                action, inst.getId(), inst.getIsTest(), SecureUtil.getUserId());
            throw new ServiceException("该实例不是测试实例，不允许通过测试接口操作");
        }
    }

    @Override
    public WfTestResultVO state(Long instId) {
        if (instId == null) {
            throw new ServiceException("测试实例ID不能为空");
        }
        WfInstance inst = instanceMapper.selectById(instId);
        requireTestInst(inst, "state");
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
        List<String> logLines = buildProgressLog(inst, nodeList, links);

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
        long curPending = todos.stream()
            .filter(x -> inst.getCurrentNodeKey() != null && inst.getCurrentNodeKey().equals(x.getNodeKey()))
            .count();
        result.setCurrentNodePendingCount((int) curPending);

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
        // 测试域守卫：不校验的话，传正式实例 id 会因 pendingTestTasks 查不到任务而落入
        // advance(instId) —— 直接推进生产实例（V11）
        requireTestInst(inst, "step");
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
            persistInteractiveLog(after, r);
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
                    // 开始节点（申请人）意见同样加统一前缀（C15）
                    submitLog.setOpinion(testOpinion(dto.getOpinion()));
                    approvalLogMapper.updateById(submitLog);
                }
            }
            WfInstance afterStart = instanceMapper.selectById(instId);
            WfTestResultVO r = state(instId);
            r.setSummary("已提交开始节点（申请人）表单，流程停在【" + r.getCurrentNodeName() + "】等待办理。");
            persistInteractiveLog(afterStart, r);
            return r;
        }

        // 代跑签字意见统一加「[测试]」前缀（C15）：该意见会以「节点接收人」名义落进 wf_approval_log，
        // 不加标识就与真实审批无从区分
        String opinion = testOpinion(dto.getOpinion());
        // 交互式测试：一次「提交」只办理一个人（一个待办）。
        // 会签节点＝同一节点有多条待办，需每个人各自点一次提交；或签＝首条办结即关闭其余并推进；
        // 普通单人节点＝只有一条待办，一次办结。这样与 ecology「会签需逐人审批」一致，
        // 不会「一点提交把会签所有人都办掉」。自动测试（runScenario）仍按原逻辑整节点一次走完。
        boolean approvedOne = false;
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
            approvedOne = true;
            // 只办当前这一人：剩下同节点的待办（会签其余办理人）保持待办，等下一轮「提交」再办
            break;
        }
        if (!approvedOne) {
            // 兜底：当前节点已无可办待办（极少见，如全部被或签关闭），回退到最新状态
            WfInstance afterFallback = instanceMapper.selectById(instId);
            WfTestResultVO r = state(instId);
            persistInteractiveLog(afterFallback, r);
            return r;
        }

        WfInstance after = instanceMapper.selectById(instId);
        WfTestResultVO r = state(instId);
        persistInteractiveLog(after, r);
        return r;
    }

    @Override
    public List<WfTaskVO> myTodo() {
        List<WfTaskVO> vos = new ArrayList<>();
        Long me = SecureUtil.getUserId();
        if (me == null) {
            return vos;
        }
        // 「我的测试待办」= 当前登录人在**测试实例**上的待办（真人模式的数据源，方案 §6.4 C12）。
        // 与 todo(instId) 的区别：那个是「某测试实例的全部待办」（管理面代跑用来定位 taskId），
        // 这个按登录人过滤 —— 供节点操作者本人从测试入口看到属于自己的测试单并办理。
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getAssignee, me)
            .eq(WfTask::getIsTest, 1)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO)
            .orderByDesc(WfTask::getCreateTime));
        for (WfTask t : tasks) {
            WfInstance inst = instanceMapper.selectById(t.getInstId());
            if (inst == null) {
                continue;
            }
            List<WfProcessNode> nodeList = nodeMapper.selectList(
                Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, inst.getDefId()));
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
            vo.setIsTest(t.getIsTest());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public WfTestResultVO approve(WfTestStepDTO dto) {
        if (dto == null || dto.getInstId() == null) {
            throw new ServiceException("测试实例ID不能为空");
        }
        WfInstance inst = instanceMapper.selectById(dto.getInstId());
        // C11：目标必须是测试实例（防借测试入口推进正式实例）
        requireTestInst(inst, "approve");
        // C12 记录级鉴权：真人模式下必须「本人是该测试实例当前待办的执行人」；
        // 管理员不受限（保留管理面代跑能力 —— 代跑走 /test/step，两者提交语义一致）
        if (!WfAuthUtil.isAdmin()) {
            Long me = SecureUtil.getUserId();
            boolean mine = pendingTestTasks(dto.getInstId()).stream()
                .anyMatch(t -> me != null && me.equals(t.getAssignee()));
            if (!mine) {
                log.warn("[blade-workflow] 越权拦截：/test/approve 非本人待办. instId={}, current={}",
                    dto.getInstId(), me);
                throw new WfAccessDeniedException("该测试待办不属于当前用户，无法提交");
            }
        }
        // 提交语义与代跑完全一致：step 内部按「待办接收人」身份办理，真人模式下接收人即本人
        return step(dto);
    }

    @Override
    public List<WfTaskVO> todo(Long instId) {
        List<WfTaskVO> vos = new ArrayList<>();
        if (instId == null) {
            return vos;
        }
        WfInstance inst = instanceMapper.selectById(instId);
        // 测试域守卫：避免用正式实例 id 读到正式待办（信息泄漏，V11）
        requireTestInst(inst, "todo");
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

    /** 操作者标签：「姓名（ID）」；解析失败或 ID 非法时返回 null（由调用方降级为「系统」） */
    private String opLabel(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        String name = resolveName(id);
        if (name != null && !name.equals(String.valueOf(id))) {
            return name + "（" + id + "）";
        }
        return "（" + id + "）";
    }

    /** 把用户ID集合拼成「姓名（ID）」并用「，」连接；空时返回「系统」 */
    private String joinOpNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "系统";
        }
        List<String> parts = new ArrayList<>();
        for (Long id : ids) {
            String s = opLabel(id);
            if (s != null) {
                parts.add(s);
            }
        }
        return parts.isEmpty() ? "系统" : String.join("，", parts);
    }

    /** 取出口中文名：优先 conditionCn，缺失时用目标节点名兜底 */
    private String findCondCn(List<WfNodeLink> links, List<WfProcessNode> nodeList, String fromKey, String toKey) {
        if (links != null) {
            for (WfNodeLink l : links) {
                if (fromKey != null && fromKey.equals(l.getFromNodeKey())
                        && toKey != null && toKey.equals(l.getToNodeKey())) {
                    if (l.getConditionCn() != null && !l.getConditionCn().isBlank()) {
                        return l.getConditionCn();
                    }
                }
            }
        }
        return nodeName(nodeList, toKey);
    }

    /** 是否为创建（开始）节点：nodeType == 0 */
    private boolean isCreateNode(List<WfProcessNode> nodeList, String nodeKey) {
        if (nodeList == null || nodeKey == null) {
            return false;
        }
        for (WfProcessNode n : nodeList) {
            if (nodeKey.equals(n.getNodeKey()) && n.getNodeType() != null && n.getNodeType() == 0) {
                return true;
            }
        }
        return false;
    }

    /** 追加单个节点的「到达」叙述：到达节点 / 节点前附加操作 / 开始自动测试(仅首节点) */
    private void appendArrival(List<String> lines, SimpleDateFormat fmt, Date ts,
            String nodeName, List<Long> operators, boolean[] started) {
        String tsStr = fmt.format(ts == null ? new Date() : ts);
        String opDisplay = joinOpNames(operators);
        lines.add(tsStr + " 到达节点\"" + nodeName + "\"，操作者\"" + opDisplay + "\"");
        lines.add(tsStr + " 执行节点\"" + nodeName + "\"的节点前附加操作");
        if (!started[0]) {
            lines.add(tsStr + " 开始自动测试");
            started[0] = true;
        }
    }

    /**
     * 追加单个节点的「办理」叙述：逐操作者提交 + 节点整体通过。
     *
     * <p><b>会签 / 并行节点</b>：{@code operators} 含多人，每人各生成一条「操作者"姓名（ID）"提交」，
     * 这样测试日志能清楚列出<b>每个会签审批人的审批记录</b>（不再只显示第一个）。
     * 单人节点即一条；节点整体通过（会签=最后一人提交后节点才通过）放在所有提交记录之后。</p>
     */
    private void appendNodeSubmit(List<String> lines, SimpleDateFormat fmt, Date ts,
            String nodeName, List<Long> operators) {
        String tsStr = fmt.format(ts == null ? new Date() : ts);
        if (operators != null) {
            for (Long op : operators) {
                String who = opLabel(op);
                if (who != null) {
                    lines.add(tsStr + " 操作者\"" + who + "\"提交");
                }
            }
        }
        lines.add(tsStr + " 通过节点\"" + nodeName + "\"");
    }

    /**
     * 由审批日志还原「测试进度日志」为可读流转时间线（对齐 formmode 测试日志观感）：
     * 到达节点 / 节点前附加操作 / 开始自动测试 / 操作者提交 / 通过节点 / 执行出口 / 生成流程编号 / 流程已结束。
     * 真实运行中同一节点可能产生多条审批记录（会签 / 重复提交），这里按节点去重、聚合操作者，保证时间线清晰易读。
     */
    private List<String> buildProgressLog(WfInstance inst, List<WfProcessNode> nodeList, List<WfNodeLink> links) {
        List<String> lines = new ArrayList<>();
        if (inst == null || inst.getId() == null) {
            return lines;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<WfApprovalLog> logs = approvalLogMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, inst.getId())
            .orderByAsc(WfApprovalLog::getOperateTime)
            .orderByAsc(WfApprovalLog::getId));
        // 按出现顺序收集去重节点 + 每节点聚合操作者
        LinkedHashMap<String, List<Long>> byNode = new LinkedHashMap<>();
        List<String> orderedKeys = new ArrayList<>();
        for (WfApprovalLog l : logs) {
            String key = l.getNodeKey();
            if (key == null) {
                continue;
            }
            byNode.computeIfAbsent(key, k -> new ArrayList<>());
            if (l.getOperator() != null && !byNode.get(key).contains(l.getOperator())) {
                byNode.get(key).add(l.getOperator());
            }
            if (!orderedKeys.contains(key)) {
                orderedKeys.add(key);
            }
        }
        boolean[] started = { false };
        int prevIdx = -1;
        for (int i = 0; i < orderedKeys.size(); i++) {
            String nodeKey = orderedKeys.get(i);
            String nm = nodeName(nodeList, nodeKey);
            Date ts = null;
            for (WfApprovalLog l : logs) {
                if (nodeKey.equals(l.getNodeKey()) && l.getOperateTime() != null) {
                    ts = l.getOperateTime();
                    break;
                }
            }
            // 执行出口（离开上一节点）
            if (prevIdx >= 0) {
                String prevKey = orderedKeys.get(prevIdx);
                String condCn = findCondCn(links, nodeList, prevKey, nodeKey);
                String tsStr = fmt.format(ts == null ? new Date() : ts);
                lines.add(tsStr + " 执行出口\"" + condCn + "\"");
                if (prevIdx == 0) {
                    lines.add(tsStr + " 出口\"" + condCn + "\"生成流程编号");
                }
            }
            // 到达节点：列出该节点全部办理人（会签/并行取完整名单，来源=测试态待办，避免漏列任一会签人）
            List<Long> fullOps = new ArrayList<>();
            for (WfTestResultVO.TestOperatorVO v : toOperatorVos(inst.getId(), nodeKey)) {
                fullOps.add(v.getUserId());
            }
            appendArrival(lines, fmt, ts, nm, fullOps, started);
            // 逐操作者提交：会签/并行每人一条审批记录，单人节点即一条（来源=审批日志实际提交人）
            appendNodeSubmit(lines, fmt, ts, nm, byNode.get(nodeKey));
            prevIdx = i;
        }
        if (inst.getStatus() != null && WfInstance.STATUS_RUNNING != inst.getStatus()) {
            lines.add(fmt.format(inst.getEndTime() == null ? new Date() : inst.getEndTime())
                + " 流程已结束（" + instanceStatusCn(inst.getStatus()) + "）");
        }
        return lines;
    }

    /**
     * 交互式测试的「测试历史」落库 / 刷新（方案 C15：逐步可追溯 + 留痕自证）。
     *
     * <p><b>与原实现的差别</b>：原来只在实例跑到终态时落一条、且「同实例已存在就跳过」，
     * 于是既看不到进行中的测试进展，也无法回答「谁在哪一步点的提交」。现改为<b>每次提交刷新同一条</b>：
     * 进行中记为 {@link WfTestLog#TEST_RUNNING}，结束后刷新为最终结论；并在正文末尾显式写出
     * 「本次测试操作人（代跑人）」。</p>
     *
     * <p><b>为什么必须显式写执行人</b>：{@code wf_task} 无 {@code operate_user} 列、Blade 的审计列
     * 自动填充在本项目未生效（实测 {@code create_user} 全 NULL）、Flowable 历史的
     * {@code ASSIGNEE_}/{@code COMPLETED_BY_} 也全 NULL；而代跑又是「以节点接收人身份」提交的，
     * 不写这一行就无从追溯是谁在测试面板点的提交。</p>
     */
    private void persistInteractiveLog(WfInstance inst, WfTestResultVO result) {
        if (inst == null || inst.getId() == null || inst.getStatus() == null) {
            return;
        }
        try {
            WfProcessDefinition def = defMapper.selectById(inst.getDefId());
            if (def == null) {
                return;
            }
            WfTestRunDTO dto = new WfTestRunDTO();
            dto.setDefId(def.getId());
            dto.setTestUserId(inst.getStarter());
            List<String> lines = result.getLog() == null
                ? new ArrayList<>() : new ArrayList<>(result.getLog());
            lines.add("本次测试操作人（代跑人）=" + resolveName(SecureUtil.getUserId())
                + "；测试发起人=" + resolveName(inst.getStarter())
                + "；各节点签字意见统一加「" + TEST_OPINION_PREFIX.trim() + "」前缀以区别真实审批");
            saveTestLog(def, dto, result, lines, inst.getId());
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

    /**
     * 会签/或签/依次「引擎多实例」门禁的 <b>wf_task 侧</b>集成验证（对应《下沉迁移方案》§6.5）。
     *
     * <p>程序化造三条「单 MI 节点」流程定义（nodeType=1 + 不同 signOrder），经与正式部署同口径的
     * {@code deployForTest}（开关 {@code blade.workflow.engine-multi-instance.enabled=true} 时注入多实例）
     * 部署，真实发起测试实例，用 {@code IWfTaskService#autoApprove(system=true)} 逐人驱动门禁，
     * 断言 wf_task 行为：</p>
     * <ul>
     *   <li>会签：进入即有 3 条引擎任务 / 3 条 wf_task；逐人办，pending 序列 [2,1,0]，末办才结束；</li>
     *   <li>或签：进入即 3 条引擎任务 / 3 条 wf_task；首办即过，剩余兄弟待办被对账关闭，pending→0 且结束；</li>
     *   <li>依次：进入仅 1 条引擎任务（串行）；逐人办，pending 序列 [1,1,0]，末办才结束。</li>
     * </ul>
     *
     * <p>⚠️ 前置：{@code blade.workflow.engine-multi-instance.enabled} 必须开启，否则注入不发生，
     * 断言（尤其进入时引擎任务数、或签兄弟待办对账）会暴露「未下沉」。每条定义/实例/部署在方法内自清理。</p>
     *
     * @param formId 复用既有表单ID（nullable；测试态不建业务行，仅用于满足入参契约）
     * @return 各模式断言结果与总结论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> miCounterSignScenario(Long formId) {
        Map<String, Object> result = new LinkedHashMap<>();
        int[] signOrders = {WfNodeSettingsUtil.SIGN_AND, WfNodeSettingsUtil.SIGN_OR, WfNodeSettingsUtil.SIGN_SEQ};
        String[] labels = {"会签(all)", "或签(or)", "依次(seq)"};
        List<Map<String, Object>> details = new ArrayList<>();
        boolean allPassed = true;
        for (int i = 0; i < signOrders.length; i++) {
            Map<String, Object> d = runMiMode(labels[i], signOrders[i], formId);
            details.add(d);
            if (!(Boolean) d.get("passed")) {
                allPassed = false;
            }
        }
        result.put("allPassed", allPassed);
        result.put("modes", details);
        return result;
    }

    /**
     * 单模式 MI 场景：造定义→部署→发起→逐人驱动→断言→清理。
     * 异常被捕获并记为未通过，不污染其它模式（方法级自清理，见 finally）。
     */
    private Map<String, Object> runMiMode(String label, int signOrder, Long formId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("mode", label);
        d.put("signOrder", signOrder);
        String procKey = "miScenario_" + System.nanoTime() + "_" + signOrder;
        Long defId = null;
        Long instId = null;
        String deploymentId = null;
        List<Long> nodeIds = new ArrayList<>();
        try {
            // 1) 程序化造「单 MI 节点」流程定义（BPMN + 节点 + 操作者）
            WfProcessDefinition def = new WfProcessDefinition();
            def.setProcKey(procKey);
            def.setName("MI场景-" + label);
            def.setBpmnXml(miBpmn(procKey));
            def.setVersion(1);
            def.setStatus(0);
            def.setFormId(formId);
            defMapper.insert(def);
            defId = def.getId();

            WfProcessNode startNode = new WfProcessNode();
            startNode.setDefId(defId);
            startNode.setNodeKey("startEvent");
            startNode.setNodeName("开始");
            startNode.setNodeType(0);
            startNode.setSortOrder(0);
            nodeMapper.insert(startNode);
            nodeIds.add(startNode.getId());

            WfProcessNode miNode = new WfProcessNode();
            miNode.setDefId(defId);
            miNode.setNodeKey("miNode");
            miNode.setNodeName("会签节点");
            miNode.setNodeType(1);
            miNode.setSignOrder(signOrder);
            miNode.setSortOrder(1);
            nodeMapper.insert(miNode);
            nodeIds.add(miNode.getId());

            for (String uid : new String[] {"1001", "1002", "1003"}) {
                WfNodeOperator op = new WfNodeOperator();
                op.setNodeId(miNode.getId());
                op.setOpType(3); // 人员
                op.setObjId(uid);
                op.setSignOrder(null); // 由节点 signOrder 决定
                operatorMapper.insert(op);
            }

            // 2) 与正式部署同口径的测试部署（开关开启时注入多实例）
            deploymentId = definitionService.deployForTest(defId);

            // 3) 真实发起测试实例（测试态：不建业务行、is_test=1）
            StartProcessDTO startDto = new StartProcessDTO();
            startDto.setDefId(defId);
            startDto.setFormId(formId);
            startDto.setStarter(1L);
            startDto.setTestFlag(true);
            startDto.setTestDeploymentId(deploymentId);
            startDto.setEngineKey(procKey + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX);
            startDto.setProcDefId(processService.latestProcDefId(
                procKey + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX));
            startDto.setTitle("【测试】MI场景-" + label);
            startDto.setDataId(IdWorker.getId());
            instId = instanceService.start(startDto);

            WfInstance inst = instanceMapper.selectById(instId);

            // 4) 进入时断言：引擎多实例是否已按人拆分（并行=3 / 串行=1）
            long initialEngineTasks = processService.currentTasks(inst.getEngineInstId()).stream()
                .filter(t -> "miNode".equals(t.getTaskDefinitionKey())).count();
            long expectedInitial = (signOrder == WfNodeSettingsUtil.SIGN_SEQ) ? 1 : 3;
            d.put("initialEngineTasks", initialEngineTasks);
            d.put("expectedInitialEngineTasks", expectedInitial);

            // 5) 逐人驱动门禁（system=true 绕过鉴权与菜单校验），记录每步后 pending / 是否结束
            List<Long> pendingAfter = new ArrayList<>();
            List<Boolean> endedAfter = new ArrayList<>();
            int guard = 0;
            while (guard++ < 20) {
                List<WfTask> todos = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
                    .eq(WfTask::getInstId, instId)
                    .eq(WfTask::getNodeKey, "miNode")
                    .eq(WfTask::getStatus, WfTask.STATUS_TODO));
                if (todos.isEmpty()) {
                    break;
                }
                WfTask t = todos.get(0);
                taskService.autoApprove(t.getId(), "同意", new LinkedHashMap<>(), t.getAssignee());
                long pending = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
                    .eq(WfTask::getInstId, instId)
                    .eq(WfTask::getNodeKey, "miNode")
                    .eq(WfTask::getStatus, WfTask.STATUS_TODO));
                pendingAfter.add(pending);
                HistoricProcessInstance hp = processService.historicProcess(inst.getEngineInstId());
                endedAfter.add(hp != null && hp.getEndTime() != null);
            }

            // 6) 断言（编码正确预期；若当前实现缺对账等，测试会如实暴露）
            List<Long> expectedPending;
            if (signOrder == WfNodeSettingsUtil.SIGN_AND) {
                expectedPending = List.of(2L, 1L, 0L);
            } else if (signOrder == WfNodeSettingsUtil.SIGN_OR) {
                expectedPending = List.of(0L);
            } else {
                expectedPending = List.of(1L, 1L, 0L);
            }
            boolean ended = !endedAfter.isEmpty()
                && Boolean.TRUE.equals(endedAfter.get(endedAfter.size() - 1));
            boolean ok = (initialEngineTasks == expectedInitial)
                && pendingAfter.equals(expectedPending) && ended;
            d.put("pendingAfter", pendingAfter);
            d.put("expectedPendingAfter", expectedPending);
            d.put("endedAfter", endedAfter);
            d.put("passed", ok);
            if (!ok) {
                d.put("failReason", "initialEngineTasks=" + initialEngineTasks + "/" + expectedInitial
                    + ", pendingAfter=" + pendingAfter + ", expected=" + expectedPending);
            }
        } catch (Exception e) {
            d.put("passed", false);
            d.put("error", e.getMessage());
            log.warn("[blade-workflow][MI场景] 模式[{}]执行异常: {}", label, e.getMessage());
        } finally {
            // 引擎部署在独立事务，必须显式卸载；业务行在本 @Transactional 内删除（异常时随回滚撤销）
            if (deploymentId != null) {
                try {
                    processService.deleteDeployment(deploymentId);
                } catch (Exception ignore) {
                    log.warn("[blade-workflow][MI场景] 卸载部署失败（可能已结束）: {}", deploymentId);
                }
            }
            if (instId != null) {
                taskMapper.delete(Wrappers.<WfTask>lambdaQuery().eq(WfTask::getInstId, instId));
                approvalLogMapper.delete(Wrappers.<WfApprovalLog>lambdaQuery().eq(WfApprovalLog::getInstId, instId));
                snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery().eq(WfFormSnapshot::getInstId, instId));
                instanceMapper.deleteById(instId);
            }
            if (defId != null) {
                if (!nodeIds.isEmpty()) {
                    operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
                        .in(WfNodeOperator::getNodeId, nodeIds));
                }
                nodeMapper.delete(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
                defMapper.deleteById(defId);
            }
        }
        return d;
    }

    /** 单 MI 节点 BPMN（process id 会被 deployForTest 改名为测试 key）；assignee 占位，部署期被多实例元素变量覆盖。 */
    private static String miBpmn(String procKey) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://springblade.workflow" id="def_%s">
                <process id="%s" name="MI Scenario" isExecutable="true">
                    <startEvent id="startEvent" name="开始"/>
                    <userTask id="miNode" name="会签节点" flowable:assignee="placeholder"/>
                    <endEvent id="endEvent" name="结束"/>
                    <sequenceFlow id="f1" sourceRef="startEvent" targetRef="miNode"/>
                    <sequenceFlow id="f2" sourceRef="miNode" targetRef="endEvent"/>
                </process>
            </definitions>
            """.formatted(procKey, procKey);
    }

    private String buildSummary(int passed, int total, boolean reachedEnd, boolean aborted) {        if (aborted) {
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
