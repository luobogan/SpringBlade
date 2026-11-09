package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfExtTask;

/**
 * 扩展任务 Mapper（协办/加签协办/抄送/传阅/已读/退回重提交/自由流程步骤）。
 *
 * <p>去 wf_ 表 · P6 退役第一台阶：blade 扩展任务的权威存储，列表/角标的扩展任务段由此读取。
 * 引擎任务本体仍由 {@link WfTaskMapper} 承载。</p>
 */
public interface WfExtTaskMapper extends BaseMapper<WfExtTask> {
}
