package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单Mapper
 */

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 自定义查询：获取订单总数
     * 使用 @Select 避免 MyBatis Plus 的 SQL 语法问题
     */
    @Select("SELECT COUNT(*) FROM `order`")
    Long countAllOrders();

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Select("SELECT * FROM `order` WHERE order_no = #{orderNo}")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 统计订单表中的唯一用户ID数量
     * @return 唯一用户数量
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM `order` WHERE user_id IS NOT NULL")
    Long countDistinctUsers();
}



