package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 表单布局视图对象（供跨服务 Feign 调用，如 blade-workflow 组装审批态渲染包）。
 *
 * <p>仅承载渲染所需的必要字段（id / formId / 布局 JSON），避免 Feign 契约
 * 依赖 service 模块的 Entity（分层约束：api 模块不可引用 service 实体）。</p>
 */
@Data
@Schema(description = "表单布局视图对象")
public class FormLayoutVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "布局ID")
	private Long id;

	@Schema(description = "表单ID")
	private Long formId;

	@Schema(description = "布局名称")
	private String layoutName;

	@Schema(description = "布局JSON")
	private String layoutJson;

	@Schema(description = "布局配置")
	private String layoutConfig;

	@Schema(description = "布局类型：0编辑(默认) 1显示 3监控 4打印")
	private Integer layoutType;

	@Schema(description = "绑定流程节点Key（空=通用）")
	private String nodeKey;

}
