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
 * 门户插件登记实体
 * 对应表：blade_portal_plugin
 * 对标泛微 E-cology：hppluginsetting（§14.2 G，pluginmanage 插件管理）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_plugin")
@Schema(name = "门户插件", description = "门户插件登记（pid/文件路径/启用/排序/类型，对标泛微 hppluginsetting）")
public class PortalPlugin extends Model<PortalPlugin> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("pid")
	@Schema(name = "插件标识（租户内唯一）")
	private String pid;

	@TableField("plugin_desc")
	@Schema(name = "插件描述")
	private String pluginDesc;

	@TableField("file_path")
	@Schema(name = "插件文件路径（JS/CSS 资源地址）")
	private String filePath;

	@TableField("is_use")
	@Schema(name = "是否启用（0停用 1启用）")
	private Integer isUse;

	@TableField("order_num")
	@Schema(name = "排序号（越小越靠前）")
	private Integer orderNum;

	@TableField("p_type")
	@Schema(name = "插件类型（js脚本/css样式/component组件）")
	private String pType;

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
