package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户插件保存入参（新增/编辑）
 * 对标泛微 hppluginsetting §14.2 G
 */
@Data
@Schema(name = "门户插件入参", description = "新增/编辑门户插件登记")
public class PortalPluginSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "插件标识（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String pid;

	@Schema(name = "插件描述")
	private String pluginDesc;

	@Schema(name = "插件文件路径（JS/CSS 资源地址）")
	private String filePath;

	@Schema(name = "是否启用（0停用 1启用）")
	private Integer isUse;

	@Schema(name = "排序号（越小越靠前）")
	private Integer orderNum;

	@Schema(name = "插件类型（js脚本/css样式/component组件）")
	private String pType;

	@Schema(name = "备注")
	private String remark;
}
