package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户配置项实体（对标泛微 portalconfig 表 §16.6）
 * <p>租户维度键值配置（ccode/vvalue/name/note），管理端列出并编辑取值；
 * 进程内缓存 + TTL，set 后 refresh 即时失效（对标 PortalConfigUtil）。</p>
 */
@Data
@TableName("blade_portal_config")
@Schema(name = "门户配置项", description = "门户级键值配置（对标泛微 portalconfig）")
public class PortalConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("ccode")
	@Schema(name = "配置项编码（租户内唯一）")
	private String ccode;

	@TableField("vvalue")
	@Schema(name = "配置项取值")
	private String vvalue;

	@TableField("name")
	@Schema(name = "配置项名称（展示）")
	private String name;

	@TableField("note")
	@Schema(name = "配置项说明（展示）")
	private String note;

	@TableField("is_deleted")
	private Integer isDeleted;

	@TableField("status")
	private Integer status;

	@TableField("create_user")
	private Long createUser;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_user")
	private Long updateUser;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
