package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文号使用记录（对应 E9 odoc_numberRequest）
 *
 * <p>每次取号落一条占用记录，唯一索引 (instance_id, number_id) 防同一实例重复取号；
 * serial 记录占用流水，回写/保留号依据此表。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_number_request")
@Schema(description = "文号使用记录")
public class WfOdocNumberRequest extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "文号规则ID")
    private Long numberId;

    @Schema(description = "生成的文号")
    private String numberName;

    @Schema(description = "年份(跨年重置判断)")
    private String years;

    @Schema(description = "占用流水")
    private Integer serial;

}
