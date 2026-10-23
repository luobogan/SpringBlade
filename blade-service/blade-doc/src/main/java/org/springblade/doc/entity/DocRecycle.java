package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 回收站（对应 E9 recycle_docdetail）：逻辑删除的文档，可恢复或彻底删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_recycle")
@Schema(description = "文档回收站")
public class DocRecycle extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "删除人")
    private Long deleteUser;

    @Schema(description = "删除时间")
    private java.time.LocalDateTime deleteTime;
}
