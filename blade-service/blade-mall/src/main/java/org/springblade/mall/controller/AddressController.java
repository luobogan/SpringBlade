package org.springblade.mall.controller;

import org.springblade.mall.dto.AddressDTO;

import org.springblade.mall.service.AddressService;
import org.springblade.mall.vo.AddressVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.secure.utils.SecureUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 地址管理控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/address")
public class AddressController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private final AddressService addressService;

    @Autowired
    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 获取用户的地址列表
     */
    @GetMapping
    public R<List<AddressVO>> getAddressList(HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("获取用户地址列表，userId: {}", userId);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        List<AddressVO> addressList = addressService.getAddressList(userId);
        return R.data(addressList, "获取成功");
    }

    /**
     * 获取地址详情
     */
    @GetMapping("/{id}")
    public R<AddressVO> getAddressDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("获取地址详情，addressId: {}, userId: {}", id, userId);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        AddressVO addressVO = addressService.getAddressDetail(id, userId);
        return R.data(addressVO, "获取成功");
    }

    /**
     * 创建地址
     */
    @PostMapping
    public R<AddressVO> createAddress(@Valid @RequestBody AddressDTO addressDTO, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("创建地址，userId: {}, addressDTO: {}", userId, addressDTO);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        AddressVO addressVO = addressService.createAddress(addressDTO, userId);
        return R.data(addressVO, "创建成功");
    }

    /**
     * 更新地址
     */
    @PutMapping("/{id}")
    public R<AddressVO> updateAddress(@PathVariable Long id,
                                          @Valid @RequestBody AddressDTO addressDTO,
                                          HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("更新地址，addressId: {}, userId: {}, addressDTO: {}", id, userId, addressDTO);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        addressDTO.setId(id);
        AddressVO addressVO = addressService.updateAddress(addressDTO, userId);
        return R.data(addressVO, "更新成功");
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("删除地址，addressId: {}, userId: {}", id, userId);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        addressService.deleteAddress(id, userId);
        return R.success("删除成功");
    }

    /**
     * 设置默认地址
     */
    @PostMapping("/{id}/default")
    public R<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("设置默认地址，addressId: {}, userId: {}", id, userId);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        addressService.setDefaultAddress(id, userId);
        return R.success("设置成功");
    }

    /**
     * 获取默认地址
     */
    @GetMapping("/default")
    public R<AddressVO> getDefaultAddress(HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("获取默认地址，userId: {}", userId);

        if (userId == null || userId == -1) {
            return R.fail("登录已过期，请重新登录");
        }

        AddressVO addressVO = addressService.getDefaultAddress(userId);
        return R.data(addressVO, "获取成功");
    }
}



