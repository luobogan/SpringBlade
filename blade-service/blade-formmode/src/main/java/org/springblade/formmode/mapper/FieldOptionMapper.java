package org.springblade.formmode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.formmode.entity.FieldOption;

/**
 * 字段选项配置 Mapper 接口
 * 对应数据库表：mode_form_field_option
 */
@Mapper
public interface FieldOptionMapper extends BaseMapper<FieldOption> {
}
