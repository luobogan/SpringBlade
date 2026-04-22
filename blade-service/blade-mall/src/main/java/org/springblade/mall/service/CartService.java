package org.springblade.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.mall.dto.CartDTO;
import org.springblade.mall.entity.Cart;
import org.springblade.mall.vo.CartVO;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService extends IService<Cart> {

    /**
     * 获取用户的购物车列表
     *
     * @param userId 用户 ID
     * @return 购物车列表
     */
    List<CartVO> getCartList(Long userId);

    /**
     * 分页获取用户的购物车列表
     *
     * @param userId 用户 ID
     * @param page 分页参数
     * @return 购物车分页数据
     */
    Page<CartVO> getCartListByPage(Long userId, Page<CartVO> page);

    /**
     * 添加商品到购物车
     *
     * @param cartDTO 购物车信息
     * @param userId 用户ID
     * @return 添加后的购物车项
     */
    CartVO addToCart(CartDTO cartDTO, Long userId);

    /**
     * 更新购物车商品数量
     *
     * @param cartId 购物车ID
     * @param quantity 商品数量
     * @param userId 用户ID
     * @return 更新后的购物车项
     */
    CartVO updateCartItem(Long cartId, Integer quantity, Long userId);

    /**
     * 更新购物车商品SKU
     *
     * @param cartId 购物车ID
     * @param skuId SKU ID
     * @param userId 用户ID
     * @return 更新后的购物车项
     */
    CartVO updateCartItemSku(Long cartId, Long skuId, Long userId);

    /**
     * 删除购物车项
     *
     * @param cartId 购物车ID
     * @param userId 用户ID
     */
    void deleteCartItem(Long cartId, Long userId);

    /**
     * 批量删除购物车项
     *
     * @param cartIds 购物车ID列表
     * @param userId 用户ID
     */
    void batchDeleteCartItems(List<Long> cartIds, Long userId);

    /**
     * 切换选中状态
     *
     * @param cartId 购物车ID
     * @param selected 是否选中
     * @param userId 用户ID
     */
    void toggleSelect(Long cartId, Integer selected, Long userId);

    /**
     * 全选/全不选
     *
     * @param selected 是否选中
     * @param userId 用户ID
     */
    void toggleSelectAll(Integer selected, Long userId);

    /**
     * 清空购物车
     *
     * @param userId 用户ID
     */
    void clearCart(Long userId);

    /**
     * 获取选中的购物车项
     *
     * @param userId 用户ID
     * @return 选中的购物车项
     */
    List<CartVO> getSelectedItems(Long userId);
}



