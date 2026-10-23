package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 目录文档属性配置（对应 E9 DocSecCategoryDocProperty）：属性可见性/必填/显示顺序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sec_category_doc_property")
@Schema(description = "目录文档属性配置")
public class DocSecCategoryDocProperty extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录ID")
    private Long categoryId;

    @Schema(description = "属性字段名")
    private String fieldName;

    @Schema(description = "是否可见 0否 1是")
    private Integer isView;

    @Schema(description = "是否必填 0否 1是")
    private Integer isRequired;

    @Schema(description = "显示顺序(列序)")
    private Integer viewIndex;
}
