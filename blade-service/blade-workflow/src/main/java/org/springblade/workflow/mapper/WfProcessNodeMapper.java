package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.workflow.entity.WfProcessNode;

/**
 * 流程节点 Mapper（流程模板的一部分，脱离租户隔离）
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfProcessNodeMapper extends BaseMapper<WfProcessNode> {

    /**
     * 查询节点（**含逻辑删除行**）。
     * <p>唯一索引 uk_def_node(def_id,node_key) 不含 is_deleted：节点被逻辑删除后行仍在、
     * 唯一键仍被占用，而 @TableLogic 会让常规查询看不到它（表现为「不存在」）→ 画布重新加回
     * 同一 nodeKey 时 insert 直接撞唯一键（DuplicateKeyException）。
     * 故 saveBpmn 的存在性判定必须查含已删除行。</p>
     */
    @Select("SELECT * FROM wf_process_node WHERE def_id = #{defId} AND node_key = #{nodeKey} LIMIT 1")
    WfProcessNode selectAnyByDefAndKey(@Param("defId") Long defId, @Param("nodeKey") String nodeKey);

    /**
     * 复活逻辑删除的节点。
     * <p>必须用原生 UPDATE：MP 的 @TableLogic 会给 updateById 附上 is_deleted = 0 条件，
     * 对被逻辑删除的行将更新 0 行。</p>
     */
    @Update("UPDATE wf_process_node SET is_deleted = 0, node_name = #{nodeName}, node_type = #{nodeType}, sign_order = #{signOrder}, sort_order = #{sortOrder} WHERE def_id = #{defId} AND node_key = #{nodeKey}")
    int reviveNode(@Param("defId") Long defId, @Param("nodeKey") String nodeKey,
                   @Param("nodeName") String nodeName, @Param("nodeType") Integer nodeType,
                   @Param("signOrder") Integer signOrder, @Param("sortOrder") Integer sortOrder);
}
