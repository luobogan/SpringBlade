package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户元素模板实体
 * 对应表：blade_portal_element_template
 * 对标泛微 E-cology：HPELEMENTTEMPLATE（§15.3 B2 elementtemplate 元素模板库：另存为/预览/批量删除）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_element_template")
@Schema(name = "门户元素模板", description = "元素配置快照模板（对标泛微 HPELEMENTTEMPLATE）")
public class PortalElementTemplate extends Model<PortalElementTemplate> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("element_type")
	@Schema(name = "元素类型（todo/notice/message/chart/links/rich）")
	private String elementType;

	@TableField("template_name")
	@Schema(name = "模板名称")
	private String templateName;

	@TableField("config_json")
	@Schema(name = "元素私有配置JSON（模板快照）")
	private String configJson;

	@TableField("style_json")
	@Schema(name = "元素样式配置JSON（模板快照）")
	private String styleJson;

	@TableField("remark")
	private String remark;

	@TableField("is_deleted")
	private Integer isDeleted;

	@TableField("status")
	private Integer status;

	@TableField("create_user")
	private Long createUser;

	@TableField("create_dept")
	private Long createDept;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_user")
	private Long updateUser;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
