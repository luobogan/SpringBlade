package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 节点操作者实体
 *
 * <p>对齐 ecology {@code workflow_groupdetail}。</p>
 * <p>op_type 对齐 OperatorDBType：3人员 1部门 30分部 2角色 58岗位 4所有人 /
 * 17创建人本人 18创建人上级 19本部门 / 40本人 41上级 /
 * 5字段-人员 6字段-人员上级 42字段-部门 43字段-角色 / 99矩阵 97接口 98SQL。</p>
 * <p>bhxj：0本部 1含下级 2含上级 3逐级向上。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_operator")
@Schema(description = "节点操作者")
public class WfNodeOperator extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程节点ID")
    private Long nodeId;

    @Schema(description = "操作组序号")
    private Integer groupNo;

    @Schema(description = "操作者类型（对齐 OperatorDBType）")
    private Integer opType;

    @Schema(description = "对象ID或表单字段名")
    private String objId;

    @Schema(description = "安全级别下限")
    private Integer levelMin;

    @Schema(description = "安全级别上限")
    private Integer levelMax;

    @Schema(description = "0本部 1含下级 2含上级 3逐级向上")
    private Integer bhxj;

    @Schema(description = "会签关系")
    private Integer signOrder;

    @Schema(description = "批次（依次审批顺序）")
    private Integer batchNo;

    @Schema(description = "操作者生效条件")
    private String conditionJson;

    @Schema(description = "操作组名称")
    private String groupName;

    @Schema(description = "操作组可见性 1=可见 0=不可见")
    private Integer canView;

    @Schema(description = "是否协办/征询意见人 1=是 0=否")
    private Integer isCoadjutant;

    @Schema(description = "协办签字类型")
    private Integer signType;

    @Schema(description = "是否系统协办 1=是 0=否")
    private Integer isSysCoadjutant;

    @Schema(description = "提交时是否填写协办描述 1=是 0=否")
    private Integer isSubmitDesc;

    @Schema(description = "协办是否生成待办 1=是 0=否")
    private Integer isPending;

    @Schema(description = "协办是否可修改表单 1=是 0=否")
    private Integer isModify;

    @Schema(description = "协办/征询意见人（人员id串，逗号分隔）")
    private String coadjutants;

}
