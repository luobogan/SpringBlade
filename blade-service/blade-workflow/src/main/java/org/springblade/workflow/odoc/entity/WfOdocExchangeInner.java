package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 内部交换记录（本单位机构间，对应 E9 exchange_senddocinfo_inner / exchange_receive_doc_inner）。
 *
 * <p>把一条公文推送给目标机构：direction=1 发出（发起方视角）、2 接收（目标方视角）。
 * documentIdentifier 为交换生成的公文标识，issued_number_of_document 回写文号。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_exchange_inner")
@Schema(description = "内部交换记录")
public class WfOdocExchangeInner extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 发出 */
    public static final int DIRECTION_SEND = 1;
    /** 接收 */
    public static final int DIRECTION_RECEIVE = 2;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "发送人ID(发出视角归属;不依赖 create_user 自动填充)")
    private Long senderId;

    @Schema(description = "公文类型 1发文 2收文 3签报")
    private Integer officalType;

    @Schema(description = "方向 1发出 2接收")
    private Integer direction;

    @Schema(description = "对方机构/单位")
    private String targetUnit;

    @Schema(description = "公文标识(交换生成)")
    private String documentIdentifier;

    @Schema(description = "发文文号")
    private String issuedNum;

    @Schema(description = "印发/来文单位")
    private String issuedUnit;

    @Schema(description = "交换状态 0待接收 1已接收 2已退回")
    private Integer exchangeStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "测试态标记 1=测试产生(可一键清理)")
    private Integer isTest;

}
