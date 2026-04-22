package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.entity.MemberBenefit;
import org.springblade.mall.mapper.MemberBenefitMapper;
import org.springblade.mall.service.MemberBenefitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 会员权益服务实现类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Service
public class MemberBenefitServiceImpl extends ServiceImpl<MemberBenefitMapper, MemberBenefit> implements MemberBenefitService {

    @Override
    public List<MemberBenefit> getByLevelId(Long levelId) {
        return this.list(
            new QueryWrapper<MemberBenefit>()
                .eq("level_id", levelId)
                .eq("status", 1)
                .orderByAsc("sort_order")
        );
    }

    @Override
    public List<String> getBenefitTypes() {
        return Arrays.asList(
            "折扣", "包邮", "专属客服", "生日礼", "退换货", "积分奖励", "正品保障", "专属顾问", "新品优先"
        );
    }
}



