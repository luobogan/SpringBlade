package org.springblade.workflow.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 出口条件（UEL）变量名提取：运行期兜底与发布期配置检查<b>共用同一口径</b>。
 *
 * <p>条件是形如 {@code ${eeramt <= 3000 && payamt > 0}} 的 UEL 字符串，变量名是里面的标识符；
 * 字符串字面量（{@code 'APPROVED'}）与 EL 关键字/引擎内置对象要剔除，避免误判成表单字段。</p>
 */
public final class WfConditionVarUtil {

    private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** EL/UEL 关键字与引擎内置对象：不是表单字段 */
    private static final Set<String> RESERVED = Set.of(
        "true", "false", "null", "empty", "and", "or", "not",
        "eq", "ne", "gt", "ge", "lt", "le", "div", "mod", "instanceof",
        "execution", "task", "variable", "variables", "authenticatedUserId",
        "getVariable", "get", "contains", "startsWith", "endsWith", "matches",
        "length", "size", "String", "Math", "Double", "Integer", "Long", "Boolean"
    );

    private WfConditionVarUtil() {
    }

    /** 提取一个条件表达式里的全部变量名（去重、保序） */
    public static Set<String> extract(String expr) {
        Set<String> names = new LinkedHashSet<>();
        if (expr == null || expr.isBlank()) {
            return names;
        }
        // 去掉字符串字面量，避免把 'APPROVED' 之类误判成变量
        String cleaned = expr.replaceAll("'[^']*'", " ").replaceAll("\"[^\"]*\"", " ");
        Matcher m = IDENT.matcher(cleaned);
        while (m.find()) {
            String n = m.group();
            if (!RESERVED.contains(n)) {
                names.add(n);
            }
        }
        return names;
    }
}
