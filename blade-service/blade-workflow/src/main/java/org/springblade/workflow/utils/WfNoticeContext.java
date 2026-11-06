package org.springblade.workflow.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求级「非阻断提示」通道：service 层发现配置问题但不能中断主流程时，把中文提示暂存，
 * 由 controller 拼进 {@code R.msg} 返回给前端展示。
 *
 * <p>使用场景（对齐 E9「配置问题靠提示、不靠静默兜底/硬拦」口径）：</p>
 * <ul>
 *   <li>运行期：出口条件 UEL 变量未随提交下发，已按中性默认值兜底 → 提示检查字段配置/必填/条件引用；</li>
 *   <li>发布期：出口条件引用的字段未配置或非必填 → 非阻断 warn，同时让用户看见。</li>
 * </ul>
 *
 * <p><b>生命周期</b>：仅同一 HTTP 请求线程内有效。controller 用 {@link #join(String)} 取走并清空，
 * 且必须放在 {@code try/finally} 中调 {@link #clear()}，避免异常路径/线程复用把提示串给下一个请求。
 * 仅用于同步请求处理，不跨线程传递。</p>
 */
public final class WfNoticeContext {

    private static final ThreadLocal<List<String>> NOTICES = ThreadLocal.withInitial(ArrayList::new);

    private WfNoticeContext() {
    }

    /** 追加一条非阻断提示（去重） */
    public static void add(String msg) {
        if (msg == null || msg.isBlank()) {
            return;
        }
        List<String> list = NOTICES.get();
        if (!list.contains(msg)) {
            list.add(msg);
        }
    }

    /** 取走全部提示并清空（读后即清） */
    public static List<String> drain() {
        List<String> list = NOTICES.get();
        List<String> copy = new ArrayList<>(list);
        NOTICES.remove();
        return copy;
    }

    /** 清空（异常路径兜底，防止线程复用串提示） */
    public static void clear() {
        NOTICES.remove();
    }

    /** 拼到默认成功文案后；无提示时原样返回默认文案 */
    public static String join(String defaultMsg) {
        List<String> list = drain();
        if (list.isEmpty()) {
            return defaultMsg;
        }
        return defaultMsg + "（配置提示：" + String.join("；", list) + "）";
    }
}
