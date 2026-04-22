package org.springblade.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量导入DTO
 *
 * @author youpinmall
 * @date 2024-01-01
 */
@Data
@Schema(description = "批量导入DTO")
public class BatchImportDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 导入类型
	 */
	@Schema(description = "导入类型")
	private String type;

	/**
	 * 导入数据列表
	 */
	@NotEmpty(message = "导入数据不能为空")
	@Schema(description = "导入数据列表")
	private List<ImportItem> items;

	/**
	 * 导入项
	 */
	@Data
	public static class ImportItem implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * 商品名称
		 */
		@Schema(description = "商品名称")
		private String name;

		/**
		 * 商品描述
		 */
		@Schema(description = "商品描述")
		private String description;

		/**
		 * 分类ID
		 */
		@Schema(description = "分类ID")
		private Long categoryId;

		/**
		 * 品牌ID
		 */
		@Schema(description = "品牌ID")
		private Long brandId;

		/**
		 * 价格
		 */
		@Schema(description = "价格")
		private java.math.BigDecimal price;

		/**
		 * 原价
		 */
		@Schema(description = "原价")
		private java.math.BigDecimal originalPrice;

		/**
		 * 库存
		 */
		@Schema(description = "库存")
		private Integer stock;

		/**
		 * 主图
		 */
		@Schema(description = "主图")
		private String image;

		/**
		 * 图片列表
		 */
		@Schema(description = "图片列表")
		private List<String> images;

		/**
		 * 颜色列表
		 */
		@Schema(description = "颜色列表")
		private List<String> colors;

		/**
		 * 尺码列表
		 */
		@Schema(description = "尺码列表")
		private List<String> sizes;
	}

}
