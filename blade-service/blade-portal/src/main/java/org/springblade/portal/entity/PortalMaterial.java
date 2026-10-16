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
 * 门户素材库元数据实体
 * 对应表：blade_portal_material
 * 对标泛微 E-cology：materiallib（§15.4 C2；泛微纯文件系统无关系表，SpringBlade 落地为
 * 「文件走资源服务 + 元数据落表」，本地化偏差见设计说明）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_material")
@Schema(name = "门户素材", description = "素材库元数据（文件走资源服务，对标泛微 materiallib）")
public class PortalMaterial extends Model<PortalMaterial> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("dir_name")
	@Schema(name = "所属目录名（对标 getDirs 目录维度）")
	private String dirName;

	@TableField("material_name")
	@Schema(name = "素材名称")
	private String materialName;

	@TableField("material_type")
	@Schema(name = "素材类型（image图片 file文件）")
	private String materialType;

	@TableField("file_url")
	@Schema(name = "文件访问地址（资源服务/OSS 路径）")
	private String fileUrl;

	@TableField("file_size")
	@Schema(name = "文件大小（字节）")
	private Long fileSize;

	@TableField("mime_type")
	@Schema(name = "MIME类型")
	private String mimeType;

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
