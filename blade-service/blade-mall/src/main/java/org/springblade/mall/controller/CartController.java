package org.springblade.mall.controller;

import org.springblade.mall.dto.CartDTO;
import org.springblade.mall.service.CartService;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.mall.vo.CartVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springblade.core.launch.constant.AppConstant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 购物车管理控制器
 */
@RestController
@RequestMapping("/cart")
public class CartController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    /**
     * 获取用户的购物车列表
     */
    @GetMapping
    public R<List<CartVO>> getCartList(HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("获取用户购物车列表，userId: {}", userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        List<CartVO> cartList = cartService.getCartList(userId);
        return R.data(cartList, "获取成功");
    }

    /**
     * 分页获取用户的购物车列表
     */
    @PostMapping("/selectPage")
    public R<Page<CartVO>> getCartListByPage(@RequestBody(required = false) Page<CartVO> page,
                                                      HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("分页获取用户购物车列表，userId: {}, page: {}", userId, page);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        if (page == null) {
            page = new Page<>(1, 10);
        }

        Page<CartVO> cartPage = cartService.getCartListByPage(userId, page);
        return R.data(cartPage, "获取成功");
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping
    public R<CartVO> addToCart(@Valid @RequestBody CartDTO cartDTO, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("添加商品到购物车, userId: {}, productId: {}, quantity: {}",
                userId, cartDTO.getProductId(), cartDTO.getQuantity());

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        CartVO cartVO = cartService.addToCart(cartDTO, userId);
        return R.data(cartVO, "添加成功");
    }

    /**
     * 更新购物车商品数量
     */
    @PutMapping("/{id}")
    public R<CartVO> updateCartItem(@PathVariable Long id,
                                          @RequestParam Integer quantity,
                                          HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("更新购物车商品数量, cartId: {}, quantity: {}, userId: {}", id, quantity, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        CartVO cartVO = cartService.updateCartItem(id, quantity, userId);
        return R.data(cartVO, "更新成功");
    }

    /**
     * 更新购物车商品SKU
     */
    @PutMapping("/{id}/sku")
    public R<CartVO> updateCartItemSku(@PathVariable Long id,
                                             @RequestParam Long skuId,
                                             HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("更新购物车商品SKU, cartId: {}, skuId: {}, userId: {}", id, skuId, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        CartVO cartVO = cartService.updateCartItemSku(id, skuId, userId);
        return R.data(cartVO, "更新成功");
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteCartItem(@PathVariable Long id, HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("删除购物车项, cartId: {}, userId: {}", id, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        cartService.deleteCartItem(id, userId);
        return R.success("删除成功");
    }

    /**
     * 批量删除购物车项
     */
    @DeleteMapping("/batch")
    public R<Void> batchDeleteCartItems(@RequestBody List<Long> cartIds,
                                              HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("批量删除购物车项, cartIds: {}, userId: {}", cartIds, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        cartService.batchDeleteCartItems(cartIds, userId);
        return R.success("删除成功");
    }

    /**
     * 切换选中状态
     */
    @PutMapping("/{id}/select")
    public R<Void> toggleSelect(@PathVariable Long id,
                                      @RequestParam Integer selected,
                                      HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("切换选中状态, cartId: {}, selected: {}, userId: {}", id, selected, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        cartService.toggleSelect(id, selected, userId);
        return R.success("切换成功");
    }

    /**
     * 全选/全不选
     */
    @PutMapping("/select-all")
    public R<Void> toggleSelectAll(@RequestParam Integer selected,
                                          HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("全选/全不选, selected: {}, userId: {}", selected, userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        cartService.toggleSelectAll(selected, userId);
        return R.success("切换成功");
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public R<Void> clearCart(HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("清空购物车, userId: {}", userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        cartService.clearCart(userId);
        return R.success("清空成功");
    }

    /**
     * 获取选中的购物车项
     */
    @GetMapping("/selected")
    public R<List<CartVO>> getSelectedItems(HttpServletRequest request) {
        Long userId = SecureUtil.getUserId(request);
        log.info("获取选中的购物车项, userId: {}", userId);

        if (userId == null) {
            return R.fail("登录已过期，请重新登录");
        }

        List<CartVO> selectedItems = cartService.getSelectedItems(userId);
        return R.data(selectedItems, "获取成功");
    }
}



