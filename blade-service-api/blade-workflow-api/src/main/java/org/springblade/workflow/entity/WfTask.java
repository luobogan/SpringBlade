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
 * <p><b>【表退役暂缓 · 2026-11-08 决策】</b>本表<b>不删</b>、也<b>不停写</b>。
 * 原 P6 计划是「退役 wf_task → 删 Entity/Mapper/Service → DROP 表」，现<b>暂缓</b>，
 * 原因是「去 wf_ 表」全量盘点（2026-11-08）确认了三类 ACT 无法表达、必须由本表承载的业务语义：</p>
 * <ol>
 *   <li><b>blade 扩展任务</b>：协办(7)/抄送(8)/传阅(11) 是 blade 业务产物，引擎无原生物；
 *       且历史抄送以本表行为权威存储（{@code ACT_RU_IDENTITYLINK} 任务完成后会被清理）。</li>
 *   <li><b>无引擎任务的合成待办</b>：草稿待办、退回发起人重提交（{@code engine_task_id} 为空）。</li>
 *   <li><b>blade 6 态语义</b>：{@code BUSINESS_STATUS_} 只是本表 {@code status} 的投影，
 *       引擎先删 {@code ACT_RU_TASK}、事件再反写 sync，中间存在漂移窗口。</li>
 * </ol>
 * <p><b>重启退役的条件</b>：① blade 扩展任务迁到独立表或 ACT 新增承载列；
 * ② 合成待办有替代载体；③ blade 6 态的权威存储定案（见
 * {@code doc/md/去wf_表-任务读源翻源上线检查.md} §7.10）。</p>
 * <p><b>注意</b>：{@code WfTaskActWriter} 的 N:1 守卫每次双写都要
 * {@code SELECT COUNT(*) FROM wf_task WHERE engine_task_id = ?} → 本表在被替换前<b>不能物理删除</b>。</p>
 */
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
