package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 支付记录Mapper
 */

public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 根据支付单号查询支付记录
     */
    @Select("SELECT * FROM payment WHERE payment_no = #{paymentNo}")
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 根据订单号查询支付记录列表
     */
    @Select("SELECT * FROM payment WHERE order_no = #{orderNo} ORDER BY create_time DESC")
    List<Payment> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户的支付记录列表
     */
    @Select("SELECT * FROM payment WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Payment> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据状态查询支付记录
     */
    @Select("SELECT * FROM payment WHERE status = #{status} ORDER BY create_time DESC")
    List<Payment> selectByStatus(@Param("status") String status);

    /**
     * 获取支付记录总数
     */
    @Select("SELECT COUNT(*) FROM payment")
    Long countAllPayments();

    /**
     * 获取今日支付总额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM payment WHERE status = 'SUCCESS' AND DATE(create_time) = CURDATE()")
    java.math.BigDecimal sumTodayPaymentAmount();
}




