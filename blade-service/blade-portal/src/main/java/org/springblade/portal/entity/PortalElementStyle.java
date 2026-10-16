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
 * 门户元素样式实体（style_kind 区分 元素样式 / 头条样式）
 * 对应表：blade_portal_element_style
 * 对标泛微 E-cology：hpMenuStyle(menustyletype='element') + headlinestyle（§15.3 B3/B4）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_element_style")
@Schema(name = "门户元素样式", description = "元素样式库+头条样式库（style_kind 区分，对标 §15.3 B3/B4）")
public class PortalElementStyle extends Model<PortalElementStyle> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("style_kind")
	@Schema(name = "样式分类（element元素样式 headline头条样式）")
	private String styleKind;

	@TableField("style_name")
	@Schema(name = "样式名称")
	private String styleName;

	@TableField("style_desc")
	@Schema(name = "样式描述")
	private String styleDesc;

	@TableField("style_config")
	@Schema(name = "样式配置JSON（卡片/网格/头条布局参数）")
	private String styleConfig;

	@TableField("cite_count")
	@Schema(name = "被引用次数（引用追踪）")
	private Integer citeCount;

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
