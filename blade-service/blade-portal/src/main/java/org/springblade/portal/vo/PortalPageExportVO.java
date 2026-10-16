package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.entity.PortalPageShare;

import java.io.Serializable;
import java.util.List;

/**
 * 门户页面导出 VO（对标泛微 portalimport 导出能力，见分析 §13.4）
 * <p>包含页面 + 元素 + 授权的完整配置，可直接序列化后经导入接口落地为新页面。</p>
 */
@Data
@Schema(name = "门户页面导出VO", description = "页面+元素+授权的完整配置（用于导入/导出）")
public class PortalPageExportVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "页面配置")
	private PortalPage page;

	@Schema(name = "元素配置列表")
	private List<PortalElement> elements;

	@Schema(name = "授权配置列表")
	private List<PortalPageShare> shares;
}
