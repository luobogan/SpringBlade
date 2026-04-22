package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.mall.dto.AddressDTO;
import org.springblade.mall.entity.Address;
import org.springblade.mall.mapper.AddressMapper;
import org.springblade.mall.service.AddressService;
import org.springblade.mall.vo.AddressVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springblade.core.secure.utils.SecureUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 地址服务实现类
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

    @Override
    public List<AddressVO> getAddressList(Long userId) {
        log.info("查询用户地址列表, userId: {}", userId);

        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt);

        List<Address> addressList = baseMapper.selectList(queryWrapper);
        return addressList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressVO getAddressDetail(Long addressId, Long userId) {
        log.info("查询地址详情, addressId: {}, userId: {}", addressId, userId);

        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getId, addressId)
                .eq(Address::getUserId, userId);

        Address address = baseMapper.selectOne(queryWrapper);
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        return convertToVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO createAddress(AddressDTO addressDTO, Long userId) {
        log.info("创建地址, userId: {}, addressDTO: {}", userId, addressDTO);

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);
        address.setConsignee(addressDTO.getName()); // 设置收货人姓名到 consignee 字段
        address.setIsDefault(addressDTO.getIsDefault()); // 显式设置 isDefault 字段

        // 如果设置为默认地址，先取消其他默认地址
        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            cancelOtherDefaultAddresses(userId);
        }

        baseMapper.insert(address);

        log.info("地址创建成功, addressId: {}", address.getId());
        return convertToVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO updateAddress(AddressDTO addressDTO, Long userId) {
        log.info("更新地址, userId: {}, addressDTO: {}", userId, addressDTO);

        // 检查地址是否存在
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getId, addressDTO.getId())
                .eq(Address::getUserId, userId);
        Address existingAddress = baseMapper.selectOne(queryWrapper);
        if (existingAddress == null) {
            throw new RuntimeException("地址不存在");
        }

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);
        address.setConsignee(addressDTO.getName()); // 设置收货人姓名到 consignee 字段
        address.setIsDefault(addressDTO.getIsDefault()); // 显式设置 isDefault 字段

        // 如果设置为默认地址，先取消其他默认地址
        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            cancelOtherDefaultAddresses(userId);
        }

        baseMapper.updateById(address);

        log.info("地址更新成功, addressId: {}", address.getId());
        return convertToVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long addressId, Long userId) {
        log.info("删除地址, addressId: {}, userId: {}", addressId, userId);

        // 检查地址是否存在
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getId, addressId)
                .eq(Address::getUserId, userId);
        Address address = baseMapper.selectOne(queryWrapper);
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        baseMapper.deleteById(addressId);

        log.info("地址删除成功, addressId: {}", addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long addressId, Long userId) {
        log.info("设置默认地址, addressId: {}, userId: {}", addressId, userId);

        // 检查地址是否存在
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getId, addressId)
                .eq(Address::getUserId, userId);
        Address address = baseMapper.selectOne(queryWrapper);
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        // 取消其他默认地址
        cancelOtherDefaultAddresses(userId);

        // 设置该地址为默认
        address.setIsDefault(true);
        baseMapper.updateById(address);

        log.info("默认地址设置成功, addressId: {}", addressId);
    }

    @Override
    public AddressVO getDefaultAddress(Long userId) {
        log.info("查询用户默认地址, userId: {}", userId);

        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, true)
                .last("LIMIT 1");

        Address address = baseMapper.selectOne(queryWrapper);
        if (address == null) {
            return null;
        }

        return convertToVO(address);
    }

    /**
     * 取消用户的其他默认地址
     *
     * @param userId 用户ID
     */
    private void cancelOtherDefaultAddresses(Long userId) {
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, true);

        Address update = new Address();
        update.setIsDefault(false);

        baseMapper.update(update, queryWrapper);
    }

    /**
     * 转换为 VO
     *
     * @param address 地址实体
     * @return 地址VO
     */
    private AddressVO convertToVO(Address address) {
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(address, addressVO);
        return addressVO;
    }
}



