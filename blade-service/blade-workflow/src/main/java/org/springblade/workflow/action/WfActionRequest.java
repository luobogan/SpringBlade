package org.springblade.workflow.action;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义接口动作的执行上下文（对齐 ecology E9 {@code RequestInfo}）。
 *
 * <p>由 {@link WfActionExecutor} 组装后传给 {@link IWfCustomAction#execute(WfActionRequest)}。</p>
 */
@Data
public class WfActionRequest {

    /** 流程实例 ID */
    private Long instId;

    /** 流程定义 ID */
    private Long defId;

    /** 当前节点 Key */
    private String nodeKey;

    /** 阶段：pre（节点前附加操作）/ post（节点后附加操作） */
    private String phase;

    /** 操作者用户 ID（多数场景为空，如节点激活时的前附加操作） */
    private Long operator;

    /** 注册记录里的接口动作标识 */
    private String actionKey;

    /** 接口动作名称 */
    private String actionName;

    /**
     * 参数（参数名称 → 参数值）。
     * 取自注册记录的「参数设置」，可被节点上的覆盖值覆盖；
     * 「是否数据源=是」的参数，其值在运行时从 {@link #formData} 按字段名取。
     */
    private Map<String, Object> params = new LinkedHashMap<>();

    /** 当前实例的表单数据快照（可能为空 map） */
    private Map<String, Object> formData = new LinkedHashMap<>();
}
