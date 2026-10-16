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
 * 门户独立元素库实体
 * 对应表：blade_portal_element_lib
 * 对标泛微 E-cology：hpBaseElement / HPELEMENTTEMPLATE（元素库 + 元素模板，§12.5 P3）
 * <p>元素定义入库后可被多个页面「引用」实例化（页面元素记 lib_id）；
 * 封存(status=0)后不可再被新引用，既有引用继续有效。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_element_lib")
@Schema(name = "门户元素库", description = "可复用的独立元素定义库（对标 hpBaseElement）")
public class PortalElementLib extends Model<PortalElementLib> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("element_type")
	@Schema(name = "元素类型")
	private String elementType;

	@TableField("element_name")
	@Schema(name = "元素名称")
	private String elementName;

	@TableField("config_json")
	@Schema(name = "元素私有配置JSON")
	private String configJson;

	@TableField("style_json")
	@Schema(name = "元素样式配置JSON")
	private String styleJson;

	@TableField("remark")
	@Schema(name = "备注")
	private String remark;

	@TableField("is_deleted")
	@Schema(name = "是否删除（0未删除 1已删除）")
	private Integer isDeleted;

	@TableField("status")
	@Schema(name = "状态（1正常 0封存 -1已删除）")
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
