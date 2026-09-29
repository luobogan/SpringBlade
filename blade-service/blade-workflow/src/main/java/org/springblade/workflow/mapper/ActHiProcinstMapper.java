package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.ActHiProcinst;

/**
 * {@link ActHiProcinst} 读映射器。
 *
 * <p>仅用于「去 wf_ 表」后实例读源切换（P4/P5）。所有查询走原生 {@code ACT_HI_PROCINST}，
 * 不写本表（业务列由 {@code WfInstanceActWriter} 在引擎同事务内写回）。</p>
 */
public interface ActHiProcinstMapper extends BaseMapper<ActHiProcinst> {
}
