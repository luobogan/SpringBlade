package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 节点级字段权限矩阵
 *
 * <p>对齐 ecology {@code workflow_nodeform}，但按本项目 scope 体系落地：</p>
 * <ul>
 *   <li>scope：{@code main} | {@code dt{idx}} | {@code dt{idx}_r{row}}（与 {@code data-excelp-scope} 对齐；
 *       {@code dt{idx}_r{row}} 为 B5 行级授权，渲染端按 {@code dt{idx}_r{row}} → {@code dt{idx}} → {@code main} 回退）</li>
 *   <li>field_name：与 {@code data-excelp-field} 对齐</li>
 *   <li>perm：0隐藏 1只读 2可编辑 3必填（对齐 ecology fieldattr）
 *       —— 注意本项目前端 {@code fieldAttr} 原本只有 1/2/3，此处补 0=隐藏</li>
 * </ul>
 * <p>渲染优先级：节点权限 &gt; 单元格 fieldAttr &gt; Excel 样式 &gt; 默认。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_field_perm")
@Schema(description = "节点级字段权限矩阵")
public class WfNodeFieldPerm extends TenantEntity {

    /** 隐藏 */
    public static final int PERM_HIDDEN = 0;
    /** 只读 */
    public static final int PERM_READONLY = 1;
    /** 可编辑 */
    public static final int PERM_EDITABLE = 2;
    /** 必填 */
    public static final int PERM_REQUIRED = 3;

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "作用域：main | dt{idx} | dt{idx}_r{row}（与 data-excelp-scope 对齐）")
    private String scope;

    @Schema(description = "字段名（与 data-excelp-field 对齐）")
    private String fieldName;

    @Schema(description = "0隐藏 1只读 2可编辑 3必填（对齐 ecology fieldattr）")
    private Integer perm;

    // ---------------- 三维度（对齐 ecology workflow_nodeform 的 isview / iseditable / ismandatory）----------------
    // 由 V2026.09.21_001__wf_node_field_perm_three_dims.sql 引入；perm 保留为「兼容派生列」。
    // 三列**全为 NULL** 视为存量行 → 读取时按 perm 现推；否则以三列为准。写库时双向都写。

    @Schema(description = "字段是否显示 1=显示 0=隐藏；NULL=存量行（按 perm 推导）")
    @TableField("is_visible")
    private Integer isVisible;

    @Schema(description = "字段是否可编辑 1=可编辑 0=只读；NULL=存量行（按 perm 推导）")
    @TableField("is_editable")
    private Integer isEditable;

    @Schema(description = "字段是否必填 1=必填 0=非必填；NULL=存量行（按 perm 推导）")
    @TableField("is_required")
    private Integer isRequired;

}
