package org.springblade.workflow.service.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审批日志「读源=act」时的 ACT_HI_COMMENT 还原器 + 读源开关。
 *
 * <p>与 {@link WfWriteHelper#syncCommentToEngine} 对称：写侧把
 * {@code nodeKey/operator/wfTaskId/opinion/ts} 编码进 {@code ACT_HI_COMMENT.MESSAGE_}，
 * 本类把 {@code MESSAGE_} 还原成 {@link WfApprovalLog} 维度（nodeKey/operator/opinion/logType/operateTime）。</p>
 *
 * <p>抽成独立 Bean，供 {@code WfInstanceServiceImpl}（logs / nodeOperators）与 {@code WfTestServiceImpl}
 * （测试进度时间线）共用同一「读源开关 + 解析契约」，避免多份实现漂移、与写侧失配。</p>
 *
 * <p><b>前置</b>：须开启 {@code blade.workflow.approval-comment.enabled} 双写，且历史评论已回填
 * （见 {@code doc/sql/migration/act_backfill_approval_comment.sql}），否则翻源后仅见双写开启后的记录。</p>
 */
@Slf4j
@Component
public class WfApprovalLogActReader {

    private static final ObjectMapper COMMENT_OM = new ObjectMapper();

    /** 实例/日志读源：wf=遗留 wf_*（默认，零行为变化）；act=原生 ACT_HI_*（去 wf_ 表） */
    @Value("${blade.workflow.instance-read-source:wf}")
    private String instanceReadSource;

    private final JdbcTemplate jdbcTemplate;

    public WfApprovalLogActReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 实例/日志读源是否切到原生 ACT（去 wf_ 表） */
    public boolean actRead() {
        return "act".equalsIgnoreCase(instanceReadSource);
    }

    /**
     * 从 ACT_HI_COMMENT 还原审批日志。engineInstId 为空（实例从未部署到引擎）返回空。
     * wfInstId 仅回填到 {@link WfApprovalLog#getInstId()}，便于下游按 wf 实例维度消费。
     */
    public List<WfApprovalLog> readFromAct(String engineInstId, Long wfInstId) {
        if (engineInstId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT TYPE_, TIME_, MESSAGE_ FROM ACT_HI_COMMENT "
            + "WHERE PROC_INST_ID_ = ? ORDER BY TIME_ ASC";
        List<WfApprovalLog> rows = jdbcTemplate.query(sql, (rs, i) -> {
            WfApprovalLog l = parseActComment(rs.getString("MESSAGE_"),
                rs.getString("TYPE_"), rs.getTimestamp("TIME_"));
            if (l == null) {
                return null;
            }
            l.setInstId(wfInstId);
            return l;
        }, engineInstId);
        return rows.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 对称解析单条 ACT_HI_COMMENT（与 {@code WfWriteHelper.syncCommentToEngine} 写入的 JSON 形态一致）。
     * 解析失败（非本模块写入的评论 / 格式异常）返回 null，由调用方跳过，避免污染流转意见。
     */
    @SuppressWarnings("unchecked")
    public static WfApprovalLog parseActComment(String message, String type, Date time) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Map<String, Object> p;
        try {
            p = COMMENT_OM.readValue(message, Map.class);
        } catch (Exception e) {
            return null;
        }
        WfApprovalLog l = new WfApprovalLog();
        l.setId(null);
        l.setTaskId(toLong(p.get("wfTaskId")));
        l.setNodeKey(p.get("nodeKey") == null ? "" : String.valueOf(p.get("nodeKey")));
        l.setOperator(toLong(p.get("operator")));
        l.setLogType(type);
        l.setOpinion(p.get("opinion") == null ? "" : String.valueOf(p.get("opinion")));
        l.setOperateTime(time);
        return l;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
