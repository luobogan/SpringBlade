package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.entity.MemberAccount;
import org.springblade.mall.entity.MemberGrowthLog;
import org.springblade.mall.entity.MemberLevel;
import org.springblade.mall.entity.MemberPointsLog;
import org.springblade.mall.mapper.MemberAccountMapper;
import org.springblade.mall.mapper.MemberGrowthLogMapper;
import org.springblade.mall.mapper.MemberLevelMapper;
import org.springblade.mall.mapper.MemberPointsLogMapper;
import org.springblade.mall.service.MemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.mall.vo.MemberAccountVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * 会员服务实现类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Service
public class MemberServiceImpl extends ServiceImpl<MemberAccountMapper, MemberAccount> implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    @Autowired
    private MemberLevelMapper memberLevelMapper;

    @Autowired
    private MemberPointsLogMapper memberPointsLogMapper;

    @Autowired
    private MemberGrowthLogMapper memberGrowthLogMapper;

    @Override
    public MemberAccountVO getMemberInfo(Long userId) {
        // 查询会员账户
        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            // 如果没有账户，创建一个默认的
            memberAccount = createDefaultMemberAccount(userId);
        }

        // 转换为 VO
        MemberAccountVO vo = new MemberAccountVO();
        BeanUtils.copyProperties(memberAccount, vo);

        // 查询会员等级信息
        if (memberAccount.getLevelId() != null) {
            MemberLevel memberLevel = memberLevelMapper.selectById(memberAccount.getLevelId());
            if (memberLevel != null) {
                vo.setLevelName(memberLevel.getName());
                vo.setDiscountRate(memberLevel.getDiscountRate());
                vo.setBenefits(memberLevel.getBenefits());
            }
        }

        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(memberAccount.getStatus()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateMemberLevel(Long userId) {
        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            return;
        }

        // 根据成长值查询应属等级
        List<MemberLevel> levels = memberLevelMapper.selectList(
            new QueryWrapper<MemberLevel>()
                .eq("status", 1)
                .le("min_growth", memberAccount.getGrowth())
                .orderByDesc("level_value")
        );

        if (!levels.isEmpty()) {
            MemberLevel targetLevel = levels.get(0);

            // 如果等级发生变化，更新等级
            if (!targetLevel.getId().equals(memberAccount.getLevelId())) {
                memberAccount.setLevelId(targetLevel.getId());

                // 如果等级不是永久有效的，设置有效期
                if (targetLevel.getDurationDays() > 0) {
                    LocalDateTime now = LocalDateTime.now();
                    memberAccount.setMembershipStart(now);
                    memberAccount.setMembershipEnd(now.plusDays(targetLevel.getDurationDays()));
                }

                this.updateById(memberAccount);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer points, String sourceType, Long sourceId, String description) {
        if (points <= 0) {
            return;
        }

        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            memberAccount = createDefaultMemberAccount(userId);
        }

        // 更新积分
        int beforePoints = memberAccount.getPoints();
        memberAccount.setPoints(beforePoints + points);
        memberAccount.setTotalPoints(memberAccount.getTotalPoints() + points);
        this.updateById(memberAccount);

        // 记录积分日志
        MemberPointsLog log = new MemberPointsLog();
        log.setUserId(userId);
        log.setPointsValue(points);
        log.setType(1); // 1: 获得
        log.setSubType(getSubType(sourceType));
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setBeforePoints(beforePoints);
        log.setAfterPoints(memberAccount.getPoints());
        log.setExpireDate(LocalDate.now().plusYears(1)); // 积分有效期 1 年
        log.setDescription(description);
        log.setCreatedAt(LocalDateTime.now());
        memberPointsLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean consumePoints(Long userId, Integer points, String sourceType, Long sourceId, String description) {
        if (points <= 0) {
            return false;
        }

        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null || memberAccount.getPoints() < points) {
            return false;
        }

        // 更新积分
        int beforePoints = memberAccount.getPoints();
        memberAccount.setPoints(beforePoints - points);
        memberAccount.setUsedPoints(memberAccount.getUsedPoints() + points);
        this.updateById(memberAccount);

        // 记录积分日志
        MemberPointsLog log = new MemberPointsLog();
        log.setUserId(userId);
        log.setPointsValue(-points); // 消费为负数
        log.setType(2); // 2: 消费
        log.setSubType(getSubType(sourceType));
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setBeforePoints(beforePoints);
        log.setAfterPoints(memberAccount.getPoints());
        log.setDescription(description);
        log.setCreatedAt(LocalDateTime.now());
        memberPointsLogMapper.insert(log);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowth(Long userId, Integer growth, String sourceType, Long sourceId, String description) {
        if (growth <= 0) {
            return;
        }

        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            memberAccount = createDefaultMemberAccount(userId);
        }

        // 更新成长值
        int beforeGrowth = memberAccount.getGrowth();
        memberAccount.setGrowth(beforeGrowth + growth);
        memberAccount.setExperience(memberAccount.getExperience() + growth);
        this.updateById(memberAccount);

        // 记录成长值日志
        MemberGrowthLog log = new MemberGrowthLog();
        log.setUserId(userId);
        log.setGrowthValue(growth);
        log.setType(getGrowthType(sourceType));
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setBeforeGrowth(beforeGrowth);
        log.setAfterGrowth(memberAccount.getGrowth());
        log.setDescription(description);
        log.setCreatedAt(LocalDateTime.now());
        memberGrowthLogMapper.insert(log);

        // 重新计算等级
        calculateMemberLevel(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkin(Long userId) {
        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            memberAccount = createDefaultMemberAccount(userId);
        }

        LocalDate today = LocalDate.now();

        // 检查今天是否已签到
        if (today.equals(memberAccount.getLastCheckin())) {
            throw new RuntimeException("今天已经签到过了");
        }

        // 更新签到信息
        memberAccount.setLastCheckin(today);

        // 判断是否连续签到
        LocalDate yesterday = today.minusDays(1);
        if (yesterday.equals(memberAccount.getLastCheckin())) {
            // 连续签到
            memberAccount.setContinuousCheckinDays(memberAccount.getContinuousCheckinDays() + 1);
        } else {
            // 中断，重新计算
            memberAccount.setContinuousCheckinDays(1);
        }

        this.updateById(memberAccount);

        // 签到奖励：10 积分 + 10 成长值
        addPoints(userId, 10, "CHECKIN", null, "每日签到");
        addGrowth(userId, 10, "CHECKIN", null, "每日签到");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsumption(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        MemberAccount memberAccount = this.getOne(new QueryWrapper<MemberAccount>().eq("user_id", userId));
        if (memberAccount == null) {
            memberAccount = createDefaultMemberAccount(userId);
        }

        // 更新消费金额和订单数
        memberAccount.setTotalConsumption(memberAccount.getTotalConsumption().add(amount));
        memberAccount.setOrderCount(memberAccount.getOrderCount() + 1);
        this.updateById(memberAccount);

        // 根据消费金额赠送积分和成长值（1 元=1 积分=1 成长值）
        int pointsAndGrowth = amount.intValue();
        if (pointsAndGrowth > 0) {
            addPoints(userId, pointsAndGrowth, "ORDER", null, "购物赠送");
            addGrowth(userId, pointsAndGrowth, "ORDER", null, "购物赠送");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkExpiredMembers() {
        LocalDateTime now = LocalDateTime.now();

        // 查询过期的会员
        List<MemberAccount> expiredAccounts = this.list(
            new QueryWrapper<MemberAccount>()
                .eq("status", 1)
                .lt("membership_end", now)
        );

        for (MemberAccount account : expiredAccounts) {
            // 更新状态为过期
            account.setStatus(2);
            this.updateById(account);

            // TODO: 可以发送过期通知
            log.info("用户 {} 的会员已过期", account.getUserId());
        }
    }

    /**
     * 创建默认会员账户
     */
    private MemberAccount createDefaultMemberAccount(Long userId) {
        MemberAccount memberAccount = new MemberAccount();
        memberAccount.setUserId(userId);
        memberAccount.setLevelId(1L); // 默认普通会员
        memberAccount.setPoints(0);
        memberAccount.setTotalPoints(0);
        memberAccount.setUsedPoints(0);
        memberAccount.setGrowth(0);
        memberAccount.setExperience(0);
        memberAccount.setStatus(0); // 非会员
        memberAccount.setTotalConsumption(BigDecimal.ZERO);
        memberAccount.setOrderCount(0);
        memberAccount.setContinuousCheckinDays(0);
        memberAccount.setCreatedAt(LocalDateTime.now());
        memberAccount.setUpdatedAt(LocalDateTime.now());

        this.save(memberAccount);
        return memberAccount;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        switch (status) {
            case 0:
                return "非会员";
            case 1:
                return "会员";
            case 2:
                return "已过期";
            default:
                return "未知";
        }
    }

    /**
     * 根据来源类型获取子类型
     */
    private Integer getSubType(String sourceType) {
        if (sourceType == null) {
            return null;
        }
        switch (sourceType.toUpperCase()) {
            case "ORDER":
                return 1; // 购物
            case "CHECKIN":
                return 2; // 签到
            case "ACTIVITY":
                return 3; // 活动
            case "EXCHANGE":
                return 4; // 兑换
            case "REFUND":
                return 5; // 退款
            default:
                return null;
        }
    }

    /**
     * 根据来源类型获取成长值类型
     */
    private Integer getGrowthType(String sourceType) {
        if (sourceType == null) {
            return 4; // 默认系统调整
        }
        switch (sourceType.toUpperCase()) {
            case "ORDER":
                return 1; // 消费
            case "CHECKIN":
                return 2; // 签到
            case "ACTIVITY":
                return 3; // 活动
            default:
                return 4; // 系统调整
        }
    }
}



