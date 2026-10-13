package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 正文书签实际值（对应 E9 DocMouldBookMark 实例值）
 *
 * <p>套红落地时记录每个书签的实际值，便于审计、回溯与复用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_doc_bookmark")
@Schema(description = "正文书签实际值")
public class WfOdocDocBookmark extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "模板ID")
    private Long mouldId;

    @Schema(description = "书签ID")
    private Long bookmarkId;

    @Schema(description = "实际值")
    private String bookmarkValue;

}
