package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 打印/呈批设置（对应 E9 workflow_printset，按节点）
 *
 * <p>配置某节点生效的打印/呈批模板（mould_type=3）与流转意见打印策略。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_print_set")
@Schema(description = "打印/呈批设置")
public class WfOdocPrintSet extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "生效节点,逗号分隔")
    private String nodeKeys;

    @Schema(description = "打印呈批模板(mould_type=3)")
    private Long mouldId;

    @Schema(description = "流转意见打印策略 2始终打印 0始终不打印 其它放入模板但不打印")
    private Integer printFlowComment;

    @Schema(description = "签字意见默认值")
    private String commentSignDefault;

}
