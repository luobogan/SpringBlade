package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.doc.entity.DocDummyDetail;

/**
 * 文档虚拟目录挂载 Mapper。
 */
@Mapper
public interface DocDummyDetailMapper extends BaseMapper<DocDummyDetail> {
}
