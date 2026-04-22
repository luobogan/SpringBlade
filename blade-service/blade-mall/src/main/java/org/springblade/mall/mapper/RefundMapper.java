package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.Refund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 退款记录Mapper
 */

public interface RefundMapper extends BaseMapper<Refund> {

    /**
     * 根据退款单号查询退款记录
     */
    @Select("SELECT * FROM mall_refund WHERE refund_no = #{refundNo}")
    Refund selectByRefundNo(@Param("refundNo") String refundNo);

    /**
     * 根据支付单号查询退款记录列表
     */
    @Select("SELECT * FROM mall_refund WHERE payment_no = #{paymentNo} ORDER BY create_time DESC")
    List<Refund> selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 根据订单号查询退款记录列表
     */
    @Select("SELECT * FROM mall_refund WHERE order_no = #{orderNo} ORDER BY create_time DESC")
    List<Refund> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户的退款记录列表
     */
    @Select("SELECT * FROM mall_refund WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Refund> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据状态查询退款记录
     */
    @Select("SELECT * FROM mall_refund WHERE status = #{status} ORDER BY create_time DESC")
    List<Refund> selectByStatus(@Param("status") String status);

    /**
     * 获取退款记录总数
     */
    @Select("SELECT COUNT(*) FROM mall_refund")
    Long countAllRefunds();

    /**
     * 获取今日退款总额
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM mall_refund WHERE status = 'SUCCESS' AND DATE(create_time) = CURDATE()")
    java.math.BigDecimal sumTodayRefundAmount();
}




