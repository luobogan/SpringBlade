package org.springblade.workflow.odoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.springblade.workflow.odoc.entity.WfOdocRequest;

import java.util.List;
import java.util.Map;

/**
 * 公文信息(按实例) Mapper
 */
public interface WfOdocRequestMapper extends BaseMapper<WfOdocRequest> {

    /**
     * 公文统计：按公文类型 × 实例状态分组计数（对应 E9 OdocReportAction.getCountData 的基础维度）。
     *
     * <p>仅统计正式数据（is_test=0）。返回行：offical_type、status、cnt。</p>
     */
    @Select("select r.offical_type as officalType, i.status as status, count(1) as cnt "
        + "from wf_instance i join wf_odoc_request r on r.instance_id = i.id and r.is_deleted = 0 "
        + "where i.is_test = 0 group by r.offical_type, i.status")
    List<Map<String, Object>> statByOfficalType();
}
