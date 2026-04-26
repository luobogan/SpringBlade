package org.springblade.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.order.dto.OrderDTO;
import org.springblade.order.dto.OrderItemDTO;
import org.springblade.order.entity.Order;
import org.springblade.order.entity.OrderItem;
import org.springblade.order.mapper.OrderItemMapper;
import org.springblade.order.mapper.OrderMapper;
import org.springblade.order.service.IOrderService;
import org.springblade.order.vo.*;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements IOrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private IUserClient userClient;

    @Override
    @Transactional
    public OrderVO createOrder(OrderDTO orderDTO) {
        String tenantId = SecureUtil.getTenantId();
        log.info("创建订单，用户ID: {}, 租户ID: {}", orderDTO.getUserId(), tenantId);

        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        R<UserInfo> userResult = userClient.userInfo(orderDTO.getUserId());
        log.info("用户信息查询结果: {}", userResult);

        if (!userResult.isSuccess() || userResult.getData() == null) {
            throw new RuntimeException("用户不存在");
        }

        String orderNo = generateOrderNo();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            BigDecimal itemPrice = itemDTO.getPrice() != null ? itemDTO.getPrice() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        BigDecimal actualAmount = totalAmount;

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(orderDTO.getUserId());
        order.setShippingAddressId(orderDTO.getShippingAddressId());
        order.setCouponId(orderDTO.getCouponId());
        order.setTotalAmount(totalAmount);
        order.setActualAmount(actualAmount);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setOrderStatus("PENDING");
        order.setRemark(orderDTO.getRemark());
        order.setTenantId(tenantId);

        log.info("准备插入订单: {}", order);
        orderMapper.insert(order);
        log.info("订单插入成功，ID: {}", order.getId());

        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(itemDTO.getProductId());
            orderItem.setProductName(itemDTO.getProductName());
            orderItem.setProductImage(itemDTO.getProductImage());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(itemDTO.getPrice());
            orderItem.setTotalPrice(itemDTO.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            orderItem.setSkuAttributes(itemDTO.getSkuAttributes());
            orderItem.setSkuId(itemDTO.getSkuId());
            orderItem.setTenantId(order.getTenantId());

            log.info("准备插入订单项: {}", orderItem);
            orderItemMapper.insert(orderItem);
            log.info("订单项插入成功");
        }

        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO updateOrderStatus(Long id, String status) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        order.setOrderStatus(status);

        if ("PAID".equals(status)) {
            order.setPaymentTime(LocalDateTime.now());
        } else if ("SHIPPED".equals(status)) {
            order.setShippingTime(LocalDateTime.now());
        } else if ("COMPLETED".equals(status)) {
            order.setConfirmTime(LocalDateTime.now());
        } else if ("CANCELLED".equals(status)) {
            restoreStock(id);
        }

        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO payOrder(Long id, String paymentMethod, String paymentNo) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setOrderStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentNo(paymentNo);
        order.setPaymentTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO shipOrder(Long id, String shippingMethod, String trackingNo) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"PAID".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setOrderStatus("SHIPPED");
        order.setShippingMethod(shippingMethod);
        order.setTrackingNo(trackingNo);
        order.setShippingTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO completeOrder(Long id) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setOrderStatus("PENDING_REVIEW");
        order.setConfirmTime(LocalDateTime.now());

        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long id) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!"PENDING".equals(order.getOrderStatus()) && !"PAID".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setOrderStatus("CANCELLED");
        orderMapper.updateById(order);

        restoreStock(id);
        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderById(Long id, BladeUser user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        log.info("查询订单，订单ID: {}, 用户ID: {}, 租户ID: {}", id, user.getUserId(), user.getTenantId());

        Order order = orderMapper.selectById(id);
        log.info("查询结果: {}", order);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        String tenantId = user.getTenantId();
        if (StringUtil.isNotBlank(tenantId) && !tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权访问此订单");
        }

        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderByOrderNo(String orderNo) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        Order order = orderMapper.selectOne(queryWrapper);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        return convertToVO(order);
    }

    @Override
    public List<OrderVO> getOrdersByUserId(Long userId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getAllOrders() {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByStatus(String status) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", status);
        queryWrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByUserIdAndStatus(Long userId, String status) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("order_status", status);
        queryWrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByUserIdAndStatuses(Long userId, List<String> statuses) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.in("order_status", statuses);
        queryWrapper.orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        String tenantId = SecureUtil.getTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权删除此订单");
        }

        if (!"CANCELLED".equals(order.getOrderStatus()) && !"COMPLETED".equals(order.getOrderStatus())) {
            throw new RuntimeException("只能删除已取消或已完成的订单");
        }

        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", id);
        orderItemMapper.delete(itemQuery);

        orderMapper.deleteById(id);
    }

    @Override
    @Transactional
    public OrderVO reviewOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"PENDING_REVIEW".equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setOrderStatus("COMPLETED");
        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO applyReturn(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        order.setOrderStatus("RETURN_AFTER_SALES");
        orderMapper.updateById(order);
        return convertToVO(order);
    }

    @Override
    public OrderCountStats getOrderCountStats(Long userId) {
        OrderCountStats stats = new OrderCountStats();

        QueryWrapper<Order> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("user_id", userId);
        pendingQuery.eq("order_status", "PENDING");
        stats.setWaitPay(orderMapper.selectCount(pendingQuery).intValue());

        QueryWrapper<Order> receiveQuery = new QueryWrapper<>();
        receiveQuery.eq("user_id", userId);
        receiveQuery.in("order_status", List.of("PAID", "SHIPPED"));
        stats.setWaitReceive(orderMapper.selectCount(receiveQuery).intValue());

        QueryWrapper<Order> reviewQuery = new QueryWrapper<>();
        reviewQuery.eq("user_id", userId);
        reviewQuery.eq("order_status", "PENDING_REVIEW");
        stats.setWaitComment(orderMapper.selectCount(reviewQuery).intValue());

        QueryWrapper<Order> refundQuery = new QueryWrapper<>();
        refundQuery.eq("user_id", userId);
        refundQuery.eq("order_status", "RETURN_AFTER_SALES");
        stats.setRefund(orderMapper.selectCount(refundQuery).intValue());

        return stats;
    }

    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNum = random.nextInt(10000);
        return timestamp + String.format("%04d", randomNum);
    }

    private void restoreStock(Long orderId) {
        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(queryWrapper);

        for (OrderItem item : items) {
            log.info("恢复库存: productId={}, quantity={}", item.getProductId(), item.getQuantity());
        }
    }

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }

    private OrderVO convertToVO(Order order) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        if (order.getTotalAmount() != null) {
            orderVO.setTotalAmount(order.getTotalAmount().doubleValue());
        }
        if (order.getActualAmount() != null) {
            orderVO.setActualAmount(order.getActualAmount().doubleValue());
        }
        if (order.getCouponAmount() != null) {
            orderVO.setDiscountAmount(order.getCouponAmount().doubleValue());
        }

        orderVO.setCreatedAt(formatDate(order.getCreateTime()));
        orderVO.setUpdatedAt(formatDate(order.getUpdateTime()));
        orderVO.setPaidAt(formatDate(order.getPaymentTime() != null ? java.sql.Timestamp.valueOf(order.getPaymentTime()) : null));
        orderVO.setShippedAt(formatDate(order.getShippingTime() != null ? java.sql.Timestamp.valueOf(order.getShippingTime()) : null));
        orderVO.setCompletedAt(formatDate(order.getConfirmTime() != null ? java.sql.Timestamp.valueOf(order.getConfirmTime()) : null));

        orderVO.setStatus(getStatusValue(order.getOrderStatus()));
        orderVO.setStatusText(getStatusText(order.getOrderStatus()));

        if (order.getUserId() != null) {
            R<UserInfo> userResult = userClient.userInfo(order.getUserId());
            if (userResult.isSuccess() && userResult.getData() != null && userResult.getData().getUser() != null) {
                User user = userResult.getData().getUser();
                orderVO.setUserName(user.getName() != null ? user.getName() : user.getAccount());
            }
        }

        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemQuery);
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            if (item.getPrice() != null) {
                itemVO.setPrice(item.getPrice().doubleValue());
            }
            if (item.getTotalPrice() != null) {
                itemVO.setTotalPrice(item.getTotalPrice().doubleValue());
            }
            return itemVO;
        }).collect(Collectors.toList());
        orderVO.setItems(itemVOs);

        return orderVO;
    }

    private String getStatusText(String status) {
        return switch (status) {
            case "PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "SHIPPED" -> "已发货";
            case "PENDING_REVIEW" -> "待评价";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "RETURN_AFTER_SALES" -> "退换/售后";
            default -> "未知状态";
        };
    }

    private Integer getStatusValue(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "PAID" -> 10;
            case "SHIPPED" -> 30;
            case "PENDING_REVIEW" -> 40;
            case "COMPLETED" -> 50;
            case "CANCELLED" -> 60;
            case "RETURN_AFTER_SALES" -> 70;
            default -> -1;
        };
    }
}
