package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 公文电子签章签署方
 *
 * <p>挂在 {@link WfOdocSignFlow} 之下，一个签署流程可以有多个签署方，按
 * {@code signOrder} 顺序签署；{@code autoSign=1} 表示后台自动落章（无感知），
 * {@code autoSign=0} 需要签署人到签署页手动签。</p>
 *
 * <p>注意：父类 BaseEntity 的 {@code status} 是「1正常 0禁用」的公共列，
 * 本表的签署状态一律走 {@code signStatus}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_sign_signer")
@Schema(description = "公文电子签章签署方")
public class WfOdocSignSigner extends TenantEntity {

    /** 签署方类型：个人 */
    public static final int SIGNER_TYPE_PERSON = 0;
    /** 签署方类型：企业 */
    public static final int SIGNER_TYPE_ORG = 1;

    /** 签署状态：待签 */
    public static final int SIGN_STATUS_PENDING = 0;
    /** 签署状态：已签 */
    public static final int SIGN_STATUS_SIGNED = 1;
    /** 签署状态：拒签 */
    public static final int SIGN_STATUS_REJECTED = 2;

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联 wf_odoc_sign_flow.id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long flowId;

    @Schema(description = "签署方类型 0个人 1企业")
    private Integer signerType;

    @Schema(description = "签署顺序")
    private Integer signOrder;

    @Schema(description = "e签宝账号 psnId / orgId")
    private String accountId;

    @Schema(description = "签署方名称")
    private String accountName;

    @Schema(description = "个人账号（手机号）")
    private String psnAccount;

    @Schema(description = "企业统一社会信用代码")
    private String orgIdCard;

    @Schema(description = "0签署页手动 1后台自动落章")
    private Integer autoSign;

    @Schema(description = "印章ID：esign=e签宝 sealId；local=自研印章ID")
    private String sealId;

    @Schema(description = "签署链接")
    private String signUrl;

    @Schema(description = "签署状态 0待签 1已签 2拒签")
    private Integer signStatus;

    @Schema(description = "签署时间")
    private LocalDateTime signTime;

    /**
     * 主键以字符串形式序列化，避免 19 位雪花 ID 在前端 JS 解析时丢失精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
