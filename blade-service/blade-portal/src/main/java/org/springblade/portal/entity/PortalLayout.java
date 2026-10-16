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
 * 门户布局模板实体
 * 对应表：blade_portal_layout
 * 对标泛微 E-cology：portallayout（布局库 design/sys/cus，§12.3.5 / §12.5 P2）
 * <p>elements_json 存页面级(owner=0)元素排列（类型/行/栅格/排序/配置），
 * 可从模板一键实例化新门户页（复用导入链路）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_layout")
@Schema(name = "门户布局模板", description = "可复用的门户布局模板（对标泛微 portallayout）")
public class PortalLayout extends Model<PortalLayout> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("layout_name")
	@Schema(name = "模板名称")
	private String layoutName;

	@TableField("layout_type")
	@Schema(name = "模板类型：sys内置 cus自定义")
	private String layoutType;

	@TableField("elements_json")
	@Schema(name = "元素排列JSON（页面级元素快照）")
	private String elementsJson;

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
