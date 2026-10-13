package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 书签 → 表单字段映射（对应 E9 DocMouldBookMark）
 *
 * <p>套红时按 bookmarkId 找到书签，按 fieldKey 从表单数据取值填入；dateShowType 控制日期
 * 显示格式，selectValue 用于模板选择值（文号型为 -{numberId}-100）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_mould_field_map")
@Schema(description = "模板书签-表单字段映射")
public class WfOdocMouldFieldMap extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "模板ID")
    private Long mouldId;

    @Schema(description = "书签ID")
    private Long bookmarkId;

    @Schema(description = "表单字段名(fieldName)")
    private String fieldKey;

    @Schema(description = "作用域 main / dt{idx}")
    private String scope;

    @Schema(description = "日期显示类型 0原样 1中文日期")
    private Integer dateShowType;

    @Schema(description = "模板选择值(文号型为 -{numberId}-100)")
    private String selectValue;

}
