package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfDefinitionGray;

/**
 * 流程定义灰度规则 Mapper（表 {@code wf_definition_gray}）。
 *
 * <p>与 {@link WfProcessDefinitionMapper} 同口径：灰度规则属于「流程模板」级配置，
 * 应跨租户共享，故脱离 SpringBlade 租户隔离（tenantLine 忽略）。</p>
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfDefinitionGrayMapper extends BaseMapper<WfDefinitionGray> {
}
