package org.springblade.formmode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.formmode.entity.FieldExtend;

/**
 * 字段扩展属性 Mapper 接口
 * 对应数据库表：mode_form_field_extend
 */
@Mapper
public interface FieldExtendMapper extends BaseMapper<FieldExtend> {
}
