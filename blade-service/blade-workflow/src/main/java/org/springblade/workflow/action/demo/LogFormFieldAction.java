package org.springblade.workflow.action.demo;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.action.IWfCustomAction;
import org.springblade.workflow.action.WfActionRequest;
import org.springframework.stereotype.Component;

/**
 * 示例自定义接口动作：把表单里指定字段的值打进日志（可直接用于验证「注册自定义接口」链路）。
 *
 * <p><b>用法</b>：在「注册自定义接口」里填</p>
 * <ul>
 *   <li>接口动作名称：示例-记录表单字段</li>
 *   <li>接口动作标识：demo_log_field（唯一，供节点附加操作引用）</li>
 *   <li>接口动作类文件：{@code org.springblade.workflow.action.demo.LogFormFieldAction}</li>
 *   <li>参数设置：参数名称 {@code fieldName}，参数值 {@code amount}，是否数据源 = 是
 *       （「是」表示参数值按**表单字段名**在运行时取值）</li>
 * </ul>
 *
 * <p>然后在节点「前/后附加操作 → 外部接口 → 自定义接口动作」里选它即可；
 * 流转到该节点时会打印 {@code [示例自定义接口动作] ...} 日志。</p>
 *
 * <p>该类标了 {@code @Component}，执行器会优先从 Spring 容器取实例（因此可以正常注入依赖）；
 * 若不给注解，执行器会退回按无参构造新建。</p>
 */
@Slf4j
@Component
public class LogFormFieldAction implements IWfCustomAction {

    @Override
    public String execute(WfActionRequest request) {
        Object fieldName = request.getParams().get("fieldName");
        Object value = fieldName == null ? null : request.getFormData().get(String.valueOf(fieldName));
        log.info("[示例自定义接口动作] actionKey={}, nodeKey={}, phase={}, field={}, value={}, params={}",
            request.getActionKey(), request.getNodeKey(), request.getPhase(),
            fieldName, value, request.getParams());
        return "ok: " + fieldName + "=" + value;
    }
}
