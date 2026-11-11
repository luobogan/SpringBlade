package org.springblade.formmode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.formmode.entity.WorkflowFormula;

/**
 * 自定义函数 Mapper 接口
 * 对应数据库表：workflow_formula
 */
@Mapper
public interface WorkflowFormulaMapper extends BaseMapper<WorkflowFormula> {
}
