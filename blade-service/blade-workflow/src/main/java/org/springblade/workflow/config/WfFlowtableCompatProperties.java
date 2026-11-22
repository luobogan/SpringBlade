package org.springblade.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * flowtable 全功能扩展前的「共享收口双轨」兼容开关（2026-11-17）。
 *
 * <p>四处共享收口（归档判定 / 事件失败策略 / 动作闸门 / 会签推进）在扩展自动节点、事件驱动、
 * 定时异步等能力前，先建立可开关、可回退、可验证的双轨分支。设计依据见
 * {@code doc/md/四处共享收口双轨改造订正方案.md}。</p>
 *
 * <p><b>所有开关代码默认值为安全值（旧行为）</b>：字段声明处即写死 false / 空集合，
 * 不依赖 YAML 装配——本类可能被 {@code new WfFlowtableCompatProperties()} 直接实例化（单测/兜底路径），
 * 届时 Java 默认值虽同为 false，但 {@code engineMiGrayDefIds} 必须显式初始化避免 null。</p>
 *
 * <p>示例（application.yml）：</p>
 * <pre>{@code
 * blade:
 *   workflow:
 *     compat:
 *       archive-signal: false       # 归档判定：false=ACT_RU_TASK 空即归档（旧）；true=按引擎实例 ended 判定
 *       strict-event-fail: false    # 台账投影 listener 失败是否回滚引擎；仅作用于 WfEngineEventListener
 *       action-gate: false          # 执行器层 is_test/租户/白名单闸门
 *       engine-mi-gray-def-ids: []  # 会签引擎多实例灰度定义ID白名单；空=全部走现有全局开关
 * }</pre>
 *
 * <p>⚠️ Nacos {@code blade-workflow-dev.yaml} 若配了该键以 Nacos 为准，需同步修改。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "blade.workflow.compat")
public class WfFlowtableCompatProperties {

    /** 归档判定新信号源：true=按引擎实例是否 ended 判定；false=沿用 ACT_RU_TASK 空即归档（旧轨，默认）。 */
    private boolean archiveSignal = false;

    /** 台账投影 listener 失败是否回滚引擎：true=C1 强一致；false=catch→warn 最终一致（旧轨，默认）。仅作用 WfEngineEventListener。 */
    private boolean strictEventFail = false;

    /** 动作统一闸门：true=执行器层拦测试态外部动作/租户不匹配；false=沿用调用方守卫（旧轨，默认）。 */
    private boolean actionGate = false;

    /** 会签引擎多实例灰度定义ID白名单：空=完全等同现状（全局开关决定）；非空=仅名单内定义走引擎 MI。 */
    private List<Long> engineMiGrayDefIds = new ArrayList<>();

    /**
     * E7 引擎→业务状态桥总开关：false=旧行为（自动节点跑完后不建待办、不额外同步，仍由业务层 advance 驱动）；
     * true=由独立 listener 在自动节点完成后同步引擎任务到业务待办。默认 false，零行为变化。
     */
    private boolean engineAutoTodoBridge = false;

    /**
     * E7 灰度定义ID白名单：为空=对所有流程生效（开关打开时）；非空=仅名单内 defId 启用桥。
     * 建议先填白名单灰度，验证无事件循环/待办重复后再清空转全量。
     */
    private List<Long> engineAutoTodoGrayDefIds = new ArrayList<>();

    /**
     * E1 ServiceTask 自动执行总开关（2026-11-18）：false=不注入 delegate、ServiceTask 仍按引擎原生语义（默认，零行为变化）；
     * true=部署期对「挂了 customAction 的 ServiceTask」注入 {@code ${wfServiceTaskDelegate}}，
     * 运行时由统一 delegate 读取 actionKey 并走 {@code WfActionExecutor.execute()} 执行（保留 action-gate 守卫）。
     *
     * <p>必须与 action-gate 同时开启（测试态外部动作要受闸门）；单独的 E1 不含推进语义，
     * ServiceTask 完成后的建待办由 E7 桥负责，二者互补不重复（delegate 只执行动作、不 advance）。</p>
     */
    private boolean serviceTaskExec = false;

