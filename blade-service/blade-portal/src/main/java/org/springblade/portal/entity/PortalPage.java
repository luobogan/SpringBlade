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
 * 门户页面配置实体
 * 对应表：blade_portal_page
 * 对标泛微 E-cology：homepage（门户页面容器）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_page")
@Schema(name = "门户页面", description = "门户页面配置（对标泛微 homepage）")
public class PortalPage extends Model<PortalPage> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("page_code")
	@Schema(name = "页面编码")
	private String pageCode;

	@TableField("page_name")
	@Schema(name = "页面名称")
	private String pageName;

	@TableField("is_default")
	@Schema(name = "是否默认门户（0否 1是）")
	private Integer isDefault;

	@TableField("page_type")
	@Schema(name = "页面类型：0登录前门户 1登录后门户（对标泛微 loginpage/mainpage）")
	private Integer pageType;

	@TableField("view_count")
	@Schema(name = "访问次数（热度统计）")
	private Long viewCount;

	@TableField("sort")
	@Schema(name = "显示顺序")
	private Integer sort;

	@TableField("theme")
	@Schema(name = "主题标识")
	private String theme;

	@TableField("remark")
	@Schema(name = "备注")
	private String remark;

	@TableField("is_deleted")
	@Schema(name = "是否删除（0未删除 1已删除）")
	private Integer isDeleted;

	@TableField("status")
	@Schema(name = "状态（1正常 0禁用 -1已删除）")
	private Integer status;

	@TableField("create_user")
	@Schema(name = "创建人")
	private Long createUser;

	@TableField("create_dept")
	@Schema(name = "创建部门")
	private Long createDept;

	@TableField("create_time")
	@Schema(name = "创建时间")
	private LocalDateTime createTime;

	@TableField("update_user")
	@Schema(name = "修改人")
	private Long updateUser;

	@TableField("update_time")
	@Schema(name = "修改时间")
	private LocalDateTime updateTime;
}