package org.springblade.formmode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.formmode.entity.WorkflowBillField;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 表单字段定义 Mapper
 */
public interface WorkflowBillFieldMapper extends BaseMapper<WorkflowBillField> {

    /**
     * 根据表单ID查询字段列表，按排序顺序
     */
    @Select("SELECT * FROM workflow_billfield WHERE billid = #{billId} ORDER BY dsporder ASC")
    List<WorkflowBillField> selectByBillId(@Param("billId") Integer billId);

}
