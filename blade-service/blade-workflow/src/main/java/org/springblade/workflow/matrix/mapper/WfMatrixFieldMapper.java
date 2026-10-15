package org.springblade.workflow.matrix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.workflow.matrix.entity.WfMatrixField;

/**
 * 矩阵字段定义 Mapper
 */
public interface WfMatrixFieldMapper extends BaseMapper<WfMatrixField> {

	/**
	 * 物理删除某矩阵的全部字段定义。
	 *
	 * <p>⚠️ {@link WfMatrixField} 继承 TenantEntity，MyBatis-Plus 的 {@code delete()}
	 * 受 {@code @TableLogic} 影响为软删除（UPDATE is_deleted=1）。而
	 * {@code uk_wf_matrix_field_key(matrix_id, field_key)} 不含 is_deleted，
	 * 软删除的行仍占用唯一键，导致覆盖式重存时 INSERT 撞唯一键
	 * （Duplicate entry '...-div' for key 'uk_wf_matrix_field_key'）。</p>
	 *
	 * <p>覆盖式保存必须先物理删除，故用原生 SQL 绕过 @TableLogic。
	 * 租户拦截器会自动追加 tenant_id 条件，不会越权删。</p>
	 */
	@Delete("DELETE FROM wf_matrix_field WHERE matrix_id = #{matrixId}")
	int physicalDeleteByMatrixId(@Param("matrixId") Long matrixId);
}
