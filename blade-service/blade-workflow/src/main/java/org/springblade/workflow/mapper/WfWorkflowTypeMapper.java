package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfWorkflowType;

/**
 * 流程（路径）类型 Mapper（全局共享，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfWorkflowTypeMapper extends BaseMapper<WfWorkflowType> {
}
