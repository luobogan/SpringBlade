package org.springblade.workflow.action;

/**
 * 自定义接口动作（对齐 ecology E9 {@code weaver.interfaces.workflow.action}）。
 *
 * <p>在「注册自定义接口」里登记的 {@code 接口动作类文件} 必须是**类全名**，
 * 且该类必须实现本接口；节点前后附加操作选用该动作后，流转时由
 * {@link WfActionExecutor} 反射实例化并调用 {@link #execute(WfActionRequest)}。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>类需提供**无参构造**（每次执行新建实例，请勿在字段里存业务状态）；</li>
 *   <li>返回值为执行结果描述，仅用于写入日志；</li>
 *   <li>抛异常表示执行失败，由节点「流程异常处理」（continue / stop）决定跳过还是中断流转。</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>{@code
 * public class DemoAction implements IWfCustomAction {
 *     @Override
 *     public String execute(WfActionRequest request) {
 *         String amount = String.valueOf(request.getFormData().get("amount"));
 *         return "done: " + amount + ", param=" + request.getParams().get("p1");
 *     }
 * }
 * }</pre>
 */
public interface IWfCustomAction {

    /**
     * 执行动作。
     *
     * @param request 流程上下文（实例/节点/阶段/操作者/参数/表单数据）
     * @return 执行结果描述（写日志）
     * @throws Exception 执行失败
     */
    String execute(WfActionRequest request) throws Exception;
}
