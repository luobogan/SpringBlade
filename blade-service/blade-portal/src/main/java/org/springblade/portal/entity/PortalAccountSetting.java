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
 * 门户个人通用设置实体（用户单例）
 * 对应表：blade_portal_account_setting
 * 对标泛微 E-cology：accountmore（§14.2 I，ACCOUNTMOREMENUINFO + HrmUserSetting 个人字体/布局）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_account_setting")
@Schema(name = "门户个人通用设置", description = "个人字体/布局/更多菜单显隐（对标泛微 accountmore）")
public class PortalAccountSetting extends Model<PortalAccountSetting> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("user_id")
	@Schema(name = "用户ID（用户内唯一）")
	private Long userId;

	@TableField("font_size")
	@Schema(name = "个人字体（1大 2中 3默认；NULL=沿用全局）")
	private Integer fontSize;

	@TableField("layout_type")
	@Schema(name = "个人布局类型（1舒适 2标准 3紧凑；NULL=沿用全局）")
	private Integer layoutType;

	@TableField("menu_config_json")
	@Schema(name = "个人\"更多\"菜单显隐JSON（对标 ACCOUNTMOREMENUINFO）")
	private String menuConfigJson;

	@TableField("remark")
	private String remark;

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
