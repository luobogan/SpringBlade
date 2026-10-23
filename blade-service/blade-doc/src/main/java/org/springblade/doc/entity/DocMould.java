package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 显示模板（对应 E9 DocMould）。
 *
 * <p>mouldType：0HTML 2WORD 3EXCEL 4WPS；编辑模板族为 DocMouldfile（后续接入）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_mould")
@Schema(description = "显示模板")
public class DocMould extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板名")
    private String mouldName;

    @Schema(description = "模板类型 0HTML 2WORD 3EXCEL 4WPS")
    private Integer mouldType;

    @Schema(description = "是否默认")
    private Integer isDefault;

    @Schema(description = "模板内容文件ID")
    private Long imageFileId;

    @Schema(description = "模板路径")
    private String mouldPath;
}
