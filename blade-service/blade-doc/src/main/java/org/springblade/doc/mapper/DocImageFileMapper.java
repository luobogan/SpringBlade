package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.doc.entity.DocImageFile;

/**
 * 文档文件关联 Mapper。
 */
@Mapper
public interface DocImageFileMapper extends BaseMapper<DocImageFile> {
}
