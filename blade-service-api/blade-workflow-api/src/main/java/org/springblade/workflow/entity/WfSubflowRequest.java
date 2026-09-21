package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 主 / 子流程请求关系（子流程高级设置）。
 *
 * <p>对齐 ecology {@code workflow_subwfrequest}：节点触发子流程时登记一条关系记录，
 * 据此实现「全部归档才能提交（阻塞主流程归档）/ 数据汇总 / 全部归档后提醒 / 归档后自动流转」。</p>
 *
 * <p>关键设计：子流程触发后，主流程的完成（归档）会被 {@code allEndBeforeSubmit} 阻塞，
 * 直到本表所有关联子流程均 {@code status=1 已归档}，再由归档钩子 {@code advance} 主实例。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_subflow_request")
@Schema(description = "主/子流程请求关系")
public class WfSubflowRequest extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主流程实例ID")
    private Long mainInstId;

    @Schema(description = "子流程实例ID（唯一）")
    private Long subInstId;

    @Schema(description = "子流程定义ID")
    private Long subDefId;

    @Schema(description = "触发子流程的主流程节点Key")
    private String mainNodeKey;

    @Schema(description = "是否「相同子流程」范围 1=是 0=否")
    private Integer isSame;

    @Schema(description = "快照：全部归档才能提交 1=是 0=否")
    private Integer allEndBeforeSubmit;

    @Schema(description = "快照：子流程归档后汇总数据到主流程 1=是 0=否")
    private Integer dataSummary;

    @Schema(description = "快照：全部归档后自动流转主流程 1=是 0=否")
    private Integer autoForward;

    @Schema(description = "快照：全部归档后提醒 1=是 0=否")
    private Integer remindEnabled;

    @Schema(description = "提醒方式：含 sys=流程提醒 ml=短信 sm=邮件")
    private String remindTypes;

    @Schema(description = "提醒对象：节点操作者本人 1=是 0=否")
    private Integer remindBeforeOperator;

    @Schema(description = "提醒对象：指定人员（人力资源ID逗号分隔）")
    private String remindPersons;

    @Schema(description = "状态 0=进行中（未归档） 1=已归档")
    private Integer status;

    @Schema(description = "子流程归档时间")
    private java.util.Date archiveTime;

}
