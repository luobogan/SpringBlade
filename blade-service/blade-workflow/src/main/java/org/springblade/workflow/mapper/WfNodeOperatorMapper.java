package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfNodeOperator;

/**
 * 节点操作者 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfNodeOperatorMapper extends BaseMapper<WfNodeOperator> {
}
