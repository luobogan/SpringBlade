package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfSubflowRequest;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfSubflowRequestMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfSubflowService;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 子流程高级设置语义服务实现。
 *
 * <p>设计要点：子流程触发后，主流程的完成（归档）会被 {@code allEndBeforeSubmit} 阻塞，
 * 直到本节点所有关联子流程均归档；全部归档后再执行数据汇总 / 提醒 / 自动推进主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfSubflowServiceImpl implements IWfSubflowService {

    private final WfSubflowRequestMapper subflowRequestMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final IWfInstanceService instanceService;

    @Override
    public void record(Long mainInstId, Long subInstId, Long subDefId, WfProcessNode node) {
        if (mainInstId == null || subInstId == null || node == null) {
            return;
        }
        WfSubflowRequest r = new WfSubflowRequest();
        r.setMainInstId(mainInstId);
        r.setSubInstId(subInstId);
        r.setSubDefId(subDefId);
        r.setMainNodeKey(node.getNodeKey());
        // 是否「相同子流程」范围：本实现按触发节点统一归组，isSame 默认 1
        r.setIsSame(1);
        r.setAllEndBeforeSubmit(WfNodeSettingsUtil.subflowAllEndBeforeSubmit(node));
        r.setDataSummary(WfNodeSettingsUtil.subflowDataSummary(node));
        r.setAutoForward(WfNodeSettingsUtil.subflowAutoForward(node));
        r.setRemindEnabled(WfNodeSettingsUtil.subflowRemindEnabled(node));
        r.setRemindTypes(WfNodeSettingsUtil.subflowRemindTypes(node));
        r.setRemindBeforeOperator(WfNodeSettingsUtil.subflowRemindBeforeOperator(node));
        r.setRemindPersons(WfNodeSettingsUtil.subflowRemindPersons(node));
        r.setStatus(0);
        r.setCreateTime(new java.util.Date());
        subflowRequestMapper.insert(r);
    }

    @Override
    public void onSubflowArchived(Long subInstId) {
        if (subInstId == null) {
            return;
        }
        WfSubflowRequest req = subflowRequestMapper.selectOne(
            Wrappers.<WfSubflowRequest>lambdaQuery().eq(WfSubflowRequest::getSubInstId, subInstId));
        if (req == null || (req.getStatus() != null && req.getStatus() == 1)) {
            return;
        }
        req.setStatus(1);
        req.setArchiveTime(new java.util.Date());
        subflowRequestMapper.updateById(req);

        Long mainInstId = req.getMainInstId();
        String mainNodeKey = req.getMainNodeKey();
        List<WfSubflowRequest> group = subflowRequestMapper.selectList(Wrappers.<WfSubflowRequest>lambdaQuery()
            .eq(WfSubflowRequest::getMainInstId, mainInstId)
            .eq(mainNodeKey != null, WfSubflowRequest::getMainNodeKey, mainNodeKey));
        boolean allArchived = group.stream()
            .noneMatch(g -> g.getStatus() == null || g.getStatus() != 1);
        if (!allArchived) {
            return; // 仍有未归档子流程，等待
        }

        // 全部归档：数据汇总（快照含 dataSummary 的任一请求即生效）
        boolean summary = group.stream()
            .anyMatch(g -> g.getDataSummary() != null && g.getDataSummary() == 1);
        if (summary) {
            instanceService.recordLog(mainInstId, mainNodeKey, 0L,
                WfApprovalLog.LOG_COMMENT, "子流程已全部归档，数据已汇总至主流程");
        }

        // 全部归档后提醒（节点操作者本人 / 指定人员）
        boolean remind = group.stream()
            .anyMatch(g -> g.getRemindEnabled() != null && g.getRemindEnabled() == 1);
        if (remind && req.getRemindEnabled() != null && req.getRemindEnabled() == 1) {
            List<Long> recipients = resolveRecipients(mainInstId, mainNodeKey, req);
            String msg = buildRemindMsg(req);
            for (Long who : recipients) {
                instanceService.recordLog(mainInstId, mainNodeKey, who, WfApprovalLog.LOG_COMMENT, msg);
            }
        }

        // 全部归档后自动流转主流程
        boolean forward = group.stream()
            .anyMatch(g -> g.getAutoForward() != null && g.getAutoForward() == 1);
        if (forward) {
            WfInstance main = instanceMapper.selectById(mainInstId);
            if (main != null && WfInstance.STATUS_APPROVED != main.getStatus()) {
                instanceService.advance(mainInstId, resolveMainNodeOperator(mainInstId, mainNodeKey));
            }
        }
    }

    /** 解析提醒接收人：节点操作者本人（取该节点最近办结人）+ 指定人员 */
    private List<Long> resolveRecipients(Long mainInstId, String mainNodeKey, WfSubflowRequest req) {
        List<Long> recipients = new ArrayList<>();
        if (req.getRemindBeforeOperator() != null && req.getRemindBeforeOperator() == 1) {
            Long op = resolveMainNodeOperator(mainInstId, mainNodeKey);
            if (op != null && op > 0) {
                recipients.add(op);
            }
        }
        if (req.getRemindPersons() != null && !req.getRemindPersons().isBlank()) {
            for (String s : req.getRemindPersons().split(",")) {
                try {
                    Long id = Long.valueOf(s.trim());
                    if (id > 0 && !recipients.contains(id)) {
                        recipients.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // 跳过非法 ID
                }
            }
        }
        if (recipients.isEmpty()) {
            recipients.add(0L); // 无明确接收人时记到系统（仍留痕）
        }
        return recipients;
    }

    /** 取主流程触发节点的「最近办结人」作为「节点操作者本人」 */
    private Long resolveMainNodeOperator(Long mainInstId, String mainNodeKey) {
        if (mainNodeKey == null) {
            return 0L;
        }
        WfTask task = taskMapper.selectOne(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, mainInstId)
            .eq(WfTask::getNodeKey, mainNodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_DONE)
            .orderByDesc(WfTask::getOperateTime)
            .last("LIMIT 1"));
        return task == null ? 0L : (task.getAssignee() == null ? 0L : task.getAssignee());
    }

    private String buildRemindMsg(WfSubflowRequest req) {
        StringBuilder sb = new StringBuilder("子流程已全部归档");
        String types = req.getRemindTypes();
        if (types != null && !types.isBlank()) {
            List<String> ch = new ArrayList<>();
            if (types.contains("sys")) ch.add("流程提醒");
            if (types.contains("ml")) ch.add("短信");
            if (types.contains("sm")) ch.add("邮件");
            if (!ch.isEmpty()) {
                sb.append("（").append(String.join("/", ch)).append("）");
            }
        }
        sb.append("，请处理主流程");
        return sb.toString();
    }

    @Override
    public boolean holdForSubflow(Long mainInstId) {
        if (mainInstId == null) {
            return false;
        }
        long pending = subflowRequestMapper.selectCount(Wrappers.<WfSubflowRequest>lambdaQuery()
            .eq(WfSubflowRequest::getMainInstId, mainInstId)
            .eq(WfSubflowRequest::getStatus, 0)
            .eq(WfSubflowRequest::getAllEndBeforeSubmit, 1));
        return pending > 0;
    }
}
