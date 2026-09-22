package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

/**
 * 流程定义灰度规则实体（表 {@code wf_definition_gray}，迁移 {@code V2026.09.21_017}）。
 *
 * <p>用途（方案 §4.3「灰度实现」）：新版本上线不搞一刀切全量 —— 先对白名单/小比例生效，
 * 观察无异常再逐步放大；出问题把 {@code status} 置 0 即可<b>秒级切回</b>旧版本
 * （在途实例不受影响：Flowable 实例原生绑定创建时的 {@code ACT_RE_PROCDEF.ID_}）。</p>
 *
 * <p>规则载体选择「落库而非 Nacos」：便于审计「谁在何时对哪个流程开了多少灰度」。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_definition_gray")
@Schema(description = "流程定义灰度规则")
public class WfDefinitionGray extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 策略：白名单 */
    public static final String STRATEGY_WHITELIST = "whitelist";
    /** 策略：比例 */
    public static final String STRATEGY_RATIO = "ratio";
    /** 策略：部门 */
    public static final String STRATEGY_DEPT = "dept";
    /** 策略：角色 */
    public static final String STRATEGY_ROLE = "role";

    /** 启用 */
    public static final int STATUS_ENABLED = 1;
    /** 停用（= 立即回退正式版本） */
    public static final int STATUS_DISABLED = 0;

    @Schema(description = "流程定义ID（版本组锚点）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "正式版本 processDefinitionId（灰度兜底）")
    private String baseProcDefId;

    @Schema(description = "灰度版本 processDefinitionId（命中时使用）")
    private String grayProcDefId;

    @Schema(description = "策略 whitelist=白名单 / ratio=比例 / dept=部门 / role=角色")
    private String strategy;

    @Schema(description = "ratio 策略下的百分比（1-100）")
    private Integer ratio;

    @Schema(description = "白名单用户（或部门/角色）ID，逗号分隔")
    private String scopeValue;

    @Schema(description = "1启用 0停用（置0=立即回退正式版本）")
    private Integer status;

    @Schema(description = "灰度开始时间")
    private Date startTime;
}
