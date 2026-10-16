package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 保存个人门户布局入参（按用户保存某门户页的元素排列，对标 hpcurrentuse）
 */
@Data
@Schema(name = "保存个人门户布局入参", description = "按用户保存某门户页的元素排列")
public class PortalLayoutSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "门户页面ID")
	private Long pageId;

	@Schema(name = "布局项集合（元素排列覆盖）")
	private List<PortalLayoutItem> items;
}
