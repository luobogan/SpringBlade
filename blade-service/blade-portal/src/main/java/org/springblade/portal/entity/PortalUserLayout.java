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
 * 用户个人门户布局实体
 * 对应表：blade_portal_user_layout
 * 对标泛微 E-cology：用户专属 hplayout / hpcurrentuse（§18.2 / §23）
 * <p>按 (userId, pageId) 一行，layout_json 存该用户对该门户页元素的排列覆盖
 * （排序/行号/栅格宽/显隐）。为空=沿用创建者默认布局。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_user_layout")
@Schema(name = "用户个人门户布局", description = "按用户持久化门户元素排列（对标泛微 hpcurrentuse）")
public class PortalUserLayout extends Model<PortalUserLayout> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("user_id")
	@Schema(name = "用户ID")
	private Long userId;

	@TableField("page_id")
	@Schema(name = "门户页面ID（blade_portal_page.id）")
	private Long pageId;

	@TableField("layout_json")
	@Schema(name = "布局JSON：[{elementId,rowNo,colSpan,sortOrder,hidden}]")
	private String layoutJson;

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
