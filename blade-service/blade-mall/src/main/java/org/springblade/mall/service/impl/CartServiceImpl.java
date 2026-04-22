package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.mall.dto.CartDTO;
import org.springblade.mall.entity.Cart;
import org.springblade.mall.entity.Product;
import org.springblade.mall.entity.ProductSku;
import org.springblade.mall.mapper.CartMapper;
import org.springblade.mall.mapper.ProductMapper;
import org.springblade.mall.mapper.ProductSkuMapper;
import org.springblade.mall.service.CartService;
import org.springblade.mall.vo.CartVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务实现类
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Override
    public List<CartVO> getCartList(Long userId) {
        log.info("查询用户购物车列表, userId: {}", userId);

        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreatedAt);

        List<Cart> cartList = baseMapper.selectList(queryWrapper);
        return cartList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CartVO> getCartListByPage(Long userId, Page<CartVO> page) {
        log.info("分页查询用户购物车列表，userId: {}, page: {}", userId, page);

        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreatedAt);

        // 使用 Cart 类型的 Page 进行查询
        Page<Cart> cartPage = baseMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), queryWrapper);

        // 转换为 CartVO Page
        Page<CartVO> cartVOPage = new Page<>(page.getCurrent(), page.getSize(), cartPage.getTotal());
        cartVOPage.setRecords(cartPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return cartVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO addToCart(CartDTO cartDTO, Long userId) {
        log.info("添加商品到购物车，userId: {}, productId: {}, quantity: {}",
                userId, cartDTO.getProductId(), cartDTO.getQuantity());

        // 校验购买数量
        if (cartDTO.getQuantity() == null || cartDTO.getQuantity() <= 0) {
            throw new RuntimeException("购买数量必须大于 0");
        }

        // 查询商品信息
        Product product = productMapper.selectById(cartDTO.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查商品是否上架
        if (product.getStatus() != null && product.getStatus() != 1) {
            throw new RuntimeException("商品已下架，无法添加到购物车");
        }

        // 检查库存
        if (cartDTO.getSkuId() != null) {
            // 使用 SKU 库存校验
            ProductSku sku = productSkuMapper.selectById(cartDTO.getSkuId());
            if (sku == null) {
                throw new RuntimeException("商品规格不存在");
            }
            if (sku.getStock() == null || sku.getStock() <= 0) {
                throw new RuntimeException("商品库存不足");
            }
            if (sku.getStock() < cartDTO.getQuantity()) {
                throw new RuntimeException("购买数量不能超过库存：" + sku.getStock());
            }
        } else {
            // 使用商品库存校验
            if (product.getStock() == null || product.getStock() <= 0) {
                throw new RuntimeException("商品库存不足");
            }
            if (product.getStock() < cartDTO.getQuantity()) {
                throw new RuntimeException("购买数量不能超过库存：" + product.getStock());
            }
        }

        // 检查是否已存在相同商品
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, cartDTO.getProductId());

        // 如果有规格信息，需要同时匹配规格
        if (cartDTO.getSelectedSpecs() != null && !cartDTO.getSelectedSpecs().isEmpty()) {
            queryWrapper.eq(Cart::getSelectedSpecs, cartDTO.getSelectedSpecs());
        }
        if (cartDTO.getSelectedColor() != null && !cartDTO.getSelectedColor().isEmpty()) {
            queryWrapper.eq(Cart::getSelectedColor, cartDTO.getSelectedColor());
        }
        if (cartDTO.getSelectedSize() != null && !cartDTO.getSelectedSize().isEmpty()) {
            queryWrapper.eq(Cart::getSelectedSize, cartDTO.getSelectedSize());
        }

        Cart existingCart = baseMapper.selectOne(queryWrapper);

        Cart cart;
        if (existingCart != null) {
            // 已存在，更新数量和规格信息
            int newQuantity = existingCart.getQuantity() + cartDTO.getQuantity();

            // 再次校验总数量是否超过库存
            if (cartDTO.getSkuId() != null) {
                ProductSku sku = productSkuMapper.selectById(cartDTO.getSkuId());
                if (sku.getStock() < newQuantity) {
                    throw new RuntimeException("购物车中该商品总数量不能超过库存：" + sku.getStock());
                }
            } else {
                if (product.getStock() < newQuantity) {
                    throw new RuntimeException("购物车中该商品总数量不能超过库存：" + product.getStock());
                }
            }

            existingCart.setQuantity(newQuantity);
            existingCart.setSkuId(cartDTO.getSkuId());
            existingCart.setSelectedSpecs(cartDTO.getSelectedSpecs());
            existingCart.setSelectedColor(cartDTO.getSelectedColor());
            existingCart.setSelectedSize(cartDTO.getSelectedSize());
            baseMapper.updateById(existingCart);
            cart = existingCart;
        } else {
            // 不存在，新增
            cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(cartDTO.getProductId());
            cart.setSkuId(cartDTO.getSkuId());
            cart.setQuantity(cartDTO.getQuantity());
            cart.setProductName(product.getName());
            cart.setProductImage(product.getMainImage());
            cart.setProductPrice(product.getPrice());
            cart.setSelected(0);
            cart.setSelectedSpecs(cartDTO.getSelectedSpecs());
            cart.setSelectedColor(cartDTO.getSelectedColor());
            cart.setSelectedSize(cartDTO.getSelectedSize());
            baseMapper.insert(cart);
        }

        log.info("添加成功, cartId: {}", cart.getId());
        return convertToVO(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO updateCartItem(Long cartId, Integer quantity, Long userId) {
        log.info("更新购物车商品数量, cartId: {}, quantity: {}, userId: {}",
                cartId, quantity, userId);

        // 检查购物车项是否存在
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 检查是否是用户的购物车
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车项");
        }

        // 查询商品库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查库存
        if (cart.getSkuId() != null) {
            ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
            if (sku == null) {
                throw new RuntimeException("商品规格不存在");
            }
            if (sku.getStock() < quantity) {
                throw new RuntimeException("库存不足");
            }
        } else {
            if (product.getStock() < quantity) {
                throw new RuntimeException("库存不足");
            }
        }

        // 更新数量
        cart.setQuantity(quantity);
        baseMapper.updateById(cart);

        log.info("更新成功, cartId: {}", cartId);
        return convertToVO(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO updateCartItemSku(Long cartId, Long skuId, Long userId) {
        log.info("更新购物车商品SKU, cartId: {}, skuId: {}, userId: {}",
                cartId, skuId, userId);

        // 检查购物车项是否存在
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 检查是否是用户的购物车
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车项");
        }

        // 检查SKU是否存在
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new RuntimeException("商品规格不存在");
        }

        // 检查SKU是否属于同一商品
        if (!sku.getProductId().equals(cart.getProductId())) {
            throw new RuntimeException("SKU不属于同一商品");
        }

        // 检查库存
        if (sku.getStock() < cart.getQuantity()) {
            throw new RuntimeException("库存不足");
        }

        // 更新SKU ID
        cart.setSkuId(skuId);

        // 根据SKU信息更新规格信息
        // 构建规格JSON字符串
        StringBuilder specsBuilder = new StringBuilder();
        specsBuilder.append("{");
        boolean hasSpecs = false;

        if (sku.getSpec1() != null && !sku.getSpec1().isEmpty()) {
            specsBuilder.append("\"spec1\":\"").append(sku.getSpec1()).append("\"");
            hasSpecs = true;
        }
        if (sku.getSpec2() != null && !sku.getSpec2().isEmpty()) {
            if (hasSpecs) specsBuilder.append(",");
            specsBuilder.append("\"spec2\":\"").append(sku.getSpec2()).append("\"");
            hasSpecs = true;
        }
        if (sku.getSpec3() != null && !sku.getSpec3().isEmpty()) {
            if (hasSpecs) specsBuilder.append(",");
            specsBuilder.append("\"spec3\":\"").append(sku.getSpec3()).append("\"");
            hasSpecs = true;
        }
        if (sku.getSpec4() != null && !sku.getSpec4().isEmpty()) {
            if (hasSpecs) specsBuilder.append(",");
            specsBuilder.append("\"spec4\":\"").append(sku.getSpec4()).append("\"");
        }
        specsBuilder.append("}");

        cart.setSelectedSpecs(specsBuilder.toString());
        cart.setSelectedColor(sku.getSpec1()); // 假设 spec1 是颜色
        cart.setSelectedSize(sku.getSpec2()); // 假设 spec2 是尺寸

        baseMapper.updateById(cart);

        log.info("更新成功, cartId: {}, skuId: {}, specs: {}", cartId, skuId, specsBuilder.toString());
        return convertToVO(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCartItem(Long cartId, Long userId) {
        log.info("删除购物车项, cartId: {}, userId: {}", cartId, userId);

        // 检查购物车项是否存在
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 检查是否是用户的购物车
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车项");
        }

        baseMapper.deleteById(cartId);

        log.info("删除成功, cartId: {}", cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCartItems(List<Long> cartIds, Long userId) {
        log.info("批量删除购物车项, cartIds: {}, userId: {}", cartIds, userId);

        // 检查是否都是用户的购物车项
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Cart::getId, cartIds)
                .eq(Cart::getUserId, userId);

        List<Cart> cartItems = baseMapper.selectList(queryWrapper);
        if (cartItems.size() != cartIds.size()) {
            throw new RuntimeException("包含非当前用户的购物车项");
        }

        baseMapper.deleteBatchIds(cartIds);

        log.info("批量删除成功, count: {}", cartIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleSelect(Long cartId, Integer selected, Long userId) {
        log.info("切换选中状态, cartId: {}, selected: {}, userId: {}", cartId, selected, userId);

        // 检查购物车项是否存在
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 检查是否是用户的购物车
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车项");
        }

        // 更新选中状态
        cart.setSelected(selected);
        baseMapper.updateById(cart);

        log.info("切换成功, cartId: {}", cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleSelectAll(Integer selected, Long userId) {
        log.info("全选/全不选, selected: {}, userId: {}", selected, userId);

        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);

        Cart update = new Cart();
        update.setSelected(selected);

        baseMapper.update(update, queryWrapper);

        log.info("切换成功, userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        log.info("清空购物车, userId: {}", userId);

        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);

        baseMapper.delete(queryWrapper);

        log.info("清空成功, userId: {}", userId);
    }

    @Override
    public List<CartVO> getSelectedItems(Long userId) {
        log.info("获取选中的购物车项, userId: {}", userId);

        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getSelected, 1);

        List<Cart> cartList = baseMapper.selectList(queryWrapper);
        return cartList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 VO
     *
     * @param cart 购物车实体
     * @return CartVO
     */
    private CartVO convertToVO(Cart cart) {
        CartVO cartVO = new CartVO();
        BeanUtils.copyProperties(cart, cartVO);

        // 查询库存和价格
        if (cart.getSkuId() != null) {
            ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
            if (sku != null) {
                cartVO.setStock(sku.getStock());
                cartVO.setProductPrice(sku.getPrice());
                // 优先使用促销价格，否则使用 SKU 价格
                cartVO.setProductCurrentPrice(sku.getPromotionPrice() != null ? sku.getPromotionPrice() : sku.getPrice());
                // 由于 SKU 没有会员价格，暂时使用商品的价格
                Product product = productMapper.selectById(cart.getProductId());
                if (product != null) {
                    cartVO.setProductCurrentMemberPrice(product.getPrice());
                } else {
                    cartVO.setProductCurrentMemberPrice(sku.getPrice());
                }
            }
        } else {
            // 查询商品库存和价格
            Product product = productMapper.selectById(cart.getProductId());
            if (product != null) {
                cartVO.setStock(product.getStock());
                cartVO.setProductPrice(product.getPrice());
                cartVO.setProductCurrentPrice(product.getPrice());
                cartVO.setProductCurrentMemberPrice(product.getPrice());
            }
        }

        return cartVO;
    }
}



