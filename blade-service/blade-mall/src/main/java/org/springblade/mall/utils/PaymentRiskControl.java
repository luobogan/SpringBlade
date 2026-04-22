package org.springblade.mall.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.entity.Payment;
import org.springblade.mall.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import java.time.LocalDateTime;

/**
 * 支付风控工具
 * 用于检测和防止异常支付行为
 */
@Component
@RequiredArgsConstructor
public class PaymentRiskControl {

    private static final Logger log = LoggerFactory.getLogger(PaymentRiskControl.class);

    private final PaymentMapper paymentMapper;

    @Value("${payment.risk-control.enabled:true}")
    private boolean enabled;

    @Value("${payment.risk-control.daily-limit:100000}")
    private BigDecimal dailyLimit;

    @Value("${payment.risk-control.daily-count-limit:20}")
    private int dailyCountLimit;

    @Value("${payment.risk-control.single-transaction-limit:50000}")
    private BigDecimal singleTransactionLimit;

    /**
     * 检查支付风险
     * @param userId 用户ID
     * @param amount 支付金额
     * @return 风险检查结果
     */
    public RiskCheckResult checkRisk(Long userId, BigDecimal amount) {
        if (!enabled) {
            return RiskCheckResult.success();
        }

        log.info("进行支付风控检查: userId={}, amount={}", userId, amount);

        // 1. 检查单笔交易金额
        if (amount.compareTo(singleTransactionLimit) > 0) {
            log.warn("单笔交易金额超限: userId={}, amount={}, limit={}", userId, amount, singleTransactionLimit);
            return RiskCheckResult.fail("单笔交易金额超过限制");
        }

        // 2. 检查今日累计金额
        BigDecimal todayTotal = getTodayTotalPayment(userId);
        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            log.warn("今日累计支付金额超限: userId={}, today={}, amount={}, limit={}", userId, todayTotal, amount, dailyLimit);
            return RiskCheckResult.fail("今日累计支付金额超过限制");
        }

        // 3. 检查今日支付次数
        int todayCount = getTodayPaymentCount(userId);
        if (todayCount >= dailyCountLimit) {
            log.warn("今日支付次数超限: userId={}, count={}, limit={}", userId, todayCount, dailyCountLimit);
            return RiskCheckResult.fail("今日支付次数超过限制");
        }

        log.info("支付风控检查通过: userId={}", userId);
        return RiskCheckResult.success();
    }

    /**
     * 获取今日累计支付金额
     */
    private BigDecimal getTodayTotalPayment(Long userId) {
        QueryWrapper<Payment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.in("status", "PAID", "REFUNDED");
        wrapper.ge("create_time", LocalDateTime.now().toLocalDate().atStartOfDay());
        wrapper.le("create_time", LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay());

        return paymentMapper.selectList(wrapper).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取今日支付次数
     */
    private int getTodayPaymentCount(Long userId) {
        QueryWrapper<Payment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.ge("create_time", LocalDateTime.now().toLocalDate().atStartOfDay());
        wrapper.le("create_time", LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay());

        return Math.toIntExact(paymentMapper.selectCount(wrapper));
    }

    /**
     * 风险检查结果
     */
    public static class RiskCheckResult {
        private final boolean passed;
        private final String message;

        private RiskCheckResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }

        public static RiskCheckResult success() {
            return new RiskCheckResult(true, null);
        }

        public static RiskCheckResult fail(String message) {
            return new RiskCheckResult(false, message);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }
    }
}
