package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 协同区保存 DTO（对标泛微 synergy 协同区配置）。
 */
@Data
@Schema(name = "协同区保存", description = "协同区保存参数")
public class PortalSynergyAreaSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键（更新时必填，新增留空）")
	private Long id;

	@Schema(name = "协同区类型：workflow流程 / form流程表单 / node流程节点", requiredMode = Schema.RequiredMode.REQUIRED)
	private String areaType;

	@Schema(name = "显示标题")
	private String title;

	@Schema(name = "绑定流程定义ID（字符串，兼容雪花ID）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String bindId;

	@Schema(name = "绑定流程名称（保存时快照）")
	private String bindName;

	@Schema(name = "绑定节点ID（仅 node 类型）")
	private String nodeId;

	@Schema(name = "绑定节点名称")
	private String nodeName;

	@Schema(name = "绑定表单ID（仅 form 类型）")
	private String formId;

	@Schema(name = "是否启用（1启用 0禁用）")
	private Integer enabled;

	@Schema(name = "显示宽度（栅格 1-24）")
	private Integer width;

	@Schema(name = "扩展参数JSON（如展示字段键列表/排序/最大条数）")
	private String paramsJson;
}
