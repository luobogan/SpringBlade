package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元素注册实体（对标泛微 elementregister §15.3 B5）
 * <p>上传 zip 元素开发包 → 解压读取 manifest.json → 校验命名/唯一性 → 登记。
 * 编译型前端约束：注册的自定义元素须前端部署对应渲染器后方可设计/渲染。</p>
 */
@Data
@TableName("blade_portal_element_register")
@Schema(name = "元素注册", description = "元素开发包注册登记")
public class PortalElementRegister implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("element_type")
	@Schema(name = "元素类型编码（小写字母开头，租户唯一；不与内置6类冲突）")
	private String elementType;

	@TableField("element_name")
	@Schema(name = "元素名称")
	private String elementName;

	@TableField("version")
	@Schema(name = "元素包版本")
	private String version;

	@TableField("manifest_json")
	@Schema(name = "manifest.json 原文")
	private String manifestJson;

	@TableField("package_path")
	@Schema(name = "zip 包存储路径")
	private String packagePath;

	@TableField("package_size")
	@Schema(name = "包大小（字节）")
	private Long packageSize;

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
