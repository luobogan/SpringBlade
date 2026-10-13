package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 模板书签定义（对应 E9 MouldBookMark）
 *
 * <p>name 为书签名/占位符（如 fld_Mark / Content）；isContent=1 表示正文占位书签，
 * 套红时不作字段替换、而是插入正文内容。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_mould_bookmark")
@Schema(description = "模板书签定义")
public class WfOdocMouldBookmark extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long mouldId;

    @Schema(description = "书签名/占位符,如 fld_Mark / Content")
    private String name;

    @Schema(description = "描述,如 [发文文号]")
    private String descript;

    @Schema(description = "映射Gbt9704Profile.ElementType")
    private String elementType;

    @Schema(description = "显示顺序")
    private Integer showOrder;

    @Schema(description = "是否正文占位书签(1=不替换,插入正文)")
    private Integer isContent;

}
