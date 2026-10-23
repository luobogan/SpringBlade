package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档回复（对应 E9 doc_reply）：支持子回复（parentId 指向父回复）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_reply")
@Schema(description = "文档回复")
public class DocReply extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "父回复ID(0=顶层)")
    private Long parentId;

    @Schema(description = "回复人")
    private Long userId;

    @Schema(description = "回复内容")
    private String content;
}
