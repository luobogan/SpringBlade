package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springblade.workflow.vo.FieldPermVO;

import java.io.Serializable;
import java.util.List;

/**
 * 节点字段权限保存请求
 *
 * <p>对应 {@code PUT /definition/{id}/node/{nodeKey}/field-perm}。
 * 以 defId + nodeKey 为维度整体覆盖保存（先删后插，保证与设计器勾选结果一致）。</p>
 */
@Data
@Schema(description = "节点字段权限保存请求")
public class FieldPermSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "节点Key不能为空")
    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "权限矩阵")
    private List<FieldPermVO> perms;

}
