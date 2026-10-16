package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 从元素库实例化到页面入参
 */
@Data
@Schema(name = "元素库实例化入参", description = "把库元素引用实例化为某页面的页面元素")
public class PortalElementLibInstantiateDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "库元素ID")
	private Long libId;

	@Schema(name = "目标页面ID")
	private Long pageId;

	@Schema(name = "行号（缺省 1）")
	private Integer rowNo;

	@Schema(name = "栅格宽度（缺省 12）")
	private Integer colSpan;
}
