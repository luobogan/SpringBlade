package org.springblade.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.workflow.entity.WfCustomAction;

/**
 * 自定义接口动作 Mapper（流程模板的一部分，脱离租户隔离）。
 *
 * <p>⚠️ 必须 {@code @InterceptorIgnore(tenantLine = "true")}：DataScope 拦截器会对
 * 方法名含 list/page 的调用套数据权限，而库里没有对应配置 → DataScopeCache 对 null 调
 * getResourceCode() 抛 NPE（与 WfNodeOperatorMapper 同一坑）。</p>
 */
@InterceptorIgnore(tenantLine = "true")
public interface WfCustomActionMapper extends BaseMapper<WfCustomAction> {

    /**
     * 物理删除（不用 @TableLogic 逻辑删除）。
     *
     * <p>⚠️ 唯一索引 {@code uk_action_key(action_key)} 不含 is_deleted：逻辑删除的行仍占着标识，
     * 之后重新注册同一「接口动作标识」会撞唯一键（与 wf_process_node 同款坑）。</p>
     */
    @Delete("DELETE FROM wf_custom_action WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
