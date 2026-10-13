package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文模板（对应 E9 docmould）
 *
 * <p>mould_type: 1编辑模板 2套红(显示)模板 3打印呈批模板 4智能排版版式；
 * gbtProfileId 绑定 GB/T 9704 版式规则，docTypeCode 对应 15 文种。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_mould")
@Schema(description = "公文模板")
public class WfOdocMould extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板名")
    private String mouldName;

    @Schema(description = "模板类型 1编辑 2套红 3打印 4版式")
    private Integer mouldType;

    @Schema(description = "模板文件ID(.docx,含书签)")
    private Long fileId;

    @Schema(description = "绑定的GB/T9704版式profile")
    private Long gbtProfileId;

    @Schema(description = "文种代码(对应15文种)")
    private String docTypeCode;

    @Schema(description = "所属流程定义(空=全局)")
    private Long defId;

    @Schema(description = "系统类型")
    private Integer systemType;

    @Schema(description = "是否启用")
    private Integer enabled;

}
