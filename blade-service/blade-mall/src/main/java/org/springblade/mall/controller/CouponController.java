package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.CouponDTO;
import org.springblade.mall.service.CouponService;
import org.springblade.mall.vo.CouponVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/coupons")
@AllArgsConstructor
public class CouponController extends BladeController {

    private CouponService couponService;

    /**
     * 创建优惠券
     * @param couponDTO 优惠券信息
     * @return 创建的优惠券
     */
    @PostMapping
    @ApiOperationSupport(order = 1)
    @Operation(summary = "创建优惠券", description = "传入CouponDTO")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R<CouponVO> createCoupon(@RequestBody CouponDTO couponDTO) {
        CouponVO couponVO = couponService.createCoupon(couponDTO);
        return R.data(couponVO);
    }

    /**
     * 更新优惠券
     * @param id 优惠券ID
     * @param couponDTO 优惠券信息
     * @return 更新后的优惠券
     */
    @PutMapping("/{id}")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "更新优惠券", description = "传入id和CouponDTO")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R<CouponVO> updateCoupon(@PathVariable Long id, @RequestBody CouponDTO couponDTO) {
        CouponVO couponVO = couponService.updateCoupon(id, couponDTO);
        return R.data(couponVO);
    }

    /**
     * 删除优惠券
     * @param id 优惠券ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "删除优惠券", description = "传入id")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return R.success();
    }

    /**
     * 获取优惠券详情
     * @param id 优惠券ID
     * @return 优惠券详情
     */
    @GetMapping("/{id}")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "获取优惠券详情", description = "传入id")
    public R<CouponVO> getCouponById(@PathVariable Long id) {
        CouponVO couponVO = couponService.getCouponById(id);
        return R.data(couponVO);
    }

    /**
     * 根据优惠券码获取优惠券
     * @param code 优惠券码
     * @return 优惠券详情
     */
    @GetMapping("/code/{code}")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "根据优惠券码获取优惠券", description = "传入code")
    @PreAuth("permitAll()")
    public R<CouponVO> getCouponByCode(@PathVariable String code) {
        CouponVO couponVO = couponService.getCouponByCode(code);
        return R.data(couponVO);
    }

    /**
     * 获取所有优惠券
     * @return 优惠券列表
     */
    @GetMapping
    @ApiOperationSupport(order = 6)
    @Operation(summary = "获取所有优惠券", description = "获取所有优惠券")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R<List<CouponVO>> getAllCoupons() {
        List<CouponVO> coupons = couponService.getAllCoupons();
        return R.data(coupons);
    }

    /**
     * 获取活跃的优惠券
     * @return 优惠券列表
     */
    @GetMapping("/active")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "获取活跃的优惠券", description = "获取活跃的优惠券")
    public R<List<CouponVO>> getActiveCoupons() {
        List<CouponVO> coupons = couponService.getActiveCoupons();
        return R.data(coupons);
    }

    /**
     * 发放优惠券给用户
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 操作结果
     */
    @PostMapping("/issue")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "发放优惠券给用户", description = "传入userId和couponId")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R issueCouponToUser(@RequestParam Long userId, @RequestParam Long couponId) {
        couponService.issueCouponToUser(userId, couponId);
        return R.success();
    }

    /**
     * 获取用户的优惠券列表
     * @param userId 用户ID
     * @return 优惠券列表
     */
    @GetMapping("/user/{userId}")
    @ApiOperationSupport(order = 9)
    @Operation(summary = "获取用户的优惠券列表", description = "传入userId")
    public R<List<CouponVO>> getUserCoupons(@PathVariable Long userId) {
        List<CouponVO> coupons = couponService.getUserCoupons(userId);
        return R.data(coupons);
    }

    /**
     * 标记优惠券为已使用
     * @param userCouponId 用户优惠券ID
     * @return 操作结果
     */
    @PutMapping("/use/{userCouponId}")
    @ApiOperationSupport(order = 10)
    @Operation(summary = "标记优惠券为已使用", description = "传入userCouponId")
    public R markCouponAsUsed(@PathVariable Long userCouponId) {
        couponService.markCouponAsUsed(userCouponId);
        return R.success();
    }
}



