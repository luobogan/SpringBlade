package org.springblade.order.service;

import org.springblade.core.secure.BladeUser;
import org.springblade.order.dto.OrderDTO;
import org.springblade.order.vo.OrderCountStats;
import org.springblade.order.vo.OrderVO;

import java.util.List;

public interface IOrderService {

    OrderVO createOrder(OrderDTO orderDTO);

    OrderVO updateOrderStatus(Long id, String status);

    OrderVO payOrder(Long id, String paymentMethod, String paymentNo);

    OrderVO shipOrder(Long id, String shippingMethod, String trackingNo);

    OrderVO completeOrder(Long id);

    OrderVO cancelOrder(Long id);

    OrderVO getOrderById(Long id, BladeUser user);

    OrderVO getOrderByOrderNo(String orderNo);

    List<OrderVO> getOrdersByUserId(Long userId);

    List<OrderVO> getAllOrders();

    List<OrderVO> getOrdersByStatus(String status);

    List<OrderVO> getOrdersByUserIdAndStatus(Long userId, String status);

    List<OrderVO> getOrdersByUserIdAndStatuses(Long userId, List<String> statuses);

    void deleteOrder(Long id);

    OrderVO reviewOrder(Long id);

    OrderVO applyReturn(Long id);

    OrderCountStats getOrderCountStats(Long userId);
}
