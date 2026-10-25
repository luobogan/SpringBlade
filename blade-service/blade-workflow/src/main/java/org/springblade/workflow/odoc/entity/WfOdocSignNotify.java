package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 公文电子签章回调记录
 *
 * <p>e签宝回调的原始落库：唯一索引 {@code (esign_flow_id, action, ts)} 做幂等去重，
 * 保证同一事件重复投递只处理一次；{@code processed} 标记处理结果，
 * 0待处理 1已处理 2处理失败（失败可人工重放）。</p>
 *
 * <p>注意：父类 BaseEntity 的 {@code status} 是「1正常 0禁用」的公共列，
 * 本表的处理状态位叫 {@code processed}，两者不可混用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_sign_notify")
@Schema(description = "公文电子签章回调记录")
public class WfOdocSignNotify extends TenantEntity {

    /** 处理状态：待处理 */
    public static final int PROCESSED_PENDING = 0;
    /** 处理状态：已处理 */
    public static final int PROCESSED_DONE = 1;
    /** 处理状态：处理失败 */
    public static final int PROCESSED_FAILED = 2;

    private static final long serialVersionUID = 1L;

    @Schema(description = "本地主表ID（未匹配到时留空，待人工处理）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long flowId;

    @Schema(description = "e签宝签署流程ID")
    private String esignFlowId;

    @Schema(description = "回调事件，如 SIGN_FLOW_UPDATE")
    private String action;

    @Schema(description = "签署结果")
    private Integer signResult;

    @Schema(description = "原始报文")
    private String body;

    @Schema(description = "回调签名")
    private String sig;

    @Schema(description = "e签宝回调时间戳")
    private String ts;

    @Schema(description = "0待处理 1已处理 2处理失败")
    private Integer processed;

    @Schema(description = "处理说明/失败原因")
    private String processMsg;

    @Schema(description = "接收时间")
    private LocalDateTime receiveTime;

    @Schema(description = "处理时间")
    private LocalDateTime processTime;

    /**
     * 主键以字符串形式序列化，避免 19 位雪花 ID 在前端 JS 解析时丢失精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
