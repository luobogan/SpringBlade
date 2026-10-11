package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.springblade.workflow.entity.WfNodeFieldPerm;

/**
 * 节点字段权限 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfNodeFieldPermMapper extends BaseMapper<WfNodeFieldPerm> {

	/**
	 * 幂等写入：重复导入（def_id+node_key+scope+field_name 已存在）时转为 UPDATE，避免
	 * uk_node_scope_field 唯一键冲突导致整个 importBpmnXml 事务回滚（首导入成功、再导入失败）。
	 * 覆盖式删插（见 WfDefinitionServiceImpl.importNodeFieldPermsFromWfNode）仍负责清除 BPMN 已移除的字段权限行。
	 */
	@Insert("INSERT INTO wf_node_field_perm (def_id, node_key, scope, field_name, perm, is_visible, is_editable, is_required) "
		+ "VALUES (#{defId}, #{nodeKey}, #{scope}, #{fieldName}, #{perm}, #{isVisible}, #{isEditable}, #{isRequired}) "
		+ "ON DUPLICATE KEY UPDATE perm = VALUES(perm), is_visible = VALUES(is_visible), "
		+ "is_editable = VALUES(is_editable), is_required = VALUES(is_required)")
	int upsertFieldPerm(WfNodeFieldPerm p);
}
