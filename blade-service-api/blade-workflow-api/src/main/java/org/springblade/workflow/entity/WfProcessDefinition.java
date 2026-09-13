package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程定义实体
 *
 * <p>对齐 ecology {@code workflow_base}；status：0草稿 1已发布 2停用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_definition")
@Schema(description = "流程定义")
public class WfProcessDefinition extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎流程Key（= BPMN process id）")
    private String procKey;

    @Schema(description = "关联 workflow_bill.id（表单）")
    private Long formId;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）")
    private String bpmnXml;

    @Schema(description = "版本号")
    private Integer version;

    /**
     * 版本组锚点（对齐 ecology workflow_base.activeVersionID）。
     *
     * <p>同一流程的多个版本（同 procKey，version 递增）各自一行记录，
     * 组内每行的该字段都指向「当前激活版本」的 defId；首版 = 自身 id，
     * NULL = 单版本流程（组 = 自身）。版本切换/激活 = 组内改锚点；
     * 新发起实例始终使用激活版本，在途实例由 Flowable 按部署时的
     * ACT_RE_PROCDEF.ID_ 原生隔离，不受新版本影响。</p>
     */
    @Schema(description = "版本组锚点：指向当前激活版本的 defId；首版=自身id，NULL=单版本流程（组=自身）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long activeVersionId;

    @Schema(description = "是否自由流程")
    private Integer isFree;

    @Schema(description = "自由流程类型：1简易 2高级（对齐 ecology newFreeWfType）")
    private Integer freeWfType;

    @Schema(description = "路径类型（对齐 ecology path_type 字典 code）")
    private String type;

    @Schema(description = "对应表单类型：0自定义表单 1系统表单")
    private Integer formType;

    @Schema(description = "路径描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

    /**
     * 主键以字符串形式序列化。
     *
     * <p>主键为 ASSIGN_ID 雪花 ID（19 位），若以 JSON 数字返回，前端 JS 解析会丢失精度，
     * 导致编辑保存时 {@code updateById} 用失真主键命中 0 行（后端仍回 success，表现为「假成功」）。
     * 对齐 BladeX 标准实体做法：id 一律转字符串。</p>
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
