package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * GB/T 9704 版式规则库（一切版式能力的地基，P0 前置项）
 *
 * <p>profileJson 序列化 Gbt9704Profile（版心尺寸、要素坐标、字体字号、文种版式等），
 * 可按机关（orgScope）与文种定制；不依赖任何厂商 SDK 或 AI。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_gbt_profile")
@Schema(description = "GB/T9704版式规则库")
public class WfOdocGbtProfile extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "profile名称,如 默认/XX市政府/党委机关")
    private String profileName;

    @Schema(description = "适用机关(空=全局)")
    private String orgScope;

    @Schema(description = "标准版本")
    private String standardVer;

    @Schema(description = "Gbt9704Profile 序列化 JSON")
    private String profileJson;

    @Schema(description = "是否启用")
    private Integer enabled;

}
