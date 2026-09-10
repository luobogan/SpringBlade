package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.workflow.entity.WfProcessDefinition;

/**
 * 流程定义 Mapper
 *
 * <p>流程定义（含节点/出口/操作者/字段权限/路径类型）属于「流程模板」，应跨租户共享，
 * 故脱离 SpringBlade 租户隔离（tenantLine 忽略）。运行态数据（实例/任务）仍保持租户隔离。</p>
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfProcessDefinitionMapper extends BaseMapper<WfProcessDefinition> {
}