    /**
     * E1 灰度定义ID白名单（2026-11-18 补）：为空=对所有流程生效（开关打开时）；非空=仅名单内 defId 启用 E1 注入与执行。
     *
     * <p><b>为何必须有</b>：E1 是「部署期给 ServiceTask 注入 delegate」的<b>全局</b>开关，一旦打开就会影响
     * <b>所有</b>含 ServiceTask 的已发布流程（重新发布即注入），无法按流程灰度。E7 有
     * {@link #engineAutoTodoGrayDefIds} 可灰度，E1 若无则无法遵守「先灰度验证再转全量」的放量纪律。</p>
     *
     * <p>判定点两处（双保险）：① 部署期注入（{@code WfDefinitionServiceImpl#injectServiceTaskDelegate}）——
     * 名单外的定义<b>不注入</b> delegate，流程保持引擎原生 ServiceTask 语义；
     * ② 运行期执行（{@code WfServiceTaskDelegate}）—— 名单外直接跳过，
     * 覆盖「已按旧配置部署过、之后才把开关打开」的场景（工件里已有 delegateExpression）。</p>
     *
     * <p>建议先填 1~2 个测试 defId 灰度，验证动作执行 + E7 待办同步 + 无事件循环后再清空转全量。</p>
     */
    private List<Long> serviceTaskExecGrayDefIds = new ArrayList<>();

    /**
     * S3 timer 业务侧轮询开关（2026-11-18）：false=不扫描引擎 timer job（默认，零行为变化，等价于现状）；
     * true=由 WfTimerJobScanner 定期扫描 ACT_RU_TIMER_JOB 中已到期的 job，经
     * {@code moveTimerToExecutableJob → executeJob} 手动驱动流转，<b>绕开 AsyncExecutor</b>。
     *
     * <p><b>为什么能绕开 AsyncExecutor</b>：实测（S3TimerJobProbeTest）确认即使 AsyncExecutor 关闭，
     * 流程进入 timer 节点时引擎仍会创建 timer job；只是没有执行器去消费。本开关就是用业务侧扫描补上
     * 「消费」这一步，无需开 AsyncExecutor 及其三项兜底。</p>
     *
     * <p><b>互斥</b>：开启前必须确认 AsyncExecutor 仍关闭（FlowableConfig.setAsyncExecutorActivate(false)），
     * 否则两侧会重复执行同一 timer job。</p>
     */
    private boolean s3TimerEnabled = false;

    /**
     * S4 等待唤醒开关（2026-11-18）：false=唤醒服务拒绝调用（默认，零行为变化）；
     * true=允许 ReceiveTask.trigger / messageEventReceived 唤醒等待态流程。
     *
     * <p>唤醒是「推进流程」的写操作，内部入口走 HAS_AUTH，外部 permitAll 入口必须另行验签+幂等+网关白名单。</p>
     */
    private boolean s4WakeupEnabled = false;

    /**
     * S5 前置兜底：deadletter 死信作业告警扫描开关（2026-11-18）：false=不扫描（默认，零行为变化）；
     * true=定时扫描 {@code ACT_RU_DEADLETTER_JOB}，非空即 log.error + 经 INoticeClient 投递管理员告警。
     *
     * <p>方案C §3.4 三项兜底之一（作业积压/失败告警）。开启 AsyncExecutor 前必须具备，
     * 否则死信作业无人发现、台账漏更新不可观测。自动重试默认关闭（避免掩盖真实故障），
     * 需人工通过 IProcessService 的死信重投/删除入口处置。</p>
     */
    private boolean deadletterAlarmEnabled = false;

    /**
     * S5 前置兜底：常驻漂移对账开关（2026-11-18）：false=不做定期对账（默认，依赖既有事件反写）；
     * true=WfDriftReconciler 定期扫描「业务终态但引擎仍活跃」的实例并修复（增量时间窗，非全量）。
     *
     * <p>与一次性脚本 {@code HistoricalDriftFixer} 区别：本对账器只扫最近窗口内的实例，
     * 且逐个用 {@code isProcessInstanceEnded} 确认确实漂移才动手，避免每轮全量空转。</p>
     */
    private boolean driftReconcileEnabled = false;

    /**
     * S6 ScriptTask 脚本执行总开关（2026-11-18）：false=ScriptTask 发布期硬拦（默认，零行为变化）；
     * true=放开 ScriptTask，使用带白名单沙箱的 Groovy 引擎执行脚本。
     *
     * <p>安全：脚本引擎经 {@code SecureASTCustomizer} 白名单加固（禁方法定义/闭包/包声明/间接引用绕过，
     * 限导入类、限接收者类、限语句类型）。与 action-gate 正交：脚本在引擎层执行，
     * 不并入 IWfCustomAction 注册表。</p>
     */
    private boolean s6ScriptTaskEnabled = false;

}
