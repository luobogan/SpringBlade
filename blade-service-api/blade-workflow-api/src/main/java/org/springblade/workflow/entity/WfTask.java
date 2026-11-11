package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 流程任务实体（待办 / 已办）
 *
 * <p>对齐 ecology {@code workflow_currentoperator}。</p>
 * <p>status 对齐 ecology {@code isremark}：0待办 2已办 4办结 6自动提交
 * 7协办 8抄送 11传阅。</p>
 * <p>engine_task_id 为 Flowable 任务ID（弱关联）；original_user 用于代理人代办场景。</p>
 *
 * <p><b>【历史 · 2026-11-08 决策：表退役暂缓】</b>当时判定本表不删、不停写，
 * 因三类 ACT 无法表达的业务语义需由本表承载（blade 扩展任务、无引擎任务的合成待办、
 * blade 6 态语义）；重启退役条件见 §7.10。</p>
 *
 * <p><b>【历史 · 2026-11-09 进展】</b>blade 扩展任务已一次性迁至 {@link WfExtTask}
 * （表 {@code wf_ext_task}），本表职责收敛为「仅引擎任务本体台账」。</p>
 *
 * <p><b>【2026-11-10 终态 · 已废弃】</b>批次 1 完成：
 * ① 任务 id 由 {@code IdWorker.getId()} 预生成，不再靠本表 insert 取得；
 * ② 业务列经 {@code WfTaskActDirectWriter} 直写 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST}；
 * ③ 操作链按 {@code BIZ_TASK_ID_} 经 {@code WfTaskBizResolver} 反查 ACT。
 * 故本表<b>运行时零写入</b>，仅保留存量行供追溯与 {@code task-ledger-mode=legacy} 回退。</p>
 *
 * @deprecated <b>2026-11-10 已废弃</b>：表与实体<b>不物理删除</b>（用户决策：仅打废弃标记），
 *     仅作历史追溯 / 回退载体。新代码不得新增对本表的写入；任务运行态以 ACT 引擎表为唯一事实源，
 *     扩展任务请用 {@link WfExtTask}。后续 P6 终态再考虑物理删除（须先备份）。
 */
@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_task")
@Schema(description = "流程任务（待办/已办）")
public class WfTask extends TenantEntity {

    /** 待办 */
    public static final int STATUS_TODO = 0;
    /** 已办 */
    public static final int STATUS_DONE = 2;
    /** 办结 */
    public static final int STATUS_FINISHED = 4;
    /** 自动提交 */
    public static final int STATUS_AUTO_SUBMIT = 6;
    /** 协办 */
    public static final int STATUS_COADJUTANT = 7;
    /** 抄送 */
    public static final int STATUS_CIRCULATE = 8;
    /** 传阅 */
    public static final int STATUS_READ = 11;

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "引擎任务ID（Flowable）")
    private String engineTaskId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "办理人")
    private Long assignee;

    @Schema(description = "代理人代办时的原处理人")
    private Long originalUser;

    @Schema(description = "会签关系")
    private Integer signOrder;

    @Schema(description = "测试态标记 1=测试产生的待办（可一键清理）")
    private Integer isTest;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "接收时间")
    private Date receiveTime;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "处理时间")
    private Date operateTime;

    /**
     * 首次查看时间（流程轨迹「已查看」判定）。
     *
     * <p>办理人打开待办/办理页时写入（只记首次，不覆盖）。流程轨迹按
     * 待办(0)+viewTime 非空 = 已查看、待办(0)+为空 = 未操作。</p>
     */
    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "首次查看时间")
    private Date viewTime;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "截止时间")
    private Date dueTime;

    @Schema(description = "超时动作是否已执行（防重复触发）1=已执行 0=未执行")
    private Integer timeoutHandled;

    @Schema(description = "任务级扩展上下文JSON（自由流程步骤显示名/步序；NULL=普通任务）")
    private String extJson;

}
