package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档评分（对应 E9 DocScore）：一人一评分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_score")
@Schema(description = "文档评分")
public class DocScore extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "评分人")
    private Long userId;

    @Schema(description = "分值(1-5)")
    private Integer score;
}
