package org.springblade.mall.service;

import org.springblade.mall.dto.ReviewDTO;

import org.springblade.mall.vo.ReviewVO;
import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService {
    
    /**
     * 创建评价
     * @param reviewDTO 评价信息
     * @return 创建的评价
     */
    ReviewVO createReview(ReviewDTO reviewDTO);
    
    /**
     * 删除评价
     * @param id 评价ID
     */
    void deleteReview(Long id);
    
    /**
     * 获取评价详情
     * @param id 评价ID
     * @return 评价详情
     */
    ReviewVO getReviewById(Long id);
    
    /**
     * 获取商品评价列表
     * @param productId 商品ID
     * @return 评价列表
     */
    List<ReviewVO> getReviewsByProductId(Long productId);
    
    /**
     * 获取用户评价列表
     * @param userId 用户ID
     * @return 评价列表
     */
    List<ReviewVO> getReviewsByUserId(Long userId);
    
    /**
     * 获取所有评价
     * @return 评价列表
     */
    List<ReviewVO> getAllReviews();
    
    /**
     * 根据评分获取评价
     * @param productId 商品ID
     * @param rating 评分
     * @return 评价列表
     */
    List<ReviewVO> getReviewsByRating(Long productId, Integer rating);
    
    /**
     * 获取商品评价统计
     * @param productId 商品ID
     * @return 评价统计
     */
    ReviewStatsVO getReviewStats(Long productId);
    
    /**
     * 评价统计VO
     */
    class ReviewStatsVO {
        private double averageRating;
        private int totalReviews;
        private int fiveStarReviews;
        private int fourStarReviews;
        private int threeStarReviews;
        private int twoStarReviews;
        private int oneStarReviews;

        // Getter 和 Setter 方法
        public double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(double averageRating) {
            this.averageRating = averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public void setTotalReviews(int totalReviews) {
            this.totalReviews = totalReviews;
        }

        public int getFiveStarReviews() {
            return fiveStarReviews;
        }

        public void setFiveStarReviews(int fiveStarReviews) {
            this.fiveStarReviews = fiveStarReviews;
        }

        public int getFourStarReviews() {
            return fourStarReviews;
        }

        public void setFourStarReviews(int fourStarReviews) {
            this.fourStarReviews = fourStarReviews;
        }

        public int getThreeStarReviews() {
            return threeStarReviews;
        }

        public void setThreeStarReviews(int threeStarReviews) {
            this.threeStarReviews = threeStarReviews;
        }

        public int getTwoStarReviews() {
            return twoStarReviews;
        }

        public void setTwoStarReviews(int twoStarReviews) {
            this.twoStarReviews = twoStarReviews;
        }

        public int getOneStarReviews() {
            return oneStarReviews;
        }

        public void setOneStarReviews(int oneStarReviews) {
            this.oneStarReviews = oneStarReviews;
        }
    }
}


