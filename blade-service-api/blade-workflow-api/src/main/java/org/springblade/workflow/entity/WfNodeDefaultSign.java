package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 节点「按操作类型默认签字意见」（对齐泛微：不同操作自动带出默认意见）。
 *
 * <p>唯一键 (def_id, node_key, menu_type)；menu_type 取值如 submit/reject/forward/...
 * 或自定义按钮 key。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_default_sign")
@Schema(description = "按操作类型默认签字意见")
public class WfNodeDefaultSign extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义 ID")
    private Long defId;

    @Schema(description = "节点 Key")
    private String nodeKey;

    @Schema(description = "操作类型（submit/reject/forward/... 或自定义按钮 key）")
    private String menuType;

    @Schema(description = "该操作类型的默认签字意见")
    private String defaultOpinion;

}
