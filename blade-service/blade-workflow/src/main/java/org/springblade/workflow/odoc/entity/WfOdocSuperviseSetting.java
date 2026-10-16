package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文督办设置（流程级，对应 E9 odoc_supervisesetting / supervisedelay / superviseremind）。
 *
 * <p>按流程定义配置：由谁督办、超期多少天触发、提醒方式。督办记录按实例落 {@link WfOdocSupervise}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_supervise_setting")
@Schema(description = "公文督办设置(流程级)")
public class WfOdocSuperviseSetting extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "公文类型 1发文 2收文 3签报")
    private Integer officalType;

    @Schema(description = "督办人ID,逗号分隔")
    private String supervisorIds;

    @Schema(description = "超期督办天数(实例发起/到达节点超过该天数未办结则督办)")
    private Integer delayDays;

    @Schema(description = "提醒方式(站内/邮件/短信,逗号分隔)")
    private String remindWay;

    @Schema(description = "是否启用 0否 1是")
    private Integer enabled;

}
