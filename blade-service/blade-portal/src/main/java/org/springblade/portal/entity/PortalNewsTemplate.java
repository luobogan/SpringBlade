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
 * 门户新闻模板实体
 * 对应表：blade_portal_news_template
 * 对标泛微 E-cology：PAGENEWSTEMPLATE（§15.4 C1 newstemplate 新闻模板库，inner/outer 两类）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_news_template")
@Schema(name = "门户新闻模板", description = "内部/外部新闻排版模板（对标泛微 PAGENEWSTEMPLATE）")
public class PortalNewsTemplate extends Model<PortalNewsTemplate> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("template_name")
	@Schema(name = "模板名称")
	private String templateName;

	@TableField("template_type")
	@Schema(name = "模板类型（inner内部新闻 outer外部新闻）")
	private String templateType;

	@TableField("layout_html")
	@Schema(name = "版式HTML（{title}/{summary}/{time} 占位符替换）")
	private String layoutHtml;

	@TableField("is_default")
	@Schema(name = "是否默认模板（同类型内唯一）")
	private Integer isDefault;

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
