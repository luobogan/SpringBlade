package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfNodeLink;

/**
 * 流程出口 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfNodeLinkMapper extends BaseMapper<WfNodeLink> {
}
