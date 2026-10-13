package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 文号保留号（对应 E9 odoc_numberReserved，可选后期）
 *
 * <p>支持人工保留/跳过特定流水；唯一索引 (number_id, serial) 防重复保留，
 * expireTime 过期自动释放。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_number_reserved")
@Schema(description = "文号保留号")
public class WfOdocNumberReserved extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文号规则ID")
    private Long numberId;

    @Schema(description = "保留流水")
    private Integer serial;

    @Schema(description = "保留人")
    private Long reservedBy;

    @Schema(description = "过期自动释放时间")
    private LocalDateTime expireTime;

}
