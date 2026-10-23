package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springblade.doc.entity.DocPraise;

/** 文档点赞 Mapper。 */
@Mapper
public interface DocPraiseMapper extends BaseMapper<DocPraise> {
}
