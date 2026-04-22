package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.mall.dto.CouponDTO;
import org.springblade.mall.entity.Coupon;
import org.springblade.mall.exception.CouponNotFoundException;
import org.springblade.mall.entity.UserCoupon;
import org.springblade.mall.mapper.CouponMapper;
import org.springblade.mall.mapper.UserCouponMapper;
import org.springblade.mall.service.CouponService;
import org.springblade.mall.vo.CouponVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现类
 */
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    @Transactional
    public CouponVO createCoupon(CouponDTO couponDTO) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(couponDTO, coupon);
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        couponMapper.insert(coupon);
        return convertToVO(coupon);
    }

    @Override
    @Transactional
    public CouponVO updateCoupon(Long id, CouponDTO couponDTO) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CouponNotFoundException("Coupon not found");
        }
        BeanUtils.copyProperties(couponDTO, coupon);
        coupon.setId(id);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponMapper.updateById(coupon);
        return convertToVO(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CouponNotFoundException("Coupon not found");
        }
        couponMapper.deleteById(id);
    }

    @Override
    public CouponVO getCouponById(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new CouponNotFoundException("Coupon not found");
        }
        return convertToVO(coupon);
    }

    @Override
    public CouponVO getCouponByCode(String code) {
        LambdaQueryWrapper<Coupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Coupon::getCode, code);
        Coupon coupon = couponMapper.selectOne(queryWrapper);
        if (coupon == null) {
            throw new CouponNotFoundException("Coupon not found");
        }
        return convertToVO(coupon);
    }

    @Override
    public List<CouponVO> getAllCoupons() {
        List<Coupon> coupons = couponMapper.selectList(null);
        return coupons.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<CouponVO> getActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .eq(Coupon::getStatus, 1);
        List<Coupon> coupons = couponMapper.selectList(queryWrapper);
        return coupons.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void issueCouponToUser(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new CouponNotFoundException("Coupon not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new RuntimeException("Coupon is not active");
        }

        LambdaQueryWrapper<UserCoupon> userCouponQueryWrapper = new LambdaQueryWrapper<>();
        userCouponQueryWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId);
        if (userCouponMapper.selectCount(userCouponQueryWrapper) > 0) {
            throw new RuntimeException("User already has this coupon");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(1);
        userCoupon.setReceivedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public List<CouponVO> getUserCoupons(Long userId) {
        LambdaQueryWrapper<UserCoupon> userCouponQueryWrapper = new LambdaQueryWrapper<>();
        userCouponQueryWrapper.eq(UserCoupon::getUserId, userId);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(userCouponQueryWrapper);

        return userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
                    CouponVO couponVO = convertToVO(coupon);
                    couponVO.setStatus(userCoupon.getStatus());
                    couponVO.setUseTime(userCoupon.getUsedAt());
                    return couponVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markCouponAsUsed(Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new RuntimeException("User coupon not found");
        }
        if (userCoupon.getStatus() != 1) {
            throw new RuntimeException("Coupon is not available");
        }
        userCoupon.setStatus(2);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
    }

    private CouponVO convertToVO(Coupon coupon) {
        CouponVO couponVO = new CouponVO();
        
        // 显式映射字段，避免BeanUtils.copyProperties可能导致的类型不匹配
        couponVO.setId(coupon.getId());
        couponVO.setName(coupon.getName());
        couponVO.setCode(coupon.getCode());
        couponVO.setType(coupon.getType() == 1 ? "fixed" : "percent");
        couponVO.setTypeText(coupon.getType() == 1 ? "Fixed Amount" : "Percentage");
        couponVO.setValue(coupon.getValue() != null ? coupon.getValue().doubleValue() : 0.0);
        couponVO.setAmount(coupon.getValue() != null ? coupon.getValue().doubleValue() : 0.0);
        couponVO.setMinSpend(coupon.getMinSpend() != null ? coupon.getMinSpend().doubleValue() : 0.0);
        couponVO.setMinAmount(coupon.getMinSpend() != null ? coupon.getMinSpend().doubleValue() : 0.0);
        couponVO.setMaxDiscount(coupon.getMaxDiscount() != null ? coupon.getMaxDiscount().doubleValue() : 0.0);
        couponVO.setStartTime(coupon.getStartTime());
        couponVO.setEndTime(coupon.getEndTime());
        couponVO.setUsageLimit(coupon.getPerUserLimit());
        couponVO.setUsageCount(coupon.getUsedQuantity());
        LocalDateTime now = LocalDateTime.now();
        couponVO.setIsExpired(now.isAfter(coupon.getEndTime()));
        couponVO.setIsAvailable(now.isBefore(coupon.getEndTime()) && now.isAfter(coupon.getStartTime()) && coupon.getStatus() == 1);
        couponVO.setIsActive(coupon.getStatus() == 1);
        couponVO.setCreatedAt(coupon.getCreatedAt());
        couponVO.setUpdatedAt(coupon.getUpdatedAt());
        
        return couponVO;
    }
}


