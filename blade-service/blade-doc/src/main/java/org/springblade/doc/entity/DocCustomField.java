package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 自定义字段定义（对应 E9 cus_formdict）。
 *
 * <p>fieldType：1单行文本(1text/2int/3float) 2多行文本 3浏览按钮 4复选框 5选择框。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_custom_field")
@Schema(description = "自定义字段定义")
public class DocCustomField extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段名(存储键)")
    private String fieldName;

    @Schema(description = "字段标签")
    private String fieldLabel;

    @Schema(description = "字段类型 1单行 2多行 3浏览按钮 4复选 5选择")
    private Integer fieldType;

    @Schema(description = "是否加密存储 0否 1是")
    private Integer isEncrypt;

    @Schema(description = "加密/权限作用域(读与写必须一致，防 E9 §18.4 静默失效)")
    private String scope;
}
