package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 节点级明细表字段筛选
 *
 * <p>对齐 ecology「明细表数据根据操作者筛选显示」：节点可配置按明细字段值过滤明细行，
 * 仅当某行满足全部筛选规则时才对该操作者「显示 / 打印」出来。支持两套口径
 * （显示 / 打印，{@code modeType}）与四种比较方式（等于 / 不等于 / 包含 / 不包含）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_detail_filter")
@Schema(description = "节点级明细表字段筛选")
public class WfNodeDetailFilter extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "明细表序号")
    private Integer dtIndex;

    @Schema(description = "过滤口径 1=显示时过滤 2=打印时过滤")
    private Integer modeType;

    @Schema(description = "比较字段（明细列的 fieldName）")
    private String fieldName;

    @Schema(description = "比较方式 1等于 2不等于 3包含 4不包含")
    private Integer compareType;

    @Schema(description = "比较值（多值用逗号分隔）")
    private String compareValue;

    @Schema(description = "过滤后要求至少一条 1=是 0=否")
    private Integer isRequired;

}
