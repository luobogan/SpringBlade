package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档 ↔ 虚拟目录挂载（对应 E9 DocDummyDetail）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_dummy_detail")
@Schema(description = "文档虚拟目录挂载")
public class DocDummyDetail extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "虚拟目录ID(DocTreeDocField)")
    private Long catalogId;

    @Schema(description = "文档ID")
    private Long docId;
}
