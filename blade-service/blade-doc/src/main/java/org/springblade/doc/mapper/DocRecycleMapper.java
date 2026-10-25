package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.doc.entity.DocRecycle;

/**
 * 回收站 Mapper。
 */
@Mapper
public interface DocRecycleMapper extends BaseMapper<DocRecycle> {

    /**
     * 按文档ID物理删除回收站登记行。
     *
     * <p>回收站是「删除行为的登记簿」，记录本身没有再被查询/恢复的价值：
     * 若用 MP 的 delete 会因 @TableLogic 变成逻辑删除，则「删除→恢复→删除」每轮
     * 都会堆积一行 is_deleted=1 的垃圾记录，故此处物理删除。</p>
     */
    @Delete("DELETE FROM doc_recycle WHERE doc_id = #{docId}")
    int deletePhysicallyByDocId(@Param("docId") Long docId);
}
