package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 自定义接口动作注册表（对齐 ecology E9「注册自定义接口」）。
 *
 * <p>E9 里「节点前后附加操作 → 外部接口 → 自定义接口动作」选用的是一条**注册记录**：
 * 接口动作名称 + 接口动作标识（唯一）+ 接口动作类文件（类全名，须实现指定接口），
 * 另有「参数设置」（参数名称 / 参数值 / 是否数据源）。本实体即该注册记录。</p>
 *
 * <p>执行：{@code WfActionExecutor} 遇到 {@code action:&lt;标识&gt;} 形式的命令时，
 * 按 {@link #className} 反射实例化并调用 {@code IWfCustomAction#execute(WfActionRequest)}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_custom_action")
@Schema(description = "自定义接口动作（注册自定义接口）")
public class WfCustomAction extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "接口动作名称")
    private String actionName;

    @Schema(description = "接口动作标识（唯一，节点附加操作按此引用）")
    private String actionKey;

    @Schema(description = "接口动作类文件：类全名，须实现 org.springblade.workflow.action.IWfCustomAction")
    private String className;

    @Schema(description = "参数设置（JSON 数组：[{name,value,isDataSource}]）")
    private String paramsJson;

    @Schema(description = "备注")
    private String remark;
}
