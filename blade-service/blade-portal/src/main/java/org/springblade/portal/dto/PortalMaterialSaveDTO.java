package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 素材保存入参（新增/编辑）
 * 对标泛微 materiallib §15.4 C2
 */
@Data
@Schema(name = "门户素材入参", description = "新增/编辑素材库元数据")
public class PortalMaterialSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "所属目录名", requiredMode = Schema.RequiredMode.REQUIRED)
	private String dirName;

	@Schema(name = "素材名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String materialName;

	@Schema(name = "素材类型（image图片 file文件）")
	private String materialType;

	@Schema(name = "文件访问地址（资源服务/OSS 路径）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String fileUrl;

	@Schema(name = "文件大小（字节）")
	private Long fileSize;

	@Schema(name = "MIME类型")
	private String mimeType;

	@Schema(name = "备注")
	private String remark;
}
