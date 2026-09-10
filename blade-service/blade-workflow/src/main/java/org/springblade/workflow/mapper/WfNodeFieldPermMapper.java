package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfNodeFieldPerm;

/**
 * 节点字段权限 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfNodeFieldPermMapper extends BaseMapper<WfNodeFieldPerm> {
}
