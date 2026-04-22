package org.springblade.mall.service;

import org.springblade.mall.dto.CouponDTO;

import org.springblade.mall.vo.CouponVO;
import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {
    
    /**
     * 创建优惠券
     * @param couponDTO 优惠券信息
     * @return 创建的优惠券
     */
    CouponVO createCoupon(CouponDTO couponDTO);
    
    /**
     * 更新优惠券
     * @param id 优惠券ID
     * @param couponDTO 优惠券信息
     * @return 更新后的优惠券
     */
    CouponVO updateCoupon(Long id, CouponDTO couponDTO);
    
    /**
     * 删除优惠券
     * @param id 优惠券ID
     */
    void deleteCoupon(Long id);
    
    /**
     * 获取优惠券详情
     * @param id 优惠券ID
     * @return 优惠券详情
     */
    CouponVO getCouponById(Long id);
    
    /**
     * 根据优惠券码获取优惠券
     * @param code 优惠券码
     * @return 优惠券详情
     */
    CouponVO getCouponByCode(String code);
    
    /**
     * 获取所有优惠券
     * @return 优惠券列表
     */
    List<CouponVO> getAllCoupons();
    
    /**
     * 获取活跃的优惠券
     * @return 优惠券列表
     */
    List<CouponVO> getActiveCoupons();
    
    /**
     * 发放优惠券给用户
     * @param userId 用户ID
     * @param couponId 优惠券ID
     */
    void issueCouponToUser(Long userId, Long couponId);
    
    /**
     * 获取用户的优惠券列表
     * @param userId 用户ID
     * @return 优惠券列表
     */
    List<CouponVO> getUserCoupons(Long userId);
    
    /**
     * 标记优惠券为已使用
     * @param userCouponId 用户优惠券ID
     */
    void markCouponAsUsed(Long userCouponId);
}


