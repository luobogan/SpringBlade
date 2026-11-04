package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.workflow.entity.WfFreeLink;

/**
 * 自由流程实例级链路 Mapper。
 */
public interface WfFreeLinkMapper extends BaseMapper<WfFreeLink> {

    /** 整包替换 / 实例删除时的物理删除（同 WfFreeNodeMapper 的唯一键原因） */
    @Delete("DELETE FROM wf_free_link WHERE inst_id = #{instId}")
    int physicalDeleteByInstId(@Param("instId") Long instId);
}
