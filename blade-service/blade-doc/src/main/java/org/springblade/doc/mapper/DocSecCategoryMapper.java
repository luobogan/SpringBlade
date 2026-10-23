package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.doc.entity.DocSecCategory;

/**
 * 文档子目录 Mapper（分部隔离同 DocDetailMapper，经 scopeClass 自动追加）。
 */
@Mapper
public interface DocSecCategoryMapper extends BaseMapper<DocSecCategory> {
}
