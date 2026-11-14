package org.springblade.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

import java.io.Serial;

/**
 * 数据权限可见字段配置实体。
 *
 * <p>与 {@code blade_scope_data} 通过 scopeId 关联；冗余 menuId / resourceCode
 * 供业务列表按菜单直接查询列配置。字段中文名、类型、说明、顺序、可见与
 * 可编辑标记用于列表动态生成表头，不参与 SQL 拼接（拼接仍由 scopeField 负责）。</p>
 */
@Data
@TableName("blade_scope_field")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据权限可见字段配置")
public class ScopeField extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "数据权限ID（blade_scope_data.id）；0=菜单级默认配置")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long scopeId;

	@Schema(description = "菜单ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long menuId;

	@Schema(description = "资源编号")
	private String resourceCode;

	@Schema(description = "源字段定义ID（workflow_billfield.id）")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long fieldId;

	@Schema(description = "字段名（数据库列名）")
	private String fieldName;

	@Schema(description = "字段名称（列表表头显示名）")
	private String fieldLabel;

	@Schema(description = "数据库字段类型")
	private String fieldDbType;

	@Schema(description = "展示/控件类型")
	private String fieldHtmlType;

	@Schema(description = "字段子类型（人力资源/部门/日期等浏览按钮子类；与 fieldHtmlType 组合确定表单控件类型，存子类型编号）")
	private String fieldType;

	@Schema(description = "字段说明")
	private String description;

	@Schema(description = "是否可见：1是 0否")
	private Integer isVisible;

	@Schema(description = "是否可编辑：1是 0否")
	private Integer isEditable;

	@Schema(description = "列展示顺序")
	private Integer sort;

	@Schema(description = "列宽")
	private Integer width;

	@Schema(description = "对齐方式")
	private String align;
}
