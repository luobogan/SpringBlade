package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人布局项（读/存共用）
 */
@Data
@Schema(name = "个人布局项", description = "用户对某元素的排列覆盖")
public class PortalLayoutItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "元素ID")
	private Long elementId;

	@Schema(name = "行号")
	private Integer rowNo;

	@Schema(name = "栅格宽（antd 24 栅格）")
	private Integer colSpan;

	@Schema(name = "排序号")
	private Integer sortOrder;

	@Schema(name = "是否隐藏（0否 1是）")
	private Integer hidden;
}
