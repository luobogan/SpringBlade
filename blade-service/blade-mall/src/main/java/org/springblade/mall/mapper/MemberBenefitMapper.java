package org.springblade.mall.mapper;

import org.springblade.mall.entity.MemberBenefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 会员权益表 Mapper 接口
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */

public interface MemberBenefitMapper extends BaseMapper<MemberBenefit> {

    /**
     * 根据会员等级 ID 查询权益列表
     */
    List<MemberBenefit> selectByLevelId(Long levelId);
}




