package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfProcessNode;

/**
 * 流程节点 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfProcessNodeMapper extends BaseMapper<WfProcessNode> {
}
