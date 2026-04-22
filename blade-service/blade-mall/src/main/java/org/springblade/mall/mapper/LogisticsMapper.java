package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.Logistics;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物流信息Mapper
 */

public interface LogisticsMapper extends BaseMapper<Logistics> {
    /**
     * 根据订单ID查询物流信息
     */
    Logistics selectByOrderId(Long orderId);
}



