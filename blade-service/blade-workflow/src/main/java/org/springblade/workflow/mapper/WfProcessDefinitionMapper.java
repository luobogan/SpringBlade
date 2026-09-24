package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.workflow.entity.WfProcessDefinition;

import java.util.List;

/**
 * 流程定义 Mapper
 *
 * <p>流程定义（含节点/出口/操作者/字段权限/路径类型）属于「流程模板」，应跨租户共享，
 * 故脱离 SpringBlade 租户隔离（tenantLine 忽略）。运行态数据（实例/任务）仍保持租户隔离。</p>
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfProcessDefinitionMapper extends BaseMapper<WfProcessDefinition> {

	/**
	 * 按 proc_key + version 查全部行（含逻辑删除的幽灵行）。
	 * 注意：这是自定义原生 SQL，MP 的逻辑删除（仅作用于自动生成方法）不会对其注入 is_deleted 过滤，
	 * 故可查到幽灵行；tenantLine 需显式忽略（流程模板跨租户共享）。用于导入前清理同键旧记录。
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM wf_process_definition WHERE proc_key = #{procKey} AND version = #{version}")
	List<WfProcessDefinition> selectAllByProcKeyVersion(@Param("procKey") String procKey, @Param("version") Integer version);

	/**
	 * 按 id 查（含逻辑删除的幽灵行），用于物理清理前取 form_id 等。
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT * FROM wf_process_definition WHERE id = #{id}")
	WfProcessDefinition selectByIdIgnoreLogic(@Param("id") Long id);

	/**
	 * 物理删除流程定义（原生 SQL，绕过 @TableLogic 的逻辑删除改写），用于导入幂等清理。
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Delete("DELETE FROM wf_process_definition WHERE id = #{id}")
	int physicalDeleteById(@Param("id") Long id);
}
