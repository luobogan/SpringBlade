package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.workflow.entity.WfFreeNode;

/**
 * 自由流程实例级步骤 Mapper。
 *
 * <p>查询一律以 {@code inst_id} 为前导列（命中 uk_inst_order / uk_inst_node 左前缀），
 * 不按 node_key 单列过滤，避免全表扫描。</p>
 */
public interface WfFreeNodeMapper extends BaseMapper<WfFreeNode> {

    /**
     * 整包替换前的物理删除。
     *
     * <p>必须物理删除：全局逻辑删除会让 {@code uk_inst_order(inst_id,flow_order)} /
     * {@code uk_inst_node(inst_id,node_key)} 仍被旧行占用，新行插入即唯一键冲突。</p>
     */
    @Delete("DELETE FROM wf_free_node WHERE inst_id = #{instId}")
    int physicalDeleteByInstId(@Param("instId") Long instId);
}
