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
 * 门户元素用户个性化实体
 * 对应表：blade_portal_element_setting
 * 对标泛微 E-cology：hpElementSettingDetail（元素按用户的配置）
 * <p>
 * 以 {@link PortalElement} 为基准，本表按 user_id 覆盖 perPage / showTitle / styleJson。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_element_setting")
@Schema(name = "门户元素个性化", description = "门户元素按用户的个性化设置（对标 hpElementSettingDetail）")
public class PortalElementSetting extends Model<PortalElementSetting> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("element_id")
	@Schema(name = "元素ID")
	private Long elementId;

	@TableField("user_id")
	@Schema(name = "用户ID")
	private Long userId;

	@TableField("per_page")
	@Schema(name = "该用户下显示条数")
	private Integer perPage;

	@TableField("show_title")
	@Schema(name = "该用户下是否显示标题（null=沿用元素定义）")
	private Integer showTitle;

	@TableField("style_json")
	@Schema(name = "该用户下样式覆盖JSON")
	private String styleJson;

	@TableField("is_deleted")
	@Schema(name = "是否删除（0未删除 1已删除）")
	private Integer isDeleted;

	@TableField("status")
	@Schema(name = "状态（1正常 0禁用 -1已删除）")
	private Integer status;

	@TableField("create_user")
	@Schema(name = "创建人")
	private Long createUser;

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