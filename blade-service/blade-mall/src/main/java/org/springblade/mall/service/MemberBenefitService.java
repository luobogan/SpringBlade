package org.springblade.mall.service;

import org.springblade.mall.entity.MemberBenefit;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 会员权益服务接口
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
public interface MemberBenefitService extends IService<MemberBenefit> {

    /**
     * 根据会员等级 ID 查询权益列表
     *
     * @param levelId 会员等级 ID
     * @return 权益列表
     */
    List<MemberBenefit> getByLevelId(Long levelId);

    /**
     * 获取所有启用的权益类型
     *
     * @return 权益类型列表
     */
    List<String> getBenefitTypes();
}



