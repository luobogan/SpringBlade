package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 自定义操作「动作明细」（URL / 流程操作 / 接口），支持 {@code $field$} 占位符。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_custom_operation_action")
@Schema(description = "自定义操作动作明细")
public class WfCustomOperationAction extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联 wf_custom_operation.id")
    private Long opId;

    @Schema(description = "动作类型=URL 时的地址（支持 $field$ 占位符）")
    private String url;

    @Schema(description = "URL 请求方法")
    private String httpMethod;

    @Schema(description = "动作类型=流程操作时的动作标识（对齐 WfCustomAction 注册 key）")
    private String flowOperation;

    @Schema(description = "动作类型=接口时的接口名（扩展位）")
    private String interfaceName;

    @Schema(description = "参数表达式（支持 $field$ 占位符）")
    private String paramExpr;

    @Schema(description = "执行后写入的签字意见")
    private String opinion;

}
