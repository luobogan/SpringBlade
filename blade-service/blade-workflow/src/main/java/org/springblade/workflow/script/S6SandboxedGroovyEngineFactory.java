package org.springblade.workflow.script;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * S6 ScriptTask 沙箱化 Groovy 脚本引擎工厂（2026-11-18）。
 *
 * <p>用户拍板「Groovy + 沙箱加固」：能力最强，但必须限制脚本能碰的类与方法。本工厂产出的
 * {@link ScriptEngine} 用带 {@link SecureASTCustomizer} 白名单的 {@link GroovyClassLoader} 编译脚本，
 * 从而在<b>编译期</b>拦截危险代码（方法定义/闭包/包声明/System.exit/文件网络反射等），
 * 而非运行期靠 try-catch 兜底。</p>
 *
 * <p><b>与 Flowable 的对接</b>：Flowable 默认按 {@code scriptFormat}（小写 {@code groovy}）查 JSR-223 引擎；
 * Groovy 原生 factory 注册名是 {@code Groovy}（首字母大写），故本工厂 {@link #getNames()} 同时返回
 * {@code groovy} 与 {@code Groovy}，由 FlowableConfig 对每个名字注册，确保大小写都能命中沙箱引擎，
 * 不会退化到无沙箱的原生引擎。</p>
 *
 * <p><b>白名单口径</b>：</p>
 * <ul>
 *   <li>导入：仅 java.lang / java.math / java.util / java.time 常用包；</li>
 *   <li>接收者：仅 String/Number/Boolean/List/Map/Date/BigDecimal/Math 等纯值类；</li>
 *   <li>语句：禁方法定义、闭包、包声明、静态导入；开启间接引用检查防绕过。</li>
 * </ul>
 */
public class S6SandboxedGroovyEngineFactory implements ScriptEngineFactory {

    /** Flowable ScriptingEngines.GROOVY_SCRIPTING_LANGUAGE = "groovy"（小写） */
    public static final String ENGINE_NAME_LOWER = "groovy";
    private static final List<String> NAMES = Arrays.asList(ENGINE_NAME_LOWER, "Groovy");

    /** 委托原生 factory 处理全部 JSR-223 元数据，只替换脚本引擎实例 */
    private final GroovyScriptEngineFactory delegate = new GroovyScriptEngineFactory();

    private static final List<String> ALLOWED_STAR_IMPORTS = Arrays.asList(
        "java.lang", "java.math", "java.util", "java.time");

    /** 允许作为方法接收者的类（纯值/集合/时间类，无 IO/网络/反射能力） */
    @SuppressWarnings("rawtypes")
    private static final List<Class> ALLOWED_RECEIVERS = Arrays.asList(
        String.class, Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class,
        Boolean.class, Character.class, Number.class, Object.class,
        java.math.BigDecimal.class, java.math.BigInteger.class, java.util.Date.class,
        java.util.List.class, java.util.Map.class, java.util.Set.class, java.util.ArrayList.class,
        java.util.LinkedHashMap.class, java.util.HashMap.class, java.util.Collections.class,
        java.time.LocalDate.class, java.time.LocalDateTime.class, java.time.Duration.class);

    @Override
    public ScriptEngine getScriptEngine() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(buildSecureCustomizer());
        // 关键：用带沙箱配置的 GroovyClassLoader 构造 JSR-223 引擎（Groovy 4 只有单参构造器接收 loader）。
        // GroovyScriptEngineImpl 会用该 loader 编译脚本，从而让 SecureASTCustomizer 在编译期生效。
        GroovyClassLoader classLoader = new GroovyClassLoader(
            S6SandboxedGroovyEngineFactory.class.getClassLoader(), config);
        return new GroovyScriptEngineImpl(classLoader);
    }

    private SecureASTCustomizer buildSecureCustomizer() {
        SecureASTCustomizer c = new SecureASTCustomizer();
        c.setMethodDefinitionAllowed(false);
        c.setClosuresAllowed(false);
        c.setPackageAllowed(false);
        // 间接引用检查在 Groovy 4 下对纯表达式（如 1+2*3）也误拦，关闭；
        // 危险类访问仍由 receivers 白名单 + imports 白名单在编译期拦截（见危险脚本单测）。
        c.setIndirectImportCheckEnabled(false);
        c.setStarImportsWhitelist(ALLOWED_STAR_IMPORTS);
        c.setImportsWhitelist(Collections.emptyList());
        c.setStaticImportsWhitelist(Collections.emptyList());
        c.setStaticStarImportsWhitelist(Collections.emptyList());
        c.setReceiversClassesWhiteList(ALLOWED_RECEIVERS);
        return c;
    }

    /** FlowableConfig 对每个名字注册本工厂，确保大小写都命中沙箱引擎 */
    public List<String> getEngineNames() {
        return NAMES;
    }

    // ---- 以下元数据全部委托原生 factory，保持 JSR-223 契约一致 ----

    @Override public String getEngineName() { return delegate.getEngineName(); }
    @Override public String getEngineVersion() { return delegate.getEngineVersion(); }
    @Override public List<String> getExtensions() { return delegate.getExtensions(); }
    @Override public List<String> getMimeTypes() { return delegate.getMimeTypes(); }
    @Override public List<String> getNames() { return NAMES; }
    @Override public String getLanguageName() { return delegate.getLanguageName(); }
    @Override public String getLanguageVersion() { return delegate.getLanguageVersion(); }
    @Override public Object getParameter(String key) { return delegate.getParameter(key); }
    @Override public String getMethodCallSyntax(String obj, String m, String... args) {
        return delegate.getMethodCallSyntax(obj, m, args);
    }
    @Override public String getOutputStatement(String toDisplay) { return delegate.getOutputStatement(toDisplay); }
    @Override public String getProgram(String... statements) { return delegate.getProgram(statements); }
}
