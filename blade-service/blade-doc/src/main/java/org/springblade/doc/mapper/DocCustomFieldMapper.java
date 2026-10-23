package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.doc.entity.DocCustomField;

/**
 * 自定义字段定义 Mapper。
 */
@Mapper
public interface DocCustomFieldMapper extends BaseMapper<DocCustomField> {
}
