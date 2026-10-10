package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfNodeTimeoutMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.config.WfRetirementProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 节点超时规则服务实现。
 *
 * <p>多条规则按 {@code seq} 升序；{@code resolveDueTime} 取最早截止（写入待办 {@code due_time}），
 * {@code firstOverdue} 取第一条已到期规则由 {@link WfTimeoutJob} 执行动作。</p>
 *
 * <p>兼容：节点没有任何 {@code wf_node_timeout} 规则时，回退旧 {@code settings.timeout.hours}（单条）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfTimeoutServiceImpl implements IWfTimeoutService {

    private final WfNodeTimeoutMapper timeoutMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfTaskMapper taskMapper;
    private final WfProcessDefinitionMapper definitionMapper;
    private final IWfTaskService taskService;
    /** T11.2：超时提醒同步推送统一消息中心（失败仅记日志，不影响超时动作） */
    private final INoticeClient noticeClient;
    /** 任务业务列双写收口器（方案 A1） */
    private final org.springblade.workflow.service.helper.WfTaskActWriter taskActWriter;
    private final IWfInstanceService instanceService;
    private final IFormmodeClient formmodeClient;
    /** P3-5：节点超时规则改读 BPMN {@code wf:} 扩展（替代 wf_node_timeout）。默认关，运行时回归后开启。
     * 注意：开启前须确保 BPMN 已携带完整超时规则（经回填或前端 saveBpmn 收敛）；本开关同时影响
     * 运行期 {@code resolveDueTime}/{@code firstOverdue} 的取数，使超时成为「单一事实源」。 */
    private final WfBpmnExtensionReader bpmnReader;

    @Value("${blade.workflow.timeout-from-bpmn.enabled:false}")
    private boolean timeoutFromBpmn;

    @Autowired
    private WfRetirementProperties retirement = new WfRetirementProperties();

    @Override
    public List<WfNodeTimeout> listEnabled(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return new ArrayList<>();
        }
        if (timeoutFromBpmn) {
            // 优先读 BPMN；BPMN 无数据（草稿/缺扩展/缺 timeout 子元素）回退 wf_node_timeout 表，避免静默降级。
            List<WfNodeTimeout> bpmn = timeoutsFromBpmn(defId, nodeKey);
            if (!bpmn.isEmpty()) {
                return bpmn;
            }
        }
        // T-14 退役：主开关开启时禁用 wf_node_timeout 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
            return timeoutMapper.selectList(Wrappers.<WfNodeTimeout>lambdaQuery()
                .eq(WfNodeTimeout::getDefId, defId)
                .eq(WfNodeTimeout::getNodeKey, nodeKey)
                .eq(WfNodeTimeout::getEnabled, 1)
                .orderByAsc(WfNodeTimeout::getSeq));
        }
        return new ArrayList<>();
    }

    /**
     * P3-5：从 BPMN {@code wf:timeout} 读取已启用规则。BPMN 超时 schema 与 {@code wf_node_timeout}
     * 字段对齐（回填已结构化写入），转换逻辑见 {@link WfBpmnExtensionReader#toNodeTimeouts}（纯函数，可单测回归）。
     */
    private List<WfNodeTimeout> timeoutsFromBpmn(Long defId, String nodeKey) {
        return WfBpmnExtensionReader.toNodeTimeouts(defId, nodeKey, bpmnReader.timeouts(defId, nodeKey));
    }

    @Override
    public void saveRules(Long defId, String nodeKey, List<WfNodeTimeout> rules) {
        if (defId == null || nodeKey == null) {
            return;
        }
        timeoutMapper.delete(Wrappers.<WfNodeTimeout>lambdaQuery()
            .eq(WfNodeTimeout::getDefId, defId)
            .eq(WfNodeTimeout::getNodeKey, nodeKey));
        if (rules == null || rules.isEmpty()) {
            return;
        }
        int seq = 0;
        for (WfNodeTimeout r : rules) {
            r.setId(null);
            r.setDefId(defId);
            r.setNodeKey(nodeKey);
            if (r.getSeq() == null) {
                r.setSeq(seq++);
            }
            if (r.getEnabled() == null) {
                r.setEnabled(1);
            }
            r.setCreateTime(new Date());
            timeoutMapper.insert(r);
        }
    }

    @Override
    public Date resolveDueTime(Long defId, String nodeKey, WfTask task, WfInstance inst) {
        List<WfNodeTimeout> rules = listEnabled(defId, nodeKey);
        if (rules.isEmpty()) {
            // 兼容旧单条配置：BPMN 源优先读 wf:node.extJson（T-10 B4 #8），wf_ 源回退 nodeMapper
            WfProcessNode node = null;
            if (timeoutFromBpmn) {
                try {
                    node = bpmnReader.node(defId, nodeKey);
                } catch (Exception e) {
                    log.warn("[WfTimeoutServiceImpl] 解析旧单条超时(BPMN)失败，忽略. defId={}, nodeKey={}", defId, nodeKey, e);
                }
            } else {
                node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                    .eq(WfProcessNode::getDefId, defId)
                    .eq(WfProcessNode::getNodeKey, nodeKey)
                    .last("LIMIT 1"));
            }
            int hours = WfNodeSettingsUtil.timeoutHours(node);
            if (hours <= 0) {
                return null;
            }
            Date start = task != null && task.getReceiveTime() != null ? task.getReceiveTime() : new Date();
            return new Date(start.getTime() + hours * 60L * 60L * 1000L);
        }
        Date earliest = null;
        for (WfNodeTimeout r : rules) {
            Date due = computeDue(r, task, inst);
            if (due != null && (earliest == null || due.before(earliest))) {
                earliest = due;
            }
        }
        return earliest;
    }

    @Override
    public Date computeDue(WfNodeTimeout rule, WfTask task, WfInstance inst) {
        Date start;
        if (Integer.valueOf(2).equals(rule.getStartType()) && rule.getStartField() != null) {
            start = formDate(inst, rule.getStartField());
            if (start == null) {
                return null;
            }
        } else {
            start = task != null && task.getReceiveTime() != null ? task.getReceiveTime() : new Date();
        }
        int endType = rule.getEndType() == null ? 1 : rule.getEndType();
        if (endType == 2) {
            // 固定时刻：每日 HH:mm，取 start 之后的首个
            return rule.getEndFixedTime() == null ? null : nextFixedTime(start, rule.getEndFixedTime());
        }
        if (endType == 3) {
            return rule.getEndField() == null ? null : formDate(inst, rule.getEndField());
        }
        // 相对：起算 + 时长
        int min = rule.getDurationMin() == null ? 0 : rule.getDurationMin();
        if (min <= 0) {
            return null;
        }
        return new Date(start.getTime() + min * 60L * 1000L);
    }

    @Override
    public WfNodeTimeout firstOverdue(Long defId, String nodeKey, WfTask task, WfInstance inst, Date now) {
        List<WfNodeTimeout> rules = listEnabled(defId, nodeKey);
        return rules.stream()
            .map(r -> new DueHolder(r, computeDue(r, task, inst)))
            .filter(h -> h.due != null && !h.due.after(now))
            .min(Comparator.comparing(h -> h.due))
            .map(h -> h.rule)
            .orElse(null);
    }

    @Override
    public void fire(WfNodeTimeout rule, WfTask task, WfInstance inst) {
        if (rule == null || task == null || inst == null) {
            return;
        }
        // 测试态：任何超时动作都不执行（方案 §6.4 C5 / V5、V6、S6、S7）。
        // WfTimeoutJob 已在**扫描阶段**排除 is_test=1，这里是纵深防御 ——
        // 防止将来扫描条件变化、或其他调用路径把测试任务送进来，
        // 触发「自动通过 / 转办给真人 / 催办留痕」把测试动作外溢到生产。
        if (inst.getIsTest() != null && inst.getIsTest() == 1) {
            return;
        }
        String way = rule.getActionWay() == null ? "autoApprove" : rule.getActionWay();

        // 主动作执行；成功与否决定后续是否置位 / 提醒（修复 R4：失败不得误标记"已处理"）。
        boolean primaryOk;
        try {
            switch (way) {
                case "assign":
                    primaryOk = doAssign(task, rule);
                    break;
                case "remind":
                    // 仅提醒类动作：提醒本身即主动作
                    primaryOk = doRemind(task, inst, rule, way);
                    break;
                case "forward":
                case "autoApprove":
                default:
                    String opinion = (rule.getOpinion() == null || rule.getOpinion().isBlank())
                        ? "超时自动通过" : rule.getOpinion();
                    taskService.autoApprove(task.getId(), opinion);
                    primaryOk = true;
                    break;
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 超时主动作执行失败. taskId={}, way={}", task.getId(), way, e);
            primaryOk = false;
        }

        if (!primaryOk) {
            // 失败：不置位、不重复提醒，保留 timeout_handled=0 由下一扫描周期重试，
            // 避免 R4「动作失败却被误标记已处理」导致超时动作永久丢失。
            // 持久化重试计数 / 死信（防无限重试风暴）由 P3（T-8）迁移到 ACT_RU_TASK 后补（见 D9）。
            log.warn("[blade-workflow] 超时动作未成功，保留待重试. taskId={}, way={}", task.getId(), way);
            return;
        }

        // 主动作成功：非 remind 类动作仍按配置对处理人做提醒（best-effort，失败不影响已完成的动作）。
        if (!"remind".equals(way)) {
            try {
                doRemind(task, inst, rule, way);
            } catch (Exception e) {
                log.warn("[blade-workflow] 超时提醒记录失败. taskId={}", task.getId(), e);
            }
        }

        // 仅成功才标记已执行，避免重复触发
        task.setTimeoutHandled(1);
        taskMapper.updateById(task);
        // 超时已执行标记同步到 ACT_*（TIMEOUT_HANDLED_ 为 wf_task 独有列）
        taskActWriter.sync(task);
    }

    /** 超时转办：将待办办理人改为指定操作者（取首个；多操作者仅记首人，后续可扩展） */
    private boolean doAssign(WfTask task, WfNodeTimeout rule) {
        if (rule.getOperatorIds() == null || rule.getOperatorIds().isBlank()) {
            return false;
        }
        String[] ids = rule.getOperatorIds().split(",");
        for (String s : ids) {
            try {
                Long uid = Long.valueOf(s.trim());
                if (uid != null && uid > 0) {
                    task.setAssignee(uid);
                    taskMapper.updateById(task);
                    log.info("[blade-workflow] 超时转办. taskId={}, to={}", task.getId(), uid);
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // 跳过非法 ID
            }
        }
        return false;
    }

    /** 超时提醒：节点处理人本人（remindBeforeOperator）+ 指定人员，按 remindTypes 记流转意见留痕 */
    private boolean doRemind(WfTask task, WfInstance inst, WfNodeTimeout rule, String way) {
        if (rule.getRemindTypes() == null || rule.getRemindTypes().isBlank()) {
            // 无提醒配置视为成功（remind 类动作无需副作用）
            return true;
        }
        List<Long> recipients = new ArrayList<>();
        if (Integer.valueOf(1).equals(rule.getRemindBeforeOperator())
            && task.getAssignee() != null && task.getAssignee() > 0) {
            recipients.add(task.getAssignee());
        }
        if (rule.getRemindPersons() != null && !rule.getRemindPersons().isBlank()) {
            for (String s : rule.getRemindPersons().split(",")) {
                try {
                    Long id = Long.valueOf(s.trim());
                    if (id > 0 && !recipients.contains(id)) {
                        recipients.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // 跳过
                }
            }
        }
        if (recipients.isEmpty()) {
            recipients.add(0L);
        }
        String msg = buildRemindMsg(rule, way);
        for (Long who : recipients) {
            instanceService.recordLog(inst.getId(), task.getNodeKey(), who,
                WfApprovalLog.LOG_COMMENT, msg);
        }
        // T11.2：超时提醒同步推送统一消息中心（铃铛红点 + 流程通知卡片）。
        // fire() 以 timeout_handled=1 保证单次触发，此处天然幂等；失败仅记日志。
        pushNotice(inst, task, msg, recipients);
        return true;
    }

    /** 超时提醒推送消息中心：接收人剔除系统占位（0），尽力而为、失败不影响超时动作 */
    private void pushNotice(WfInstance inst, WfTask task, String msg, List<Long> recipients) {
        List<Long> receivers = recipients.stream()
            .filter(id -> id != null && id > 0)
            .toList();
        if (receivers.isEmpty()) {
            return;
        }
        try {
            NoticeSendDTO dto = new NoticeSendDTO();
            dto.setTenantId(inst.getTenantId());
            dto.setUserIds(receivers);
            dto.setContentType(4);
            dto.setContent(buildNoticeContent(inst, task) + "：" + msg);
            dto.setBizRefType("WF_TASK");
            dto.setBizRefId(task.getEngineTaskId());
            dto.setFlowKey(procKeyOf(inst));
            noticeClient.sendToUsers(dto);
        } catch (Exception e) {
            log.warn("[WfTimeoutServiceImpl] 超时提醒推送消息中心失败（忽略）. taskId={}, {}",
                task.getId(), e.getMessage());
        }
    }

    /** 通知文案：流程《标题》的「节点名」已超时 */
    private String buildNoticeContent(WfInstance inst, WfTask task) {
        String nodeName = task.getNodeKey();
        if (inst.getDefId() != null && task.getNodeKey() != null) {
            WfProcessNode node = nodeMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<WfProcessNode>lambdaQuery()
                    .eq(WfProcessNode::getDefId, inst.getDefId())
                    .eq(WfProcessNode::getNodeKey, task.getNodeKey())
                    .last("LIMIT 1"));
            if (node != null && node.getNodeName() != null && !node.getNodeName().isBlank()) {
                nodeName = node.getNodeName();
            }
        }
        return String.format("流程《%s》的「%s」已超时",
            inst.getTitle() == null || inst.getTitle().isBlank() ? "未命名流程" : inst.getTitle(),
            nodeName);
    }

    /** 实例 → 流程定义 key（proc_key），供用户级提醒配置过滤；查不到返回 null（不过滤） */
    private String procKeyOf(WfInstance inst) {
        if (inst.getDefId() == null) {
            return null;
        }
        WfProcessDefinition def = definitionMapper.selectById(inst.getDefId());
        return def == null ? null : def.getProcKey();
    }

    private String buildRemindMsg(WfNodeTimeout rule, String way) {
        StringBuilder sb = new StringBuilder("流程节点已超时");
        String types = rule.getRemindTypes();
        List<String> ch = new ArrayList<>();
        if (types.contains("sys")) ch.add("流程提醒");
        if (types.contains("ml")) ch.add("短信");
        if (types.contains("sm")) ch.add("邮件");
        if (!ch.isEmpty()) {
            sb.append("（").append(String.join("/", ch)).append("）");
        }
        if ("autoApprove".equals(rule.getActionWay()) || "forward".equals(rule.getActionWay())) {
            sb.append("，已自动通过");
        } else if ("assign".equals(rule.getActionWay())) {
            sb.append("，已转办");
        }
        return sb.toString();
    }

    /** 取表单时间字段（yyyy-MM-dd[ HH:mm[:ss]]）作为 Date；失败返回 null */
    private Date formDate(WfInstance inst, String fieldName) {
        if (inst == null || inst.getDataId() == null || fieldName == null) {
            return null;
        }
        try {
            R<FormDataVO> r = formmodeClient.getFormDataById(inst.getDataId());
            if (r == null || !r.isSuccess() || r.getData() == null) {
                return null;
            }
            Object v = r.getData().getFieldValues() == null ? null : r.getData().getFieldValues().get(fieldName);
            if (v == null) {
                return null;
            }
            return parseDate(String.valueOf(v).trim());
        } catch (Exception e) {
            log.warn("[blade-workflow] 读取表单时间字段失败. dataId={}, field={}", inst.getDataId(), fieldName, e);
            return null;
        }
    }

    private Date parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p).parse(s);
            } catch (ParseException ignored) {
                // 尝试下一个
            }
        }
        // 可能是时间戳（毫秒/秒）
        try {
            long t = Long.parseLong(s.trim());
            if (String.valueOf(t).length() <= 11) {
                t *= 1000L;
            }
            return new Date(t);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 每日 HH:mm 在 start 之后的首个时刻 */
    private Date nextFixedTime(Date start, String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) {
            return null;
        }
        String[] parts = hhmm.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(start);
        cal.set(java.util.Calendar.HOUR_OF_DAY, h);
        cal.set(java.util.Calendar.MINUTE, m);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        if (!cal.getTime().after(start)) {
            cal.add(java.util.Calendar.DATE, 1);
        }
        return cal.getTime();
    }

    /** 规则 + 计算出的截止时间 的临时载体（仅本类内排序用） */
    private static final class DueHolder {
        final WfNodeTimeout rule;
        final Date due;
        DueHolder(WfNodeTimeout rule, Date due) {
            this.rule = rule;
            this.due = due;
        }
    }
}
