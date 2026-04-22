package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.dto.OrderDTO;
import org.springblade.mall.dto.OrderItemDTO;
import org.springblade.mall.entity.*;
import org.springblade.mall.mapper.*;
import org.springblade.mall.service.MemberService;
import org.springblade.mall.service.OrderService;
import org.springblade.mall.service.ProductService;
import org.springblade.mall.vo.AddressVO;
import org.springblade.mall.vo.OrderItemVO;
import org.springblade.mall.vo.OrderVO;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private IUserClient userClient;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ProductService productService;

    @Override
    @Transactional
    public OrderVO createOrder(OrderDTO orderDTO) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 检查用户是否存在
        org.springblade.core.tool.api.R<org.springblade.system.user.entity.UserInfo> userResult = userClient.userInfo(orderDTO.getUserId());
        if (!userResult.isSuccess() || userResult.getData() == null || userResult.getData().getUser() == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查地址是否存在
        Address address = addressMapper.selectById(orderDTO.getShippingAddressId());
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 计算订单金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 检查并扣减 SKU 库存
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在：" + itemDTO.getProductId());
            }

            // 查询 SKU 库存
            ProductSku productSku = null;

            // 优先使用 skuId（如果前端传递了）
            if (itemDTO.getSkuId() != null) {
                productSku = productSkuMapper.selectById(itemDTO.getSkuId());
            }

            // 如果没有 skuId 或 skuId 不存在，根据规格字段查询对应的 SKU
            if (productSku == null) {
                QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
                skuQuery.eq("product_id", itemDTO.getProductId());

                // 根据规格字段查询对应的 SKU
                // 前端传递的 selectedSpecs 格式：{"color":"黑色","size":"256GB"}
                // 或者直接使用 spec1, spec2 等字段
                boolean hasSpec = false;

                // 优先使用 spec1, spec2 字段（标准格式）
                if (itemDTO.getColor() != null && !itemDTO.getColor().isEmpty()) {
                    skuQuery.eq("spec1", itemDTO.getColor());
                    hasSpec = true;
                }
                if (itemDTO.getSize() != null && !itemDTO.getSize().isEmpty()) {
                    skuQuery.eq("spec2", itemDTO.getSize());
                    hasSpec = true;
                }

                // 如果没有规格参数，尝试从 price 反推 SKU（价格匹配）
                if (!hasSpec && itemDTO.getPrice() != null) {
                    // 使用价格作为辅助条件
                    skuQuery.eq("price", itemDTO.getPrice());
                    // 限制只返回一条记录
                    skuQuery.last("LIMIT 1");
                } else if (!hasSpec) {
                    // 既没有规格也没有价格，默认选择第一个可用的 SKU
                    skuQuery.last("LIMIT 1");
                }

                productSku = productSkuMapper.selectOne(skuQuery);
            }

            if (productSku == null) {
                throw new RuntimeException("SKU 不存在");
            }

            if (productSku.getStock() < itemDTO.getQuantity()) {
                throw new RuntimeException("商品库存不足：" + product.getName());
            }

            // 扣减 SKU 库存
            productSku.setStock(productSku.getStock() - itemDTO.getQuantity());
            productSkuMapper.updateById(productSku);

            // 更新商品总库存
            productService.updateProductStock(itemDTO.getProductId());

            // 计算金额
            System.out.println("=== 订单商品信息 ===");
            System.out.println("商品ID: " + itemDTO.getProductId());
            System.out.println("商品名称: " + product.getName());
            System.out.println("前端传递的价格: " + itemDTO.getPrice());
            System.out.println("数据库中的价格: " + product.getPrice());
            System.out.println("购买数量: " + itemDTO.getQuantity());

            BigDecimal itemPrice = itemDTO.getPrice() != null ? itemDTO.getPrice() : product.getPrice();
            System.out.println("使用的价格: " + itemPrice);
            System.out.println("商品小计: " + itemPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            System.out.println("当前累计总金额: " + totalAmount);
        }

        System.out.println("=== 订单金额计算完成 ===");
        System.out.println("订单总金额: " + totalAmount);

        // 处理优惠券
        if (orderDTO.getCouponId() != null) {
            log.info("处理优惠券，couponId: {}", orderDTO.getCouponId());

            // 先尝试根据优惠券ID和用户ID查询用户优惠券记录
            QueryWrapper<UserCoupon> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("coupon_id", orderDTO.getCouponId());
            queryWrapper.eq("user_id", orderDTO.getUserId());
            queryWrapper.eq("status", 1); // 只查询未使用的优惠券
            UserCoupon userCoupon = userCouponMapper.selectOne(queryWrapper);

            // 如果直接查询失败，尝试直接查询用户优惠券记录
            if (userCoupon == null) {
                log.info("根据优惠券ID和用户ID查询失败，尝试直接查询用户优惠券记录");
                userCoupon = userCouponMapper.selectById(orderDTO.getCouponId());
            }

            if (userCoupon != null && userCoupon.getUserId().equals(orderDTO.getUserId()) && userCoupon.getStatus() == 1) {
                log.info("用户优惠券有效，userId: {}, status: {}", userCoupon.getUserId(), userCoupon.getStatus());
                Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
                if (coupon != null && coupon.getStatus() == 1) {
                    log.info("优惠券有效，couponId: {}, status: {}", coupon.getId(), coupon.getStatus());

                    // 验证优惠券是否在有效期内
                    LocalDateTime now = LocalDateTime.now();
                    if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
                        log.warn("优惠券未开始使用，startTime: {}", coupon.getStartTime());
                    } else if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
                        log.warn("优惠券已过期，endTime: {}", coupon.getEndTime());
                    } else if (coupon.getMinSpend() != null && totalAmount.compareTo(coupon.getMinSpend()) < 0) {
                        log.warn("订单金额未达到最低消费要求，totalAmount: {}, minSpend: {}", totalAmount, coupon.getMinSpend());
                    } else {
                        log.info("优惠券验证通过，开始计算优惠金额");
                        // 计算优惠金额
                        if (coupon.getType() == 1) {
                            // 固定金额
                            discountAmount = coupon.getValue().min(totalAmount);
                            log.info("固定金额优惠券，value: {}, discountAmount: {}", coupon.getValue(), discountAmount);
                        } else if (coupon.getType() == 2) {
                            // 百分比
                            discountAmount = totalAmount.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            if (coupon.getMaxDiscount() != null) {
                                discountAmount = discountAmount.min(coupon.getMaxDiscount());
                            }
                            log.info("百分比优惠券，value: {}, maxDiscount: {}, discountAmount: {}", coupon.getValue(), coupon.getMaxDiscount(), discountAmount);
                        }

                        // 标记优惠券已使用
                        userCoupon.setStatus(2);
                        userCoupon.setUsedAt(LocalDateTime.now());
                        userCouponMapper.updateById(userCoupon);
                        log.info("优惠券已标记为已使用，userCouponId: {}", userCoupon.getId());

                        // 更新优惠券使用数量
                        coupon.setUsedQuantity(coupon.getUsedQuantity() + 1);
                        couponMapper.updateById(coupon);
                        log.info("优惠券使用数量已更新，couponId: {}, usedQuantity: {}", coupon.getId(), coupon.getUsedQuantity());
                    }
                } else {
                    log.warn("优惠券不存在或已禁用，couponId: {}", userCoupon.getCouponId());
                }
            } else {
                log.warn("用户优惠券不存在、不属于当前用户或已使用，couponId: {}", orderDTO.getCouponId());
            }
        } else {
            log.info("未使用优惠券");
        }

        // 计算实际支付金额
        BigDecimal actualAmount = totalAmount.subtract(discountAmount);
        log.info("计算实付金额，totalAmount: {}, discountAmount: {}, actualAmount: {}", totalAmount, discountAmount, actualAmount);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(orderDTO.getUserId());
        order.setShippingAddressId(orderDTO.getShippingAddressId());
        order.setCouponId(orderDTO.getCouponId());
        order.setTotalAmount(totalAmount);
        order.setActualAmount(actualAmount);
        order.setCouponAmount(discountAmount);
        order.setOrderStatus("PENDING");
        order.setRemark(orderDTO.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTenantId(tenantId);

        // 保存订单
        orderMapper.insert(order);

        // 创建订单项
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productMapper.selectById(itemDTO.getProductId());

            // 获取商品主图
            QueryWrapper<ProductImage> imageQuery = new QueryWrapper<>();
            imageQuery.eq("product_id", product.getId());
            imageQuery.orderByAsc("sort");
            List<ProductImage> images = productImageMapper.selectList(imageQuery);
            String productImage = images.isEmpty() ? "" : images.get(0).getImageUrl();

            // 构建SKU属性
            StringBuilder skuAttributes = new StringBuilder();

            // 优先使用SKU ID查询完整的SKU信息
            if (itemDTO.getSkuId() != null) {
                log.info("查询SKU信息，SKU ID: {}", itemDTO.getSkuId());
                ProductSku productSku = productSkuMapper.selectById(itemDTO.getSkuId());
                if (productSku != null) {
                    log.info("查询到SKU信息: {}", productSku);

                    // 优先使用sku_name字段（如果存在且不为空）
                    if (productSku.getSkuName() != null && !productSku.getSkuName().isEmpty()) {
                        skuAttributes.append(productSku.getSkuName());
                    } else {
                        // 如果sku_name不存在或为空，用spec1、spec2等字段构建
                        if (productSku.getSpec1() != null && !productSku.getSpec1().isEmpty()) {
                            skuAttributes.append("颜色:").append(productSku.getSpec1());
                        }
                        if (productSku.getSpec2() != null && !productSku.getSpec2().isEmpty()) {
                            if (skuAttributes.length() > 0) {
                                skuAttributes.append(",");
                            }
                            skuAttributes.append("内存:").append(productSku.getSpec2());
                        }
                        if (productSku.getSpec3() != null && !productSku.getSpec3().isEmpty()) {
                            if (skuAttributes.length() > 0) {
                                skuAttributes.append(",");
                            }
                            skuAttributes.append("容量:").append(productSku.getSpec3());
                        }
                        if (productSku.getSpec4() != null && !productSku.getSpec4().isEmpty()) {
                            if (skuAttributes.length() > 0) {
                                skuAttributes.append(",");
                            }
                            skuAttributes.append("网络:").append(productSku.getSpec4());
                        }
                    }
                    log.info("构建的SKU属性: {}", skuAttributes.toString());
                } else {
                    log.warn("未查询到SKU信息，SKU ID: {}", itemDTO.getSkuId());
                }
            } else {
                log.info("没有SKU ID，使用前端传递的颜色和尺寸");
            }

            // 如果没有SKU ID或查询失败，使用前端传递的颜色和尺寸
            if (skuAttributes.length() == 0) {
                if (itemDTO.getColor() != null && !itemDTO.getColor().isEmpty()) {
                    skuAttributes.append("颜色:").append(itemDTO.getColor());
                }
                if (itemDTO.getSize() != null && !itemDTO.getSize().isEmpty()) {
                    if (skuAttributes.length() > 0) {
                        skuAttributes.append(",");
                    }
                    skuAttributes.append("尺寸:").append(itemDTO.getSize());
                }
            }

            // 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(productImage);
            orderItem.setQuantity(itemDTO.getQuantity());
            BigDecimal itemPrice = itemDTO.getPrice() != null ? itemDTO.getPrice() : product.getPrice();
            orderItem.setPrice(itemPrice);
            orderItem.setTotalPrice(itemPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            orderItem.setSkuAttributes(skuAttributes.length() > 0 ? skuAttributes.toString() : null);
            // 保存SKU ID
            orderItem.setSkuId(itemDTO.getSkuId());
            orderItem.setTenantId(order.getTenantId());

            // 保存订单项
            orderItemMapper.insert(orderItem);
        }

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO updateOrderStatus(Long id, String status) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        // 更新状态
        order.setOrderStatus(status);
        order.setUpdatedAt(LocalDateTime.now());

        // 处理状态相关时间
        if (status.equals("PAID")) {
            order.setPaymentTime(LocalDateTime.now());
        } else if (status.equals("SHIPPED")) {
            order.setShippingTime(LocalDateTime.now());
        } else if (status.equals("COMPLETED")) {
            order.setConfirmTime(LocalDateTime.now());
        } else if (status.equals("CANCELLED")) {
            // 取消订单时恢复库存
            restoreStock(id);
        }

        // 保存更新
        orderMapper.updateById(order);

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO payOrder(Long id, String paymentMethod, String paymentNo) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("PENDING")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新支付信息
        order.setOrderStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentNo(paymentNo);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO shipOrder(Long id, String shippingMethod, String trackingNo) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("PAID")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新发货信息
        order.setOrderStatus("SHIPPED");
        order.setShippingMethod(shippingMethod);
        order.setTrackingNo(trackingNo);
        order.setShippingTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO completeOrder(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("SHIPPED")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为待评价
        order.setOrderStatus("PENDING_REVIEW");
        order.setConfirmTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 赠送积分和成长值
        try {
            Long userId = order.getUserId();
            BigDecimal totalAmount = order.getTotalAmount();

            // 更新会员消费金额
            memberService.updateConsumption(userId, totalAmount);

            // 根据订单金额赠送积分和成长值（1 元=1 积分=1 成长值）
            int pointsAndGrowth = totalAmount.intValue();
            if (pointsAndGrowth > 0) {
                memberService.addPoints(userId, pointsAndGrowth, "ORDER", order.getId(),
                    "订单完成奖励，订单号：" + order.getOrderNo());
                memberService.addGrowth(userId, pointsAndGrowth, "ORDER", order.getId(),
                    "订单完成奖励，订单号：" + order.getOrderNo());
            }
        } catch (Exception e) {
            // 记录错误日志，但不影响订单完成流程
            System.err.println("赠送积分和成长值失败：" + e.getMessage());
        }

        // 转换为 VO 返回
        return convertToVO(order);
    }

    /**
     * 评价订单
     * @param id 订单ID
     * @return 评价结果
     */
    @Transactional
    public OrderVO reviewOrder(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("PENDING_REVIEW")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为已完成
        order.setOrderStatus("COMPLETED");
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 转换为VO返回
        return convertToVO(order);
    }

    /**
     * 申请退换/售后
     * @param id 订单ID
     * @return 申请结果
     */
    @Transactional
    public OrderVO applyReturn(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("PAID") && !order.getOrderStatus().equals("SHIPPED") && !order.getOrderStatus().equals("PENDING_REVIEW") && !order.getOrderStatus().equals("COMPLETED")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为退换/售后
        order.setOrderStatus("RETURN_AFTER_SALES");
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权操作此订单");
        }

        if (!order.getOrderStatus().equals("PENDING") && !order.getOrderStatus().equals("PAID")) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态
        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        orderMapper.updateById(order);

        // 恢复库存
        restoreStock(id);

        // 转换为VO返回
        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderById(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权访问此订单");
        }

        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderByOrderNo(String orderNo) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
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
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("created_at");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getAllOrders() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByStatus(String status) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("created_at");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByUserIdAndStatus(Long userId, String status) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("created_at");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getOrdersByUserIdAndStatuses(Long userId, List<String> statuses) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.in("status", statuses);
        queryWrapper.orderByDesc("created_at");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查租户ID是否匹配
        if (!tenantId.equals(order.getTenantId())) {
            throw new RuntimeException("无权删除此订单");
        }

        // 只能删除已取消或已完成的订单
        if (!order.getStatus().equals("CANCELLED") && !order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("只能删除已取消或已完成的订单");
        }

        // 删除订单项
        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", id);
        orderItemMapper.delete(itemQuery);

        // 删除订单
        orderMapper.deleteById(id);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNum = random.nextInt(10000);
        return timestamp + String.format("%04d", randomNum);
    }

    /**
     * 恢复库存
     */
    private void restoreStock(Long orderId) {
        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderItem> items = orderItemMapper.selectList(queryWrapper);

        for (OrderItem item : items) {
            // 恢复SKU库存
            if (item.getSkuId() != null) {
                ProductSku productSku = productSkuMapper.selectById(item.getSkuId());
                if (productSku != null) {
                    productSku.setStock(productSku.getStock() + item.getQuantity());
                    productSkuMapper.updateById(productSku);
                }
            }

            // 更新商品总库存
            productService.updateProductStock(item.getProductId());
        }
    }

    /**
     * 将订单实体转换为VO
     */
    private OrderVO convertToVO(Order order) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        // 手动转换金额字段，确保 BigDecimal 到 Double 的正确转换
        if (order.getTotalAmount() != null) {
            orderVO.setTotalAmount(order.getTotalAmount().doubleValue());
        }
        if (order.getActualAmount() != null) {
            orderVO.setActualAmount(order.getActualAmount().doubleValue());
        }
        if (order.getCouponAmount() != null) {
            orderVO.setDiscountAmount(order.getCouponAmount().doubleValue());
        }

        // 手动格式化日期字段
        if (order.getCreatedAt() != null) {
            orderVO.setCreatedAt(order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getUpdatedAt() != null) {
            orderVO.setUpdatedAt(order.getUpdatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getPaymentTime() != null) {
            orderVO.setPaidAt(order.getPaymentTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getShippingTime() != null) {
            orderVO.setShippedAt(order.getShippingTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getConfirmTime() != null) {
            orderVO.setCompletedAt(order.getConfirmTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // 转换状态为数字
        orderVO.setStatus(getStatusValue(order.getOrderStatus()));

        // 设置订单状态文本
        orderVO.setStatusText(getStatusText(order.getOrderStatus()));

        // 获取用户信息
        if (order.getUserId() != null) {
            org.springblade.core.tool.api.R<org.springblade.system.user.entity.UserInfo> userResult = userClient.userInfo(order.getUserId());
            if (userResult.isSuccess() && userResult.getData() != null && userResult.getData().getUser() != null) {
                org.springblade.system.user.entity.User user = userResult.getData().getUser();
                orderVO.setUserName(user.getName() != null ? user.getName() : user.getAccount());
            }
        }

        // 获取地址信息
        if (order.getShippingAddressId() != null) {
            Address address = addressMapper.selectById(order.getShippingAddressId());
            if (address != null) {
                AddressVO addressVO = new AddressVO();
                BeanUtils.copyProperties(address, addressVO);
                orderVO.setAddress(addressVO);
            }
        }

        // 获取优惠券信息
        if (order.getCouponId() != null) {
            // 先尝试直接查询用户优惠券记录
            UserCoupon userCoupon = userCouponMapper.selectById(order.getCouponId());
            Coupon coupon = null;

            // 如果直接查询失败，尝试根据优惠券ID和用户ID查询用户优惠券记录
            if (userCoupon == null) {
                log.info("直接查询用户优惠券失败，尝试根据优惠券ID和用户ID查询");
                QueryWrapper<UserCoupon> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("coupon_id", order.getCouponId());
                queryWrapper.eq("user_id", order.getUserId());
                userCoupon = userCouponMapper.selectOne(queryWrapper);
            }

            if (userCoupon != null) {
                // 再通过用户优惠券记录查询优惠券信息
                coupon = couponMapper.selectById(userCoupon.getCouponId());
            } else {
                // 如果还是找不到用户优惠券记录，直接查询优惠券信息
                log.info("找不到用户优惠券记录，直接查询优惠券信息");
                coupon = couponMapper.selectById(order.getCouponId());
            }

            if (coupon != null) {
                orderVO.setCouponCode(coupon.getCode());
                orderVO.setCouponName(coupon.getName());
                orderVO.setCouponType(coupon.getType());
                orderVO.setCouponValue(coupon.getValue().doubleValue());
                if (coupon.getMinSpend() != null) {
                    orderVO.setCouponMinSpend(coupon.getMinSpend().doubleValue());
                }
            }
        }

        // 获取订单项
        QueryWrapper<OrderItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("order_id", order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemQuery);
        List<OrderItemVO> itemVOs = items.stream()
                .map(item -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    // 手动转换订单项的金额字段，确保 BigDecimal 到 Double 的正确转换
                    if (item.getPrice() != null) {
                        itemVO.setPrice(item.getPrice().doubleValue());
                    }
                    if (item.getTotalPrice() != null) {
                        itemVO.setTotalPrice(item.getTotalPrice().doubleValue());
                    }
                    return itemVO;
                })
                .collect(Collectors.toList());
        orderVO.setItems(itemVOs);

        return orderVO;
    }

    /**
     * 获取订单状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待支付";
            case "PAID":
                return "已支付";
            case "SHIPPED":
                return "已发货";
            case "PENDING_REVIEW":
                return "待评价";
            case "COMPLETED":
                return "已完成";
            case "CANCELLED":
                return "已取消";
            case "RETURN_AFTER_SALES":
                return "退换/售后";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取订单状态数值
     */
    private Integer getStatusValue(String status) {
        switch (status) {
            case "PENDING":
                return 0;
            case "PAID":
                return 10;
            case "SHIPPED":
                return 30;
            case "PENDING_REVIEW":
                return 40;
            case "COMPLETED":
                return 50;
            case "CANCELLED":
                return 60;
            case "RETURN_AFTER_SALES":
                return 70;
            default:
                return -1;
        }
    }

    @Override
    public OrderCountStats getOrderCountStats(Long userId) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        OrderCountStats stats = new OrderCountStats();

        // 统计待付款订单数量
        QueryWrapper<Order> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("user_id", userId);
        pendingQuery.eq("status", "PENDING");
        stats.setWaitPay(orderMapper.selectCount(pendingQuery).intValue());

        // 统计待收货订单数量（已支付和已发货）
        QueryWrapper<Order> receiveQuery = new QueryWrapper<>();
        receiveQuery.eq("user_id", userId);
        receiveQuery.in("status", List.of("PAID", "SHIPPED"));
        stats.setWaitReceive(orderMapper.selectCount(receiveQuery).intValue());

        // 统计待评价订单数量
        QueryWrapper<Order> reviewQuery = new QueryWrapper<>();
        reviewQuery.eq("user_id", userId);
        reviewQuery.eq("status", "PENDING_REVIEW");
        stats.setWaitComment(orderMapper.selectCount(reviewQuery).intValue());

        // 统计退换/售后订单数量
        QueryWrapper<Order> refundQuery = new QueryWrapper<>();
        refundQuery.eq("user_id", userId);
        refundQuery.eq("status", "RETURN_AFTER_SALES");
        stats.setRefund(orderMapper.selectCount(refundQuery).intValue());

        return stats;
    }
}



