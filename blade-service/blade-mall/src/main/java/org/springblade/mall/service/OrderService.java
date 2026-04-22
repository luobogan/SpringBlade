package org.springblade.mall.service;

import org.springblade.mall.dto.OrderDTO;

import org.springblade.mall.vo.OrderVO;
import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param orderDTO 订单信息
     * @return 创建的订单
     */
    OrderVO createOrder(OrderDTO orderDTO);
    
    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态
     * @return 更新后的订单
     */
    OrderVO updateOrderStatus(Long id, String status);
    
    /**
     * 支付订单
     * @param id 订单ID
     * @param paymentMethod 支付方式
     * @param paymentNo 支付单号
     * @return 支付结果
     */
    OrderVO payOrder(Long id, String paymentMethod, String paymentNo);
    
    /**
     * 发货
     * @param id 订单ID
     * @param shippingMethod 配送方式
     * @param trackingNo 物流单号
     * @return 发货结果
     */
    OrderVO shipOrder(Long id, String shippingMethod, String trackingNo);
    
    /**
     * 完成订单
     * @param id 订单ID
     * @return 完成结果
     */
    OrderVO completeOrder(Long id);
    
    /**
     * 取消订单
     * @param id 订单ID
     * @return 取消结果
     */
    OrderVO cancelOrder(Long id);
    
    /**
     * 获取订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    OrderVO getOrderById(Long id);
    
    /**
     * 根据订单号获取订单
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getOrderByOrderNo(String orderNo);
    
    /**
     * 获取用户订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    List<OrderVO> getOrdersByUserId(Long userId);
    
    /**
     * 获取所有订单
     * @return 订单列表
     */
    List<OrderVO> getAllOrders();
    
    /**
     * 根据状态获取订单
     * @param status 订单状态
     * @return 订单列表
     */
    List<OrderVO> getOrdersByStatus(String status);
    
    /**
     * 根据用户ID和状态获取订单
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    List<OrderVO> getOrdersByUserIdAndStatus(Long userId, String status);
    
    /**
     * 根据用户ID和状态列表获取订单
     * @param userId 用户ID
     * @param statuses 状态列表
     * @return 订单列表
     */
    List<OrderVO> getOrdersByUserIdAndStatuses(Long userId, List<String> statuses);
    
    /**
     * 删除订单
     * @param id 订单ID
     */
    void deleteOrder(Long id);
    
    /**
     * 评价订单
     * @param id 订单ID
     * @return 评价结果
     */
    OrderVO reviewOrder(Long id);
    
    /**
     * 申请退换/售后
     * @param id 订单ID
     * @return 申请结果
     */
    OrderVO applyReturn(Long id);
    
    /**
     * 获取用户订单数量统计
     * @param userId 用户ID
     * @return 订单数量统计
     */
    OrderCountStats getOrderCountStats(Long userId);
    
    /**
     * 订单数量统计类
     */
    class OrderCountStats {
        private int waitPay;
        private int waitReceive;
        private int waitComment;
        private int refund;
        
        // Getters and Setters
        public int getWaitPay() {
            return waitPay;
        }
        public void setWaitPay(int waitPay) {
            this.waitPay = waitPay;
        }
        public int getWaitReceive() {
            return waitReceive;
        }
        public void setWaitReceive(int waitReceive) {
            this.waitReceive = waitReceive;
        }
        public int getWaitComment() {
            return waitComment;
        }
        public void setWaitComment(int waitComment) {
            this.waitComment = waitComment;
        }
        public int getRefund() {
            return refund;
        }
        public void setRefund(int refund) {
            this.refund = refund;
        }
    }
}


