package org.springblade.workflow.matrix.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 矩阵数据行
 *
 * <p>一行 = 一组条件值 + 各结果维度人员。{@code dataJson} 结构：
 * {@code { "<fieldKey>": "值", ... }}，fieldKey 即 {@link WfMatrixField#getFieldKey()}。</p>
 *
 * <ul>
 *   <li>条件维度值：单值或逗号分隔多值</li>
 *   <li>结果维度值：逗号分隔的人员ID串</li>
 * </ul>
 *
 * <p>不采用 E9 的「每矩阵动态建表 {@code Matrixtable_<id>}」，改用统一表 + JSON：
 * 多租户/迁移/备份/跨矩阵统计成本恒定；代价是 JSON 内字段无 DB 约束，
 * 一致性由应用层校验保证（见 {@code wf_matrix.sql} 头部说明）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wf_matrix_data", autoResultMap = true)
@Schema(description = "矩阵数据行")
public class WfMatrixData extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "所属矩阵 wf_matrix.id")
	private Long matrixId;

	@Schema(description = "行序（同一矩阵内唯一，升序即优先级；修复E9 dataorder可空可重复缺陷）")
	private BigDecimal dataOrder;

	/**
	 * 维度值 JSON：{@code { "<fieldKey>": "值" }}
	 *
	 * <p>用 {@link LinkedHashMap} 保序，便于导入导出与调试时稳定复现。</p>
	 */
	@Schema(description = "维度值 JSON {fieldKey: 值}")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> dataJson = new LinkedHashMap<>();
}
