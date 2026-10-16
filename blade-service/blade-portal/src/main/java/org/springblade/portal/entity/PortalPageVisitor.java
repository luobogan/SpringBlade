package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户页面独立访客实体（按 页面+日期+租户+用户 去重）
 * 对应表：blade_portal_page_visitor
 */
@Data
@TableName("blade_portal_page_visitor")
@Schema(name = "PortalPageVisitor", description = "门户页面独立访客（按日/用户去重）")
public class PortalPageVisitor implements Serializable {

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键")
	private Long id;

	@TableField("page_id")
	@Schema(name = "门户页面ID")
	private Long pageId;

	@TableField("stat_date")
	@Schema(name = "统计日期 yyyy-MM-dd")
	private String statDate;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("user_id")
	@Schema(name = "用户ID")
	private Long userId;

	@TableField("is_deleted")
	@Schema(name = "逻辑删除")
	private Integer isDeleted;
}
