package org.springblade.mall.service;

import org.springblade.mall.entity.MemberLevel;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.mall.vo.MemberLevelVO;

import java.util.List;

/**
 * <p>
 * 会员等级服务接口
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
public interface MemberLevelService extends IService<MemberLevel> {

    /**
     * 获取所有启用的会员等级
     *
     * @return 会员等级列表
     */
    List<MemberLevelVO> getAllActiveLevels();

    /**
     * 根据成长值计算等级
     *
     * @param growth 成长值
     * @return 匹配的会员等级
     */
    MemberLevel calculateLevelByGrowth(Integer growth);

    /**
     * 根据经验值计算等级
     *
     * @param experience 经验值
     * @return 匹配的会员等级
     */
    MemberLevel calculateLevelByExperience(Integer experience);

    /**
     * 获取等级详情（包含权益）
     *
     * @param levelId 等级 ID
     * @return 等级详情 VO
     */
    MemberLevelVO getLevelDetail(Long levelId);
}



