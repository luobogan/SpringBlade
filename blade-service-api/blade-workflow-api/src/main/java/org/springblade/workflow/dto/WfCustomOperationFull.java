package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.workflow.entity.WfCustomOperation;
import org.springblade.workflow.entity.WfCustomOperationAction;
import org.springblade.workflow.entity.WfCustomOperationRight;

import java.util.List;

/**
 * 自定义操作「完整配置」传输对象：按钮 + 动作明细 + 权限矩阵。
 *
 * <p>用于保存/读取一条自定义操作及其关联的动作与权限（前端「自定义操作」三级弹窗）。</p>
 */
@Data
@Schema(description = "自定义操作完整配置")
public class WfCustomOperationFull {

    @Schema(description = "按钮配置")
    private WfCustomOperation op;

    @Schema(description = "动作明细")
    private WfCustomOperationAction action;

    @Schema(description = "权限矩阵")
    private List<WfCustomOperationRight> rights;

}
