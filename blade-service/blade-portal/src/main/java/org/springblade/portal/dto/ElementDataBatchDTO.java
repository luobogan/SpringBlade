package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 元素数据批量取数入参
 * <p>
 * 两种口径（前端优先用 {@link #elements} 元素维度）：
 * <ul>
 *   <li><b>元素维度（推荐）</b>：{@code elements:[{elementId,type,perPage}]} —— 一次携带本页元素，
 *       服务端按 elementId 分别取数，返回 {@code Map<elementId, ElementDataVO>}。
 *       解决「同页多个同类型元素共用首个元素配置」与「用户个性化 perPage 调多大都不生效」两个缺陷
 *       （§22 断点 #5/#6）。perPage 由前端从已合并个性化的 /my-portal 元素上带入。</li>
 *   <li><b>类型维度（兼容旧）</b>：{@code types:[...]} —— 按 type 去重取数，返回 {@code Map<type, ElementDataVO>}。</li>
 * </ul>
 * </p>
 */
@Data
@Schema(name = "元素数据批量取数入参", description = "按元素维度（推荐）或类型维度批量取数")
public class ElementDataBatchDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "元素类型集合（去重，兼容旧口径）")
	private List<String> types;

	@Schema(name = "每个类型取多少条（为空取元素配置 perPage 或默认值，仅 types 口径生效）")
	private Integer limit;

	@Schema(name = "页面ID（可选，用于读取页面内元素的配置）")
	private Long pageId;

	@Schema(name = "元素维度取数（推荐）：一次携带本页元素，按 elementId 分别取数")
	private List<ElementRef> elements;

	/**
	 * 元素引用（元素维度取数）
	 */
	@Data
	@Schema(name = "元素引用", description = "元素维度取数的单项")
	public static class ElementRef implements Serializable {

		private static final long serialVersionUID = 1L;

		@Schema(name = "元素ID")
		private Long elementId;

		@Schema(name = "元素类型")
		private String type;

		@Schema(name = "该元素取数条数（前端已合并个性化 perPage；为空回落元素配置 perPage）")
		private Integer perPage;
	}
}
