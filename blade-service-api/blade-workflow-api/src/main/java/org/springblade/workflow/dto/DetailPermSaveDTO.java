package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springblade.workflow.vo.DetailPermVO;

import java.io.Serializable;
import java.util.List;

/**
 * 节点明细表权限保存请求
 *
 * <p>对应 {@code PUT /definition/{id}/node/{nodeKey}/detail-perm}。</p>
 */
@Data
@Schema(description = "节点明细表权限保存请求")
public class DetailPermSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "节点Key不能为空")
    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "明细表权限列表")
    private List<DetailPermVO> perms;

}
