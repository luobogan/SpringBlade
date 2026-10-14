package org.springblade.workflow.matrix.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 矩阵主表
 *
 * <p>对齐泛微 E9 {@code MatrixInfo}：一个矩阵 = 一组条件维度 + 若干结果维度 + 多行数据，
 * 用于流程节点操作者（opType=99）按表单/系统字段值动态解析办理人。</p>
 *
 * <p>{@code isSystem}：0=自定义矩阵，1=分部系统矩阵，2=部门系统矩阵（对齐 E9 {@code issystem}）。
 * P0 只支持自定义矩阵；系统矩阵的分部/部门字段定义联动留待 P3。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_matrix")
@Schema(description = "矩阵主表")
public class WfMatrix extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "矩阵名称")
	private String matrixName;

	@Schema(description = "矩阵描述")
	private String matrixDesc;

	@Schema(description = "0=自定义 1=分部系统矩阵 2=部门系统矩阵")
	private Integer isSystem;

	@Schema(description = "状态 1启用 0停用")
	private Integer status;
}
