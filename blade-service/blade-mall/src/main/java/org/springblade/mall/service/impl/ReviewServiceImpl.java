package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.dto.ReviewDTO;
import org.springblade.mall.entity.*;
import org.springblade.mall.mapper.*;
import org.springblade.mall.service.ReviewService;
import org.springblade.mall.vo.ReviewVO;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务实现类
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ReviewImageMapper reviewImageMapper;

    @Autowired
    private IUserClient userClient;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public ReviewVO createReview(ReviewDTO reviewDTO) {
        // 检查商品是否存在
        Product product = productMapper.selectById(reviewDTO.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查订单是否存在
        Order order = orderMapper.selectById(reviewDTO.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查订单状态是否已完成
        if (!order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("只能对已完成的订单进行评价");
        }

        // 检查是否已经评价过
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", reviewDTO.getProductId());
        queryWrapper.eq("order_id", reviewDTO.getOrderId());
        Review existingReview = reviewMapper.selectOne(queryWrapper);
        if (existingReview != null) {
            throw new RuntimeException("已经评价过该商品");
        }

        // 创建评价
        Review review = new Review();
        review.setProductId(reviewDTO.getProductId());
        review.setOrderId(reviewDTO.getOrderId());
        review.setOrderItemId(reviewDTO.getOrderItemId());
        review.setUserId(order.getUserId());
        review.setRating(reviewDTO.getRating());
        review.setContent(reviewDTO.getContent());
        review.setAnonymous(reviewDTO.getAnonymous() != null ? reviewDTO.getAnonymous() : 0);
        review.setStatus(1);
        review.setCreatedAt(LocalDateTime.now());

        // 保存评价
        reviewMapper.insert(review);

        // 保存评价图片
        if (reviewDTO.getImages() != null && !reviewDTO.getImages().isEmpty()) {
            for (String image : reviewDTO.getImages()) {
                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setReviewId(review.getId());
                reviewImage.setImageUrl(image);
                reviewImage.setCreatedAt(LocalDateTime.now());
                reviewImageMapper.insert(reviewImage);
            }
        }

        // 转换为VO返回
        return convertToVO(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        // 检查评价是否存在
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }

        // 删除评价图片
        QueryWrapper<ReviewImage> imageQuery = new QueryWrapper<>();
        imageQuery.eq("review_id", id);
        reviewImageMapper.delete(imageQuery);

        // 删除评价
        reviewMapper.deleteById(id);
    }

    @Override
    public ReviewVO getReviewById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        return convertToVO(review);
    }

    @Override
    public List<ReviewVO> getReviewsByProductId(Long productId) {
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("created_at");
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        return reviews.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getReviewsByUserId(Long userId) {
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("created_at");
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        return reviews.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getAllReviews() {
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        return reviews.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getReviewsByRating(Long productId, Integer rating) {
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("rating", rating);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("created_at");
        List<Review> reviews = reviewMapper.selectList(queryWrapper);
        return reviews.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewStatsVO getReviewStats(Long productId) {
        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("status", 1);
        List<Review> reviews = reviewMapper.selectList(queryWrapper);

        ReviewStatsVO stats = new ReviewStatsVO();
        stats.setTotalReviews(reviews.size());

        if (reviews.isEmpty()) {
            stats.setAverageRating(0);
            stats.setFiveStarReviews(0);
            stats.setFourStarReviews(0);
            stats.setThreeStarReviews(0);
            stats.setTwoStarReviews(0);
            stats.setOneStarReviews(0);
        } else {
            // 计算平均评分
            double totalRating = reviews.stream().mapToInt(Review::getRating).sum();
            stats.setAverageRating(totalRating / reviews.size());

            // 统计各星级评价数量
            stats.setFiveStarReviews((int) reviews.stream().filter(r -> r.getRating() == 5).count());
            stats.setFourStarReviews((int) reviews.stream().filter(r -> r.getRating() == 4).count());
            stats.setThreeStarReviews((int) reviews.stream().filter(r -> r.getRating() == 3).count());
            stats.setTwoStarReviews((int) reviews.stream().filter(r -> r.getRating() == 2).count());
            stats.setOneStarReviews((int) reviews.stream().filter(r -> r.getRating() == 1).count());
        }

        return stats;
    }

    /**
     * 将评价实体转换为VO
     */
    private ReviewVO convertToVO(Review review) {
        ReviewVO reviewVO = new ReviewVO();
        BeanUtils.copyProperties(review, reviewVO);

        // 设置评分文本
        reviewVO.setRatingText(getRatingText(review.getRating()));

        // 获取用户信息
        org.springblade.core.tool.api.R<org.springblade.system.user.entity.UserInfo> userResult = userClient.userInfo(review.getUserId());
        if (userResult.isSuccess() && userResult.getData() != null && userResult.getData().getUser() != null) {
            org.springblade.system.user.entity.User user = userResult.getData().getUser();
            reviewVO.setUserName(user.getName() != null ? user.getName() : user.getAccount());
        }

        // 获取商品信息
        Product product = productMapper.selectById(review.getProductId());
        if (product != null) {
            reviewVO.setProductName(product.getName());
        }

        // 获取订单信息
        Order order = orderMapper.selectById(review.getOrderId());
        if (order != null) {
            reviewVO.setOrderNo(order.getOrderNo());
        }

        // 获取评价图片
        QueryWrapper<ReviewImage> imageQuery = new QueryWrapper<>();
        imageQuery.eq("review_id", review.getId());
        List<ReviewImage> images = reviewImageMapper.selectList(imageQuery);
        reviewVO.setImages(images.stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList()));

        return reviewVO;
    }

    /**
     * 获取评分文本
     */
    private String getRatingText(Integer rating) {
        switch (rating) {
            case 5:
                return "非常满意";
            case 4:
                return "满意";
            case 3:
                return "一般";
            case 2:
                return "不满意";
            case 1:
                return "非常不满意";
            default:
                return "未知";
        }
    }
}


