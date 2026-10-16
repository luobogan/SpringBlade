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
 * 门户元素配置实体
 * 对应表：blade_portal_element
 * 对标泛微 E-cology：hpelement（元素定义，全局一份）
 * <p>
 * 注意：perPage / showTitle 的「用户级个性化」不在本表，
 * 而在 {@link PortalElementSetting}（对标 hpElementSettingDetail）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_element")
@Schema(name = "门户元素", description = "门户元素定义（对标泛微 hpelement）")
public class PortalElement extends Model<PortalElement> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("page_id")
	@Schema(name = "所属页面ID")
	private Long pageId;

	@TableField("owner_user_id")
	@Schema(name = "归属用户ID（0=页面级共享；>0=终端用户私有，对标 UserAddElementInfo）")
	private Long ownerUserId;

	@TableField("lib_id")
	@Schema(name = "来源元素库ID（blade_portal_element_lib.id，为空表示非库引用）")
	private Long libId;

	@TableField("element_type")
	@Schema(name = "元素类型")
	private String elementType;

	@TableField("element_name")
	@Schema(name = "元素标题")
	private String elementName;

	@TableField("show_title")
	@Schema(name = "是否显示卡片标题（0否 1是）")
	private Integer showTitle;

	@TableField("row_no")
	@Schema(name = "所在行号")
	private Integer rowNo;

	@TableField("col_span")
	@Schema(name = "栅格宽度（24栅格）")
	private Integer colSpan;

	@TableField("sort_order")
	@Schema(name = "同页排序号")
	private Integer sortOrder;

	@TableField("config_json")
	@Schema(name = "元素私有配置JSON")
	private String configJson;

	@TableField("style_json")
	@Schema(name = "元素样式配置JSON")
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