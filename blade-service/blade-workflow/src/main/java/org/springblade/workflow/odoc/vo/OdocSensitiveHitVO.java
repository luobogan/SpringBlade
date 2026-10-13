package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 敏感词命中项。
 */
@Data
@Schema(description = "敏感词命中")
public class OdocSensitiveHitVO {

	@Schema(description = "命中的词")
	private String word;

	@Schema(description = "分类")
	private String category;

	@Schema(description = "在文本中的起始下标")
	private Integer index;

	public OdocSensitiveHitVO() {
	}

	public OdocSensitiveHitVO(String word, String category, Integer index) {
		this.word = word;
		this.category = category;
		this.index = index;
	}
}
