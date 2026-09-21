package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 节点「签字意见显示设置」（屏显口径；打印口径见 {@link PrintSetVO}）。
 *
 * <p>对齐 ecology「签字意见显示设置」：控制审批态流转记录里意见块的呈现方式。</p>
 */
@Data
@Schema(description = "节点签字意见显示设置")
public class OpinionDisplayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "显示全部意见 1=显示全部 0=仅显示最后一次（默认 0）")
    private Integer viewTypeAll;

    @Schema(description = "显示方式 0=按时间倒序 1=按节点正序（默认 0）")
    private Integer showType;

    @Schema(description = "意见分栏列数 1/2/3（默认 1）")
    private Integer remarkColumn;

    @Schema(description = "不显示空意见 1=开启 0=关闭（默认 0）")
    private Integer stNull;

    @Schema(description = "意见类型显示白名单（键如 approve/reject/submit/forward/circulate/sign）；空=全部显示")
    private List<String> viewTypes;

}
