package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfTask;

/**
 * 流程任务 Mapper（<b>仅引擎任务本体台账</b>）。
 *
 * <p><b>【去 wf_ 表 · 2026-11-09】</b>本 Mapper 只承载引擎任务本体（A 类：待办/已办/办结/
 * 自动提交），其业务列是 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 自定义列与
 * {@code BIZ_TASK_ID_} 契约的唯一来源，<b>不可停写</b>。
 * blade 扩展任务（协办/抄送/传阅/已读/退回重提交/自由流程步骤名）请用
 * {@link WfExtTaskMapper}（表 {@code wf_ext_task}）。</p>
 */
public interface WfTaskMapper extends BaseMapper<WfTask> {
}
