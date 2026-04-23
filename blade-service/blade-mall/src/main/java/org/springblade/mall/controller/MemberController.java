package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.service.MemberLevelService;
import org.springblade.mall.service.MemberService;
import org.springblade.mall.vo.MemberAccountVO;
import org.springblade.mall.vo.MemberLevelVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 会员管理控制器
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@RestController
@RequestMapping("/member")
@AllArgsConstructor
public class MemberController extends BladeController {

    private MemberService memberService;

    private MemberLevelService memberLevelService;

    /**
     * 获取当前用户会员信息
     */
    @GetMapping("/info")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "获取当前用户会员信息", description = "获取当前用户会员信息")
    @PreAuth("hasAnyRole('USER', 'ADMIN')")
    public R<MemberAccountVO> getMemberInfo(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        MemberAccountVO memberInfo = memberService.getMemberInfo(user.getUserId());
        return R.data(memberInfo);
    }

    /**
     * 会员签到
     */
    @PostMapping("/checkin")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "会员签到", description = "会员签到")
    @PreAuth("hasAnyRole('USER', 'ADMIN')")
    public R checkin(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            memberService.checkin(user.getUserId());
            return R.success("签到成功");
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取会员等级列表
     */
    @GetMapping("/levels")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "获取会员等级列表", description = "获取会员等级列表")
    public R<List<MemberLevelVO>> getLevels() {
        List<MemberLevelVO> levels = memberLevelService.getAllActiveLevels();
        return R.data(levels);
    }

    /**
     * 手动触发会员等级计算（测试用）
     */
    @PostMapping("/calculate-level")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "手动触发会员等级计算", description = "手动触发会员等级计算")
    @PreAuth("hasAnyRole('USER', 'ADMIN')")
    public R calculateLevel(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        memberService.calculateMemberLevel(user.getUserId());
        return R.success("等级计算完成");
    }

    /**
     * 测试增加积分（仅开发环境）
     */
    @PostMapping("/test/add-points")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "测试增加积分", description = "传入userId和points")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R testAddPoints(
            @RequestParam Long userId,
            @RequestParam Integer points) {
        memberService.addPoints(userId, points, "TEST", null, "测试增加积分");
        return R.success("积分添加成功");
    }

    /**
     * 测试增加成长值（仅开发环境）
     */
    @PostMapping("/test/add-growth")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "测试增加成长值", description = "传入userId和growth")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public R testAddGrowth(
            @RequestParam Long userId,
            @RequestParam Integer growth) {
        memberService.addGrowth(userId, growth, "TEST", null, "测试增加成长值");
        return R.success("成长值添加成功");
    }
}



