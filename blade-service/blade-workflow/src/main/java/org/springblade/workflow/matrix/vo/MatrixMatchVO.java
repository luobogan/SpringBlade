package org.springblade.workflow.matrix.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 矩阵条件匹配结果（供 {@code WfOperatorResolver} 的矩阵分支消费）
 *
 * <p>除办理人集合外，刻意带上「生效条件 / 命中行数 / 矩阵名」，用于满足
 * E9 缺陷教训 #10：解析失败或为空时必须能带上下文定位，而非静默吞掉
 * （E9 证据：{@code MartrixItem.java:262-280} 仅 printStackTrace）。</p>
 */
@Data
@Schema(description = "矩阵条件匹配结果")
public class MatrixMatchVO {

	@Schema(description = "矩阵ID")
	private Long matrixId;

	@Schema(description = "矩阵名称")
	private String matrixName;

	@Schema(description = "结果维度字段Key")
	private String valueFieldKey;

	/** 生效的条件（condFieldKey -&gt; 取值），已剔除空值条件 */
	@Schema(description = "生效条件 condFieldKey -> 取值")
	private Map<String, String> effectiveConditions = new LinkedHashMap<>();

	@Schema(description = "命中的数据行数")
	private int hitRows;

	@Schema(description = "解析出的办理人ID（去重保序）")
	private List<Long> userIds = new ArrayList<>();

	public MatrixMatchVO() {
	}

	public MatrixMatchVO(Long matrixId, String valueFieldKey, Map<String, String> conditions) {
		this.matrixId = matrixId;
		this.valueFieldKey = valueFieldKey;
		if (conditions != null) {
			this.effectiveConditions = new LinkedHashMap<>(conditions);
		}
	}

	public boolean isEmpty() {
		return userIds == null || userIds.isEmpty();
	}

	/**
	 * 可读诊断串：供 log.error 与异常文案。
	 *
	 * <p>刻意包含矩阵名、结果维度、每个条件的字段与取值、命中行数 ——
	 * 这是 E9 缺失而本项目必须补齐的可观测性。</p>
	 */
	public String describe() {
		return "matrixId=" + matrixId
			+ ", matrixName=" + matrixName
			+ ", valueField=" + valueFieldKey
			+ ", conditions=" + effectiveConditions
			+ ", hitRows=" + hitRows
			+ ", users=" + userIds;
	}
}
