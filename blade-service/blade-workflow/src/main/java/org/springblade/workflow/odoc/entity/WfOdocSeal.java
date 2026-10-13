package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 电子签章记录
 *
 * <p>记录 CA 签章与验章全过程：签章人、章型、坐标、厂商、证书序列号、签名算法、
 * RFC3161 时间戳令牌、签章/验章时间与结果。status: 0待签 1已签 2已验 3已撤销。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_seal")
@Schema(description = "电子签章记录")
public class WfOdocSeal extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "被签章版式文件")
    private Long layoutFileId;

    @Schema(description = "签章人")
    private Long signerId;

    @Schema(description = "章型 1个人签字 2单位公章 3时间戳")
    private Integer sealType;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "横坐标偏移")
    private Double offsetX;

    @Schema(description = "纵坐标偏移")
    private Double offsetY;

    @Schema(description = "CA厂商 qiyuesuo/...")
    private String vendor;

    @Schema(description = "厂商侧签章/合同ID")
    private String vendorSealId;

    @Schema(description = "签章后文件")
    private Long sealFileId;

    @Schema(description = "签章证书序列号")
    private String certSn;

    @Schema(description = "签名算法 RSA/SM2")
    private String algorithm;

    @Schema(description = "RFC3161时间戳令牌")
    private String tsaToken;

    @Schema(description = "状态 0待签 1已签 2已验 3已撤销")
    private Integer status;

    @Schema(description = "签章时间")
    private LocalDateTime signTime;

    @Schema(description = "验章时间")
    private LocalDateTime verifyTime;

    @Schema(description = "验章结果")
    private String verifyResult;

}
