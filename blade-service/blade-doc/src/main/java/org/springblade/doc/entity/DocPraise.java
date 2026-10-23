package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档点赞（对应 E9 PRAISE_INFO）：一人一赞。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_praise")
@Schema(description = "文档点赞")
public class DocPraise extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "点赞人")
    private Long userId;
}
