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
 * 扩展任务（协办/加签协办/抄送/传阅/已读/退回发起人重提交/自由流程步骤）。
 *
 * <p><b>【去 wf_ 表 · P6 退役第一台阶 · 2026-11-09】</b>本表承载「引擎侧无原生形态」的 blade
 * 扩展任务，使 {@link WfTask} 收敛为「仅引擎任务本体台账」。列表/角标的扩展任务段改由本表读取，
 * 读源=act 时不再需要为扩展任务回读 {@code wf_task}。</p>
 *
 * <p><b>与 {@code wf_task} 的关系</b>：{@code wf_task} 仍保留引擎任务本体的写入（
 * {@code WfTaskActWriter} 把 status/is_test/sign_order/view_time/due_time 等业务列投影到
 * {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 自定义列，且 {@code BIZ_TASK_ID_} 是列表 id 与
 * 操作链的契约），故 A 类台账不可停写；本表只接手 B/C 类扩展语义。</p>
 *
 * <p><b>type 与 status 正交</b>：{@code type} 是不可变的语义类别（见常量），{@code status} 是
 * 该行自身状态（0 待处理/未读、1 已处理/已读、2 已拒绝）。列表 VO 通过
 * {@link #toWfTaskStatus()} 映射回原 {@code WfTask.status} 语义，保证前端标签零改动。</p>
 *
 * <p><b>engine_task_id</b>：沿用宿主引擎任务ID；<b>空串</b>（非 NULL）表示无引擎任务的
 * 合成待办（退回发起人重提交），与 {@code wf_task} 既有口径一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_ext_task")
@Schema(description = "扩展任务（协办/加签协办/抄送/传阅/已读/退回重提交/自由流程步骤）")
public class WfExtTask extends TenantEntity {

    /** 协办 / 转办（同节点多人，沿用宿主 engineTaskId） */
    public static final int TYPE_COADJUTANT = 1;
    /** 加签协办 */
    public static final int TYPE_ADD_SIGN = 2;
    /** 抄送 */
    public static final int TYPE_CIRCULATE = 3;
    /** 传阅 */
    public static final int TYPE_READ = 4;
    /** 已读（抄送/传阅的已读态） */
    public static final int TYPE_READED = 5;
    /** 退回发起人重提交（无引擎任务的合成待办） */
    public static final int TYPE_RESUBMIT = 6;
    /** 自由流程步骤 */
    public static final int TYPE_FREE_FLOW = 7;

    /** 待处理 / 未读 */
    public static final int STATUS_PENDING = 0;
    /** 已处理 / 已读 */
    public static final int STATUS_DONE = 1;
    /** 已拒绝 */
    public static final int STATUS_REJECT = 2;

    /** 待办段 type（可办理扩展待办；必须为 status=0 才进待办） */
    public static final String TODO_TYPE_SQL = "2,6";
    /** 已办段 type（协办/抄送/传阅/已读，无论是否已读都进已办侧，与原 status=7/8/11 行为一致） */
    public static final String DONE_ONLY_TYPE_SQL = "1,3,4,5";
    /** 自由流程步骤名装饰行 type（非列表条目，任何判据都须排除） */
    public static final int TYPE_DECORATION = 7;

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "引擎任务ID（Flowable）；空串=无引擎任务的合成待办")
    private String engineTaskId;

    @Schema(description = "宿主业务任务ID（wf_task.id / ACT BIZ_TASK_ID_）；type=7 自由流程步骤名装饰行据此回查")
    private Long bizTaskId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "扩展任务类型：1协办/转办 2加签协办 3抄送 4传阅 5已读 6退回重提交 7自由流程步骤")
    private Integer type;

    @Schema(description = "接收人/办理人")
    private Long assignee;

    @Schema(description = "原处理人（转办/协办回溯）")
    private Long originalUser;

    @Schema(description = "会签关系")
    private Integer signOrder;

    @Schema(description = "任务标题（冗余，避免列表回查实例）")
    private String title;

    @Schema(description = "意见/备注")
    private String opinion;

    @Schema(description = "扩展任务自身状态：0待处理/未读 1已处理/已读 2已拒绝")
    private Integer status;

    @Schema(description = "任务级扩展上下文JSON（自由流程步骤显示名/步序；NULL=普通扩展任务）")
    private String extJson;

    @Schema(description = "测试态标记 1=测试产生的任务")
    private Integer isTest;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "接收时间")
    private Date receiveTime;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "处理时间")
    private Date operateTime;

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

    /**
     * 映射回原 {@code WfTask.status} 语义，保证列表 VO 与前端标签零改动。
     *
     * <p>规则（对齐 §7.10 的 6 态定案，保证前端标签零改动）：</p>
     * <ul>
     *   <li>协办(1) → 7、抄送(3) → 8、传阅(4)/已读(5) → 11：与原 {@code wf_task} 的
     *       status=7/8/11 <b>完全等价</b>，落在「已办」段（{@code done()} 的 statuses 含 7/8/11）；</li>
     *   <li>加签协办(2)/退回重提交(6) → 待处理时输出 0（待办）、处理后输出 2（已办）；</li>
     *   <li>自由流程(7) 为装饰行，不进列表，映射结果不参与展示。</li>
     * </ul>
     */
    public int toWfTaskStatus() {
        int t = type == null ? 0 : type;
        int s = status == null ? STATUS_PENDING : status;
        switch (t) {
            case TYPE_COADJUTANT:
                return WfTask.STATUS_COADJUTANT;
            case TYPE_CIRCULATE:
                return WfTask.STATUS_CIRCULATE;
            case TYPE_READ:
            case TYPE_READED:
                return WfTask.STATUS_READ;
            case TYPE_ADD_SIGN:
            case TYPE_RESUBMIT:
                if (s == STATUS_REJECT) {
                    return WfTask.STATUS_FINISHED;
                }
                return s == STATUS_PENDING ? WfTask.STATUS_TODO : WfTask.STATUS_DONE;
            case TYPE_FREE_FLOW:
            default:
                return WfTask.STATUS_TODO;
        }
    }

    /**
     * 该行是否属于「待办段」扩展任务（决策 3 判据）。
     *
     * <p>注意排除 {@link #TYPE_FREE_FLOW}：该类行仅承载自由流程步骤显示名（装饰行），
     * 不是列表条目，若计入会多算行。装饰行由 {@code decorateFreeFlowNames} 按 id 单独读取。</p>
     */
    public static boolean isTodoType(int type) {
        return type == TYPE_ADD_SIGN || type == TYPE_RESUBMIT;
    }

    /** 该行是否属于「已办段」扩展任务（协办/抄送/传阅/已读，与原 status=7/8/11 行为一致）。 */
    public static boolean isDoneType(int type) {
        return type == TYPE_COADJUTANT || type == TYPE_CIRCULATE
            || type == TYPE_READ || type == TYPE_READED;
    }
}
