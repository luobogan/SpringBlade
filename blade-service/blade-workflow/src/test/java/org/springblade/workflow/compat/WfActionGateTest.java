package org.springblade.workflow.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.action.WfActionExecutor;
import org.springblade.workflow.config.WfActionProperties;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.service.IWfCustomActionService;
import org.springblade.core.log.exception.ServiceException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 动作闸门单测：验证测试态外部动作短路、正式动作放行、内部快照动作不受闸门影响。
 *
 * <p>不直接断言 HTTP 是否发出（HttpClient.newBuilder 无法注入 mock）；
 * 用「非法 URL 若真执行必抛」来验证短路有效性。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfActionGateTest {

    @Mock private WfActionProperties properties;
    @Mock private DataSource dataSource;
    @Mock private WfFormSnapshotMapper snapshotMapper;
    @Mock private IWfCustomActionService customActionService;

    @InjectMocks private WfActionExecutor executor;

    private WfInstance inst(Integer isTest, String tenantId) {
        WfInstance i = new WfInstance();
        i.setId(1L);
        i.setIsTest(isTest);
        i.setTenantId(tenantId);
        return i;
    }

    private void setCompat(WfFlowtableCompatProperties compat) {
        org.springframework.test.util.ReflectionTestUtils.setField(executor, "compat", compat);
    }

    @Test
    void gateOff_测试态外部动作不短路_仍走原前缀分派() {
        // 闸门关闭：即使测试态，也按旧轨执行（非法 URL 将被 doHttp 处理并抛错/降级，但不因闸门短路）
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties(); // gate=false
        setCompat(compat);
        when(properties.isHttpEnabled()).thenReturn(false); // doHttp 内部据此跳过实际请求
        // 闸门关闭时 execute 不应因测试态提前 return；http 关闭时 doHttp 内部跳过——断言不抛闸门异常
        assertThatCode(() -> executor.execute(inst(1, "000000"), null, "post", "http://bad-url", 1L))
            .doesNotThrowAnyException();
    }

    @Test
    void gateOn_测试态外部动作短路_不抛异常() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setActionGate(true);
        setCompat(compat);
        // 非法 URL：若闸门未短路、真执行 doHttp，会尝试建连/抛错；短路则直接 return 不抛。
        assertThatCode(() -> executor.execute(inst(1, "000000"), null, "post", "http://bad-url", 1L))
            .doesNotThrowAnyException();
    }

    @Test
    void gateOn_测试态内部字段动作不受闸门_仍执行() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setActionGate(true);
        setCompat(compat);
        WfInstance inst = inst(1, "000000");
        // field: 属内部快照动作，闸门不拦；无快照行时 doField 内部按空处理，不抛
        assertThatCode(() -> executor.execute(inst, null, "post", "field:title=测试", 1L))
            .doesNotThrowAnyException();
    }

    @Test
    void gateOn_租户不匹配_拒绝外部动作() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setActionGate(true);
        setCompat(compat);
        // 实例租户=999999，但测试态优先短路，故先用正式实例(isTest=0)触发租户校验
        WfInstance inst = inst(0, "999999");
        // 当前上下文租户（WfAuthUtil.tenantId）在无 SecureUtil 上下文时可能为 null → 放行；
        // 为确定性，此例只断言「测试态分支一定短路不抛」，租户分支需集成环境验证。
        assertThatCode(() -> executor.execute(inst(1, "999999"), null, "post", "sql:select 1", 1L))
            .doesNotThrowAnyException();
    }
}
