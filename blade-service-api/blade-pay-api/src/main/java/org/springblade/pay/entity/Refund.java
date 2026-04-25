package org.springblade.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_refund")
public class Refund extends TenantEntity {

    private String refundNo;

    private String paymentNo;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    @TableField("refund_status")
    private String refundStatus;

    private LocalDateTime refundedAt;

    private String refundTransactionId;

    private String callbackData;

    private String failReason;

    private Long auditorId;

    private LocalDateTime auditedAt;

    private String auditRemark;
}
