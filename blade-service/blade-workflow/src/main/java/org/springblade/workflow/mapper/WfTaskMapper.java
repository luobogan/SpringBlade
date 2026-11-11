package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfTask;

/**
 * 流程任务 Mapper（历史台账，<b>已废弃</b>）。
 *
 * <p><b>【历史 · 2026-11-09】</b>曾只承载引擎任务本体（A 类：待办/已办/办结/自动提交），
 * 其业务列是 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 自定义列与
 * {@code BIZ_TASK_ID_} 契约的来源。</p>
 *
 * <p><b>【2026-11-10 终态】</b>批次 1 后 {@code wf_task} 运行时零写入，
 * 本 Mapper 仅剩存量行的只读回溯、清理与 {@code legacy} 回退路径。
 * 新代码的任务写入请走 {@code WfTaskActDirectWriter}，定位请走 {@code WfTaskBizResolver}；
 * blade 扩展任务请用 {@link WfExtTaskMapper}（表 {@code wf_ext_task}）。</p>
 *
 * @deprecated <b>2026-11-10 已废弃</b>：不物理删除（用户决策：仅打标记保留可追溯）。
 *     禁止新增对本 Mapper 的写入调用；保留其余只读/清理路径避免破坏存量兼容。
 */
@Deprecated
public interface WfTaskMapper extends BaseMapper<WfTask> {
}
