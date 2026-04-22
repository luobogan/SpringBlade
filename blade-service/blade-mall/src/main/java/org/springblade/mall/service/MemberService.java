package org.springblade.mall.service;

import org.springblade.mall.entity.MemberAccount;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.mall.vo.MemberAccountVO;
import java.math.BigDecimal;

/**
 * <p>
 * 会员服务接口
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
public interface MemberService extends IService<MemberAccount> {

    /**
     * 获取用户会员信息
     *
     * @param userId 用户 ID
     * @return 会员信息 VO
     */
    MemberAccountVO getMemberInfo(Long userId);

    /**
     * 计算并更新用户会员等级
     *
     * @param userId 用户 ID
     */
    void calculateMemberLevel(Long userId);

    /**
     * 增加积分
     *
     * @param userId      用户 ID
     * @param points      积分值
     * @param sourceType  来源类型（ORDER, ACTIVITY, SYSTEM）
     * @param sourceId    来源 ID
     * @param description 描述
     */
    void addPoints(Long userId, Integer points, String sourceType, Long sourceId, String description);

    /**
     * 消费积分
     *
     * @param userId      用户 ID
     * @param points      积分值
     * @param sourceType  来源类型
     * @param sourceId    来源 ID
     * @param description 描述
     * @return 是否成功
     */
    boolean consumePoints(Long userId, Integer points, String sourceType, Long sourceId, String description);

    /**
     * 增加成长值
     *
     * @param userId      用户 ID
     * @param growth      成长值
     * @param sourceType  来源类型
     * @param sourceId    来源 ID
     * @param description 描述
     */
    void addGrowth(Long userId, Integer growth, String sourceType, Long sourceId, String description);

    /**
     * 签到
     *
     * @param userId 用户 ID
     */
    void checkin(Long userId);

    /**
     * 更新消费金额
     *
     * @param userId 用户 ID
     * @param amount 消费金额
     */
    void updateConsumption(Long userId, BigDecimal amount);

    /**
     * 检查并更新过期会员
     */
    void checkExpiredMembers();
}



