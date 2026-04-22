package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.mall.entity.MemberBenefit;
import org.springblade.mall.entity.MemberLevel;
import org.springblade.mall.mapper.MemberLevelMapper;
import org.springblade.mall.service.MemberBenefitService;
import org.springblade.mall.service.MemberLevelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.mall.vo.MemberLevelVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 会员等级服务实现类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements MemberLevelService {

    @Autowired
    private MemberBenefitService memberBenefitService;

    @Override
    public List<MemberLevelVO> getAllActiveLevels() {
        List<MemberLevel> levels = this.list(
            new QueryWrapper<MemberLevel>()
                .eq("status", 1)
                .orderByAsc("level_value")
        );

        return levels.stream().map(level -> {
            MemberLevelVO vo = new MemberLevelVO();
            BeanUtils.copyProperties(level, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public MemberLevel calculateLevelByGrowth(Integer growth) {
        if (growth == null) {
            growth = 0;
        }

        List<MemberLevel> levels = this.list(
            new QueryWrapper<MemberLevel>()
                .eq("status", 1)
                .le("min_growth", growth)
                .orderByDesc("level_value")
        );

        return levels.isEmpty() ? null : levels.get(0);
    }

    @Override
    public MemberLevel calculateLevelByExperience(Integer experience) {
        if (experience == null) {
            experience = 0;
        }

        List<MemberLevel> levels = this.list(
            new QueryWrapper<MemberLevel>()
                .eq("status", 1)
                .le("min_experience", experience)
                .orderByDesc("level_value")
        );

        return levels.isEmpty() ? null : levels.get(0);
    }

    @Override
    public MemberLevelVO getLevelDetail(Long levelId) {
        MemberLevel level = this.getById(levelId);
        if (level == null) {
            return null;
        }

        MemberLevelVO vo = new MemberLevelVO();
        BeanUtils.copyProperties(level, vo);
        
        // 查询权益列表
        List<MemberBenefit> benefits = memberBenefitService.getByLevelId(levelId);
        vo.setBenefitList(benefits.stream()
            .map(MemberBenefit::getName)
            .collect(Collectors.toList()));
        
        return vo;
    }
}



