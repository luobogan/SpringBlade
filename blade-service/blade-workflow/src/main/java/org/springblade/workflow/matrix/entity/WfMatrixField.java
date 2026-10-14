package org.springblade.workflow.matrix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 矩阵字段定义（条件维度 / 结果维度）
 *
 * <p>对齐泛微 E9 {@code MatrixFieldInfo}：{@code fieldType} 0=条件维度（黄），
 * 1=结果维度（蓝，多为多人力资源岗位）。多个条件维度之间是 <b>AND</b> 关系。</p>
 *
 * <p>{@code browserType} 对齐 E9 {@code browsertypeid}：1人力资源 / 17多人力资源 /
 * 4、57部门 / 164、194分部。</p>
 *
 * <p>⚠️ {@code fieldKey} 是应用层唯一键（{@code data_json} 的 key），<b>不是物理列名</b>
 * —— 本实现用统一表 + JSON，不动态建表。但仍须按白名单校验，避免与 E9 同款的
 * 注入/保留字/同名列问题（见落地文档 10.3 #6）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_matrix_field")
@Schema(description = "矩阵字段定义")
public class WfMatrixField extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "所属矩阵 wf_matrix.id")
	private Long matrixId;

	@Schema(description = "字段唯一键（data_json 的 key，应用层唯一）")
	private String fieldKey;

	@Schema(description = "字段显示名")
	private String displayName;

	@Schema(description = "浏览按钮类型 1人力资源 17多人力资源 4/57部门 164/194分部")
	private Integer browserType;

	@Schema(description = "0=条件维度 1=结果维度")
	private Integer fieldType;

	@Schema(description = "字段排序")
	private Integer fieldOrder;

	@Schema(description = "列宽")
	private Integer colWidth;
}
