package org.springblade.workflow.matrix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.workflow.matrix.entity.WfMatrixData;

import java.math.BigDecimal;

/**
 * 矩阵数据行 Mapper
 */
public interface WfMatrixDataMapper extends BaseMapper<WfMatrixData> {

	/**
	 * 按 (matrix_id, data_order) 物理删除（含软删除行）。
	 *
	 * <p>⚠️ {@link WfMatrixData} 继承 TenantEntity，{@code delete()} 受 {@code @TableLogic}
	 * 影响为软删除，而 {@code uk_wf_matrix_data_order(matrix_id, data_order)} 不含 is_deleted，
	 * 软删除行仍占用唯一键 → 复用同一行序重存会撞键。覆盖/清理残留需物理删除。</p>
	 */
	@Delete("DELETE FROM wf_matrix_data WHERE matrix_id = #{matrixId} AND data_order = #{dataOrder}")
	int physicalDeleteByOrder(@Param("matrixId") Long matrixId, @Param("dataOrder") BigDecimal dataOrder);

	/** 物理删除单行（用于删除数据行时立即释放行序，避免软删除残留占用唯一键）。 */
	@Delete("DELETE FROM wf_matrix_data WHERE id = #{dataId}")
	int physicalDeleteById(@Param("dataId") Long dataId);
}
