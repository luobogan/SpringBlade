package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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

    /**
     * 文档标题（非表字段，回收站列表回填）。
     *
     * <p>回收站里的文档已被逻辑删除，检索接口查不到，只能由回收站列表自行关联回填，
     * 否则列表只剩一个 docId，管理员无从判断删的是哪篇文档。</p>
     */
    @TableField(exist = false)
    @Schema(description = "文档标题（列表回填，非表字段）")
    private String docSubject;
}
