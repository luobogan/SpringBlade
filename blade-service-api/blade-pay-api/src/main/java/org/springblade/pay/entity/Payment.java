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
@TableName("blade_payment")
public class Payment extends TenantEntity {

    private String paymentNo;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String paymentMethod;

    @TableField("status")
    private String paymentStatus;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    private String remark;

    @TableField("create_time")
    private LocalDateTime paymentCreateTime;

    @TableField("update_time")
    private LocalDateTime paymentUpdateTime;
}
