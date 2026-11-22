package org.springblade.workflow.script;

import org.junit.jupiter.api.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S6 Groovy 沙箱单测（2026-11-18）：验证合法脚本可执行、危险脚本在编译期被白名单拦截。
 */
class S6SandboxedGroovyEngineTest {

    private final S6SandboxedGroovyEngineFactory factory = new S6SandboxedGroovyEngineFactory();

    @Test
    void 合法脚本_四则运算与字符串_可执行() throws Exception {
        ScriptEngine engine = factory.getScriptEngine();
        assertThat(engine.eval("1 + 2 * 3")).isEqualTo(7);
        assertThat(engine.eval("'hello' + ' world'")).isEqualTo("hello world");
        assertThat(engine.eval("new java.math.BigDecimal('1.5').add(new java.math.BigDecimal('2.5'))").toString())
            .isEqualTo("4.0");
    }

    @Test
    void 变量绑定_脚本可读流程变量() throws Exception {
        ScriptEngine engine = factory.getScriptEngine();
        engine.put("amount", 100);
        assertThat(engine.eval("amount * 2")).isEqualTo(200);
    }

    @Test
    void 危险脚本_SystemExit_被沙箱拦截() {
        assertThatThrownBy(() -> factory.getScriptEngine().eval("System.exit(1)"))
            .isInstanceOf(ScriptException.class);
    }

    @Test
    void 危险脚本_方法定义_被沙箱拦截() {
        assertThatThrownBy(() -> factory.getScriptEngine().eval("def evil() { 'x' }; evil()"))
            .isInstanceOf(ScriptException.class);
    }

    @Test
    void 危险脚本_闭包_被沙箱拦截() {
        assertThatThrownBy(() -> factory.getScriptEngine().eval("def c = { -> 'x' }; c()"))
            .isInstanceOf(ScriptException.class);
    }

    @Test
    void 危险脚本_文件IO_被沙箱拦截() {
        assertThatThrownBy(() -> factory.getScriptEngine().eval(
            "new java.io.File('/etc/passwd').text"))
            .isInstanceOf(ScriptException.class);
    }

    @Test
    void 注册到Manager后按名取引擎走沙箱() {
        // 模拟 FlowableConfig 的注册方式：对 manager 注册沙箱工厂后，按 "groovy"/"Groovy" 取到的引擎
        // 必须是沙箱的（危险脚本被拦），不能是 SPI 默认加载的无沙箱原生引擎。
        ScriptEngineManager manager = new ScriptEngineManager();
        for (String name : factory.getEngineNames()) {
            manager.registerEngineName(name, factory);
            ScriptEngine engine = manager.getEngineByName(name);
            assertThat(engine).isNotNull();
            assertThatThrownBy(() -> engine.eval("System.exit(1)"))
                .as("engine for name=%s must be sandboxed", name)
                .isInstanceOf(ScriptException.class);
        }
    }

    @Test
    void 引擎名覆盖大小写两种scriptFormat写法() {
        // Flowable 按 scriptFormat="groovy"（小写）查引擎；Groovy 原生工厂名是 "Groovy"。
        // 本工厂须同时提供两个名字，FlowableConfig 对两者都注册沙箱工厂。
        assertThat(factory.getEngineNames()).contains("groovy", "Groovy");
        // 无论用哪个名字拿到引擎，沙箱都生效（危险脚本被拦）
        for (String name : factory.getEngineNames()) {
            ScriptEngine engine = factory.getScriptEngine();
            assertThat(engine).isNotNull();
            assertThatThrownBy(() -> engine.eval("System.exit(1)"))
                .isInstanceOf(ScriptException.class);
        }
    }
}
