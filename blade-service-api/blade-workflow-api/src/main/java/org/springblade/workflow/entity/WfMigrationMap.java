package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * ecology 存量迁移映射实体
 *
 * <p>迁移与回滚的核心凭据：承载新旧 ID 映射，支持在途接管后的反查与双轨对账差异定位。</p>
 * <p>status：0待迁移 1成功 2失败。</p>
 *
 * <p><b>【T-14 退役候选 · 仅标记 @Deprecated，暂不 DROP】</b></p>
 * <p>经引用面盘点（2026-10-01）：本实体在代码中<b>无任何运行时读写</b>——仅 blade-formmode 的
 * {@code ApprovalTriggerServiceImpl} 第 84 行以<b>注释</b>形式提及，实际 {@code triggerSet.getWorkflowid()}
 * 直接落地，迁移桥接从未接线；P4 存量迁移（ecology）处于「❌ 未启动」状态。故本表为死表。</p>
 * <p>处置：保留表与实体不动，仅标记废弃；待 ecology 存量迁移正式验收（或确认永久不启动）后再行 DROP
 * 并删除本实体/Mapper。在途实例接管凭据不应随其他 wf_* 表一起提前删除。</p>
 *
 * @deprecated 死表（零引用）。保留至迁移验收后清理，详见 T-14 分批退役计划（Phase A）。
 */
@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_migration_map")
@Schema(description = "ecology 存量迁移映射（@Deprecated：死表，待迁移验收后清理）")
public class WfMigrationMap extends TenantEntity {

    /** 待迁移 */
    public static final int STATUS_PENDING = 0;
    /** 成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 失败 */
    public static final int STATUS_FAILED = 2;

    private static final long serialVersionUID = 1L;

    @Schema(description = "ecology workflow_base.id")
    private Long ecWfId;

    @Schema(description = "ecology workflow_requestbase.requestid（在途实例）")
    private Long ecRequestId;

    @Schema(description = "ecology 节点ID")
    private Long ecNodeId;

    @Schema(description = "新流程定义ID")
    private Long newDefId;

    @Schema(description = "新流程实例ID")
    private Long newInstId;

    @Schema(description = "新节点Key")
    private String newNodeKey;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "迁移时间")
    private Date migrateTime;

    @Schema(description = "错误信息")
    private String errMsg;

}
