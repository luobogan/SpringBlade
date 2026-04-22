package org.springblade.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.mall.dto.AddressDTO;
import org.springblade.mall.entity.Address;
import org.springblade.mall.vo.AddressVO;

import java.util.List;

/**
 * 地址服务接口
 */
public interface AddressService extends IService<Address> {

    /**
     * 获取用户的地址列表
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<AddressVO> getAddressList(Long userId);

    /**
     * 获取地址详情
     *
     * @param addressId 地址ID
     * @param userId 用户ID
     * @return 地址详情
     */
    AddressVO getAddressDetail(Long addressId, Long userId);

    /**
     * 创建地址
     *
     * @param addressDTO 地址信息
     * @param userId 用户ID
     * @return 创建的地址
     */
    AddressVO createAddress(AddressDTO addressDTO, Long userId);

    /**
     * 更新地址
     *
     * @param addressDTO 地址信息
     * @param userId 用户ID
     * @return 更新后的地址
     */
    AddressVO updateAddress(AddressDTO addressDTO, Long userId);

    /**
     * 删除地址
     *
     * @param addressId 地址ID
     * @param userId 用户ID
     */
    void deleteAddress(Long addressId, Long userId);

    /**
     * 设置默认地址
     *
     * @param addressId 地址ID
     * @param userId 用户ID
     */
    void setDefaultAddress(Long addressId, Long userId);

    /**
     * 获取用户的默认地址
     *
     * @param userId 用户ID
     * @return 默认地址
     */
    AddressVO getDefaultAddress(Long userId);
}



