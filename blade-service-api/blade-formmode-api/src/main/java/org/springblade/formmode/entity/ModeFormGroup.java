package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 布局组实体（对应 modeformgroup 表）
 */
@Data
@TableName("modeformgroup")
@Schema(description = "布局组")
public class ModeFormGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "组ID")
    private Long id;

    @Schema(description = "模块ID")
    private Integer modeid;

    /**
     * 表单ID（设计器定位维度）
     *
     * <p>说明：Excel 设计器只有 formId（布局按表单归属），拿不到 modeid，
     * 因此明细组属性按「formId + 明细表序号」定位；modeid 保留模块维度（可为空）。</p>
     */
    @Schema(description = "表单ID（设计器按 formId + dsporder 定位明细组）")
    private Long formId;

    @Schema(description = "组名称")
    private String groupname;

    @Schema(description = "组类型：1主表 2明细表")
    private Integer grouptype;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "显示条件")
    private String showcondition;

    // ===== 明细表组属性（对齐泛微 E9 modeformgroup，设计器侧称 dtladd/dtledit/dtldelete/dtlcopy） =====
    // 说明：这些是**设计期**开关（该明细表是否允许新增/编辑/删除/复制），
    // 与「流程节点级明细权限」(wf:detailTablePerm) 不是同一层，勿混用。

    @Schema(description = "是否允许新增明细行（1是 0否），对齐 ecology isadd / 设计器 dtladd")
    private Integer isadd;

    @Schema(description = "是否允许编辑明细行（1是 0否），对齐 ecology isedit / 设计器 dtledit")
    private Integer isedit;

    @Schema(description = "是否允许删除明细行（1是 0否），对齐 ecology isdelete / 设计器 dtldelete")
    private Integer isdelete;

    @Schema(description = "是否允许复制明细行（1是 0否），对齐 ecology iscopy / 设计器 dtlcopy")
    private Integer iscopy;

    @Schema(description = "打印时是否显示明细序号（1是 0否），对齐 ecology isprintserial")
    private Integer isprintserial;

    @Schema(description = "明细表是否允许横向滚动（1是 0否），对齐 ecology allowscroll")
    private Integer allowscroll;

    @Schema(description = "租户ID")
    private String tenantId;

}
