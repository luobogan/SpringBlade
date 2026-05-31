package org.springblade.formmode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.formmode.entity.FieldDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段定义 Mapper 接口
 */
@Mapper
public interface FieldDefinitionMapper extends BaseMapper<FieldDefinition> {
}
