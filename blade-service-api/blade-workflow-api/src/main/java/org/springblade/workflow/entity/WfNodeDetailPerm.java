package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 节点级明细表权限
 *
 * <p>对齐 ecology {@code workflow_nodeformgroup}（其前端以 10 位定长权限串
 * {@code detailgroupattr} 传递：新增/编辑/删除/隐藏空行/默认新增/必须新增/
 * SAP多行/打印序号/允许滚动/开启分页）。本项目按语义拆为独立列。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_detail_perm")
@Schema(description = "节点级明细表权限")
public class WfNodeDetailPerm extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "明细表序号")
    private Integer dtIndex;

    @Schema(description = "可新增行")
    private Integer canAdd;

    @Schema(description = "可编辑行")
    private Integer canEdit;

    @Schema(description = "可删除行")
    private Integer canDelete;

    @Schema(description = "隐藏空行")
    private Integer hideEmpty;

    @Schema(description = "默认行数")
    private Integer defaultRows;

    @Schema(description = "必须至少一条")
    private Integer required;

}
