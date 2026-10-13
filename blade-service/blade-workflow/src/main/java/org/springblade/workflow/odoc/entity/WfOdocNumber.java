package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文号规则配置（对应 E9 odoc_numberManage）
 *
 * <p>定义机关代字、年份格式、流水位数与补零规则，并绑定流程定义与套红模板；并发取号
 * 依赖本表的 serial 流水与唯一约束。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_number")
@Schema(description = "文号规则")
public class WfOdocNumber extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文号名称,如 普发文件")
    private String typeName;

    @Schema(description = "机关代字,如 X政发")
    private String prefix;

    @Schema(description = "年份前缀,如 [")
    private String yearPre;

    @Schema(description = "年份后缀,如 ]")
    private String yearEnd;

    @Schema(description = "是否显示年份")
    private Integer showYear;

    @Schema(description = "流水前缀,如 第")
    private String serialPre;

    @Schema(description = "当前流水号")
    private Integer serial;

    @Schema(description = "流水位数")
    private Integer digit;

    @Schema(description = "流水是否补零")
    private Integer fillZero;

    @Schema(description = "绑定流程定义,逗号分隔")
    private String defIds;

    @Schema(description = "绑定套红模板(文号型 select_value=-{id}-100)")
    private String mouldIds;

    @Schema(description = "是否启用")
    private Integer isOpen;

    @Schema(description = "是否已使用")
    private Integer isUsed;

}
