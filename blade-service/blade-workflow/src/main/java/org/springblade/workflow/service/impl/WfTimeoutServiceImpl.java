package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfNodeTimeoutMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
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
    private final IWfTaskService taskService;
    private final IWfInstanceService instanceService;
    private final IFormmodeClient formmodeClient;

    @Override
    public List<WfNodeTimeout> listEnabled(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return new ArrayList<>();
        }
        return timeoutMapper.selectList(Wrappers.<WfNodeTimeout>lambdaQuery()
            .eq(WfNodeTimeout::getDefId, defId)
            .eq(WfNodeTimeout::getNodeKey, nodeKey)
            .eq(WfNodeTimeout::getEnabled, 1)
            .orderByAsc(WfNodeTimeout::getSeq));
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
            // 兼容旧单条配置
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
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
        try {
            switch (way) {
                case "assign":
                    assign(task, rule);
                    break;
                case "remind":
                    // 仅提醒，不改动流转（提醒在下方统一处理）
                    break;
                case "forward":
                case "autoApprove":
                default:
                    String opinion = (rule.getOpinion() == null || rule.getOpinion().isBlank())
                        ? "超时自动通过" : rule.getOpinion();
                    taskService.autoApprove(task.getId(), opinion);
                    break;
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 超时动作执行失败. taskId={}, way={}", task.getId(), way, e);
        }
        // 提醒（任何动作之后都按配置提醒处理人）
        try {
            remind(task, inst, rule, way);
        } catch (Exception e) {
            log.warn("[blade-workflow] 超时提醒记录失败. taskId={}", task.getId(), e);
        }
        // 标记已执行，避免重复触发
        task.setTimeoutHandled(1);
        taskMapper.updateById(task);
    }

    /** 超时转办：将待办办理人改为指定操作者（取首个；多操作者仅记首人，后续可扩展） */
    private void assign(WfTask task, WfNodeTimeout rule) {
        if (rule.getOperatorIds() == null || rule.getOperatorIds().isBlank()) {
            return;
        }
        String[] ids = rule.getOperatorIds().split(",");
        for (String s : ids) {
            try {
                Long uid = Long.valueOf(s.trim());
                if (uid != null && uid > 0) {
                    task.setAssignee(uid);
                    taskMapper.updateById(task);
                    log.info("[blade-workflow] 超时转办. taskId={}, to={}", task.getId(), uid);
                    return;
                }
            } catch (NumberFormatException ignored) {
                // 跳过非法 ID
            }
        }
    }

    /** 超时提醒：节点处理人本人（remindBeforeOperator）+ 指定人员，按 remindTypes 记流转意见留痕 */
    private void remind(WfTask task, WfInstance inst, WfNodeTimeout rule, String way) {
        if (rule.getRemindTypes() == null || rule.getRemindTypes().isBlank()) {
            return;
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
