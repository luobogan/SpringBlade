package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户页面按日访问统计实体
 * 对应表：blade_portal_page_stat（页面 + 日期 + 租户维度每日 PV）
 */
@Data
@TableName("blade_portal_page_stat")
@Schema(name = "PortalPageStat", description = "门户页面按日访问统计")
public class PortalPageStat implements Serializable {

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键")
	private Long id;

	@TableField("page_id")
	@Schema(name = "门户页面ID")
	private Long pageId;

	@TableField("stat_date")
	@Schema(name = "统计日期 yyyy-MM-dd")
	private String statDate;

	@TableField("view_count")
	@Schema(name = "当日访问量")
	private Long viewCount;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("is_deleted")
	@Schema(name = "逻辑删除")
	private Integer isDeleted;
}
