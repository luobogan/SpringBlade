package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档正文内容（对应 E9 DocDetailContent）：docId 一对一，content 为大字段 HTML。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_detail_content")
@Schema(description = "文档正文内容")
public class DocDetailContent extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "正文HTML内容")
    private String content;
}
