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
 * 门户菜单样式实体
 * 对应表：blade_portal_menu_style
 * 对标泛微 E-cology：hpMenuStyle（menustyletype=menuh/menuv，§14.2 E menustylelib 菜单样式库）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_menu_style")
@Schema(name = "门户菜单样式", description = "横向/纵向菜单样式（对标泛微 hpMenuStyle menuh/menuv）")
public class PortalMenuStyle extends Model<PortalMenuStyle> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("style_name")
	@Schema(name = "样式名称")
	private String styleName;

	@TableField("style_desc")
	@Schema(name = "样式描述")
	private String styleDesc;

	@TableField("style_type")
	@Schema(name = "样式类型（menuh横向 menuv纵向）")
	private String styleType;

	@TableField("style_config")
	@Schema(name = "样式配置JSON（配色/字号/图标位置等）")
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
