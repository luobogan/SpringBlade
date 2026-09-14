package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 流程实例实体
 *
 * <p>对齐 ecology {@code workflow_requestbase}。</p>
 * <ul>
 *   <li>status：0运行中 1通过 2不通过 3撤销 4暂停
 *       （对齐 ecology {@code currentstatus} 语义；注意 ecology 的 status 是状态文字描述，
 *       真正的状态码是 currentstatus，本项目只保留语义正确的单一 status）</li>
 *   <li>engine_inst_id：Flowable 实例ID，<b>弱关联</b>，业务语义不依赖 ACT_* 表，
 *       保留替换引擎的演进位</li>
 *   <li>biz_key：{@code formId:dataId}，支持按业务数据反查流程</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_instance")
@Schema(description = "流程实例")
public class WfInstance extends TenantEntity {

    /** 运行中 */
    public static final int STATUS_RUNNING = 0;
    /** 通过 */
    public static final int STATUS_APPROVED = 1;
    /** 不通过 */
    public static final int STATUS_REJECTED = 2;
    /** 撤销 */
    public static final int STATUS_CANCELED = 3;
    /** 暂停 */
    public static final int STATUS_SUSPENDED = 4;

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎实例ID（Flowable PROC_INST_ID_）")
    private String engineInstId;

    @Schema(description = "流程定义ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "表单ID（workflow_bill.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "业务数据ID（formtable_main_{id}.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dataId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "业务主键 formId:dataId")
    private String bizKey;

    @Schema(description = "当前节点")
    private String currentNodeKey;

    @Schema(description = "发起人")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long starter;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "发起时间")
    private Date startTime;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "紧急程度 0/1/2")
    private Integer urgency;

    @Schema(description = "父流程实例（子流程）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /**
     * 主键以字符串形式序列化（对齐 {@code WfProcessDefinition}）：
     * 避免 19 位雪花 ID 在前端 JS 解析时丢失精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
