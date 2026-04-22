package org.springblade.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.entity.MemberBenefit;
import org.springblade.mall.mapper.MemberBenefitMapper;
import org.springblade.mall.service.MemberLevelService;
import org.springblade.mall.vo.MemberBenefitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 后台会员权益管理控制器
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/admin/member-benefits")
@AllArgsConstructor
@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
public class AdminMemberBenefitController extends BladeController {

    private MemberBenefitMapper memberBenefitMapper;

    private MemberLevelService memberLevelService;

    /**
     * 获取会员权益列表
     */
    @GetMapping
    @ApiOperationSupport(order = 1)
    @Operation(summary = "获取会员权益列表", description = "传入levelId")
    public R<List<MemberBenefitVO>> getList(
            @RequestParam(required = false) Long levelId) {

        LambdaQueryWrapper<MemberBenefit> wrapper = new LambdaQueryWrapper<>();
        if (levelId != null) {
            wrapper.eq(MemberBenefit::getLevelId, levelId);
        }
        wrapper.orderByAsc(MemberBenefit::getSortOrder);

        List<MemberBenefit> benefits = memberBenefitMapper.selectList(wrapper);
        List<MemberBenefitVO> benefitVOs = benefits.stream()
            .map(this::convertToBenefitVO)
            .collect(Collectors.toList());

        return R.data(benefitVOs);
    }

    /**
     * 获取会员权益详情
     */
    @GetMapping("/{id}")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "获取会员权益详情", description = "传入id")
    public R<MemberBenefitVO> getById(@PathVariable Long id) {
        MemberBenefit benefit = memberBenefitMapper.selectById(id);
        if (benefit == null) {
            return R.fail("权益不存在");
        }

        MemberBenefitVO benefitVO = convertToBenefitVO(benefit);
        return R.data(benefitVO);
    }

    /**
     * 创建会员权益
     */
    @PostMapping
    @ApiOperationSupport(order = 3)
    @Operation(summary = "创建会员权益", description = "传入MemberBenefitVO")
    public R<MemberBenefitVO> create(@RequestBody MemberBenefitVO benefitVO) {
        MemberBenefit benefit = new MemberBenefit();
        BeanUtils.copyProperties(benefitVO, benefit);

        memberBenefitMapper.insert(benefit);
        benefitVO.setId(benefit.getId());

        return R.data(benefitVO);
    }

    /**
     * 更新会员权益
     */
    @PutMapping("/{id}")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "更新会员权益", description = "传入id和MemberBenefitVO")
    public R<MemberBenefitVO> update(
            @PathVariable Long id,
            @RequestBody MemberBenefitVO benefitVO) {

        MemberBenefit benefit = memberBenefitMapper.selectById(id);
        if (benefit == null) {
            return R.fail("权益不存在");
        }

        BeanUtils.copyProperties(benefitVO, benefit);
        benefit.setId(id);

        memberBenefitMapper.updateById(benefit);
        return R.data(benefitVO);
    }

    /**
     * 删除会员权益
     */
    @DeleteMapping("/{id}")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "删除会员权益", description = "传入id")
    public R delete(@PathVariable Long id) {
        memberBenefitMapper.deleteById(id);
        return R.success("删除成功");
    }

    /**
     * 批量创建会员权益
     */
    @PostMapping("/level/{levelId}/batch")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "批量创建会员权益", description = "传入levelId和MemberBenefitVO列表")
    public R<List<MemberBenefitVO>> batchCreate(
            @PathVariable Long levelId,
            @RequestBody List<MemberBenefitVO> benefitVOs) {

        List<MemberBenefitVO> created = benefitVOs.stream()
            .map(vo -> {
                vo.setLevelId(levelId);
                MemberBenefit benefit = new MemberBenefit();
                BeanUtils.copyProperties(vo, benefit);
                memberBenefitMapper.insert(benefit);
                vo.setId(benefit.getId());
                return vo;
            })
            .collect(Collectors.toList());

        return R.data(created);
    }

    // ==================== 辅助方法 ====================

    private MemberBenefitVO convertToBenefitVO(MemberBenefit benefit) {
        MemberBenefitVO vo = new MemberBenefitVO();
        BeanUtils.copyProperties(benefit, vo);

        // 获取等级名称
        if (benefit.getLevelId() != null) {
            var level = memberLevelService.getById(benefit.getLevelId());
            if (level != null) {
                vo.setLevelName(level.getName());
            }
        }

        return vo;
    }
}



