package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springblade.workflow.vo.DetailFilterVO;

import java.io.Serializable;
import java.util.List;

/**
 * 节点明细表字段筛选保存请求
 *
 * <p>对应 {@code PUT /definition/{id}/node/{nodeKey}/detail-filter?modeType=1|2}，整体覆盖某一口径。</p>
 */
@Data
@Schema(description = "节点明细表字段筛选保存请求")
public class DetailFilterSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "节点Key不能为空")
    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "过滤口径 1=显示时过滤 2=打印时过滤")
    private Integer modeType;

    @Schema(description = "筛选规则列表（多条规则间为「且」关系）")
    private List<DetailFilterVO> rules;

}
