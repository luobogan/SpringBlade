package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 触发工作流设置实体（对应 mode_triggerworkflowset 表）
 */
@Data
@TableName("mode_triggerworkflowset")
@Schema(description = "触发工作流设置")
public class ModeTriggerWorkflowSet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "设置ID")
    private Long id;

    @Schema(description = "模块ID（modeinfo.id，BIGINT 雪花）")
    private Long modeid;

    @Schema(description = "触发操作：0新建 1编辑 2删除 3全部")
    private Integer triggeropt;

    @Schema(description = "工作流ID（wf_process_definition.id，BIGINT 雪花）")
    private Long workflowid;

    @Schema(description = "工作流名称")
    private String workflowname;

    /**
     * 流程定义Key（BPMN process id）。
     *
     * <p>「以 Flowable 为唯一事实源」改造：触发绑定从 {@link #workflowid}（wf_process_definition.id）
     * 迁移到本字段（引擎原生身份）。为空时回退到 {@link #workflowid}（兼容存量）。</p>
     */
    @TableField("workflow_key")
    private String workflowKey;

    @Schema(description = "是否启用：1启用 0禁用")
    private Integer status;

    @Schema(description = "显示条件（JSON）")
    private String showcondition;

    @Schema(description = "排序号")
    private Integer dsporder;

    @Schema(description = "租户ID")
    private String tenantId;

}
