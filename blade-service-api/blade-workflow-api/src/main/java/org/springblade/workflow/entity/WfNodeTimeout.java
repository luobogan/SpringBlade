package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 节点超时规则（多条，对齐泛微节点信息「超时设置」）。
 *
 * <p>一条流程节点可配置多条规则，按 {@code seq} 升序执行；规则命中（已到期）时执行
 * {@code actionWay}（自动通过 / 流转 / 指定操作者 / 提醒），并按 {@code remindTypes / remindPersons}
 * 提醒处理人。</p>
 *
 * <p>起算方式 {@code startType}：1=节点到达（收到待办时间）；2=表单时间字段。</p>
 * <p>截止方式 {@code endType}：1=相对（起算 + durationMin）；2=固定时刻（每日 HH:mm）；3=表单时间字段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_timeout")
@Schema(description = "节点超时规则")
public class WfNodeTimeout extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义 ID")
    private Long defId;

    @Schema(description = "节点 Key")
    private String nodeKey;

    @Schema(description = "排序（升序执行）")
    private Integer seq;

    @Schema(description = "是否启用 1=是 0=否")
    private Integer enabled;

    @Schema(description = "起算方式 1=节点到达 2=表单时间字段")
    private Integer startType;

    @Schema(description = "起算=表单时间字段时的字段名")
    private String startField;

    @Schema(description = "截止=相对时长时的分钟数")
    private Integer durationMin;

    @Schema(description = "截止方式 1=相对 2=固定时刻(HH:mm) 3=表单时间字段")
    private Integer endType;

    @Schema(description = "截止=固定时刻时的 HH:mm")
    private String endFixedTime;

    @Schema(description = "截止=表单时间字段时的字段名")
    private String endField;

    @Schema(description = "超时动作 autoApprove/forward/assign/remind")
    private String actionWay;

    @Schema(description = "动作=forward 时的目标节点（扩展位）")
    private String targetNodeKey;

    @Schema(description = "动作=assign 时的指定操作者（人力资源 ID 逗号分隔）")
    private String operatorIds;

    @Schema(description = "动作意见")
    private String opinion;

    @Schema(description = "提醒方式 sys=流程提醒 ml=短信 sm=邮件（逗号分隔）")
    private String remindTypes;

    @Schema(description = "提醒对象：节点当前处理人 1=是 0=否")
    private Integer remindBeforeOperator;

    @Schema(description = "提醒对象：指定人员（人力资源 ID 逗号分隔）")
    private String remindPersons;

}
