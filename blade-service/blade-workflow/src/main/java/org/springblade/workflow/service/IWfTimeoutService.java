package org.springblade.workflow.service;

import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfTask;

import java.util.Date;
import java.util.List;

/**
 * 节点超时规则服务：多条规则（起算/时长/截止 + 四态动作 + 提醒通道）的读取、保存与到期执行。
 */
public interface IWfTimeoutService {

    /** 取节点已启用的超时规则（按 seq 升序） */
    List<WfNodeTimeout> listEnabled(Long defId, String nodeKey);

    /** 保存（覆盖）某节点的全部超时规则 */
    void saveRules(Long defId, String nodeKey, List<WfNodeTimeout> rules);

    /**
     * 计算某待办的最早截止时间（多条规则取最早）；无规则时回退旧 {@code settings.timeout.hours}。
     * 返回 null 表示不计时。
     */
    Date resolveDueTime(Long defId, String nodeKey, WfTask task, WfInstance inst);

    /** 计算单条规则的截止时间；无法计算（如配置不全）返回 null */
    Date computeDue(WfNodeTimeout rule, WfTask task, WfInstance inst);

    /** 在已启用规则中取第一条已到期的（按到期时间升序）；无则返回 null */
    WfNodeTimeout firstOverdue(Long defId, String nodeKey, WfTask task, WfInstance inst, Date now);

    /** 执行某条规则的超时动作 + 提醒 */
    void fire(WfNodeTimeout rule, WfTask task, WfInstance inst);

}
