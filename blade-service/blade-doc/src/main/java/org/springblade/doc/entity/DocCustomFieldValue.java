package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 自定义字段值（对应 E9 cus_fielddata）：加密字段存密文（isEncrypted=1）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_custom_field_value")
@Schema(description = "自定义字段值")
public class DocCustomFieldValue extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "字段ID")
    private Long fieldId;

    @Schema(description = "字段值(加密字段为密文)")
    private String fieldValue;

    @Schema(description = "是否密文 0明文 1密文")
    private Integer isEncrypted;
}
