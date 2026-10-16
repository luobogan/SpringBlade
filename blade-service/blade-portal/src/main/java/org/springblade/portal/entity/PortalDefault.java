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
 * 门户默认首页（按机构）实体
 * 对应表：blade_portal_default
 * 对标泛微 E-cology：defaultportal（按机构指定登录后默认首页，见 §13.8）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_default")
@Schema(name = "门户默认首页（按机构）", description = "按机构指定登录后默认首页（对标泛微 defaultportal）")
public class PortalDefault extends Model<PortalDefault> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("dept_id")
	@Schema(name = "机构/部门ID")
	private Long deptId;

	@TableField("page_id")
	@Schema(name = "默认门户页面ID")
	private Long pageId;

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
