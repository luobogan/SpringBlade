package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 子目录（对应 E9 DocSecCategory）：承载目录属性、编号、版本、模板、审批流、水印等开关。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sec_category")
@Schema(description = "文档子目录")
public class DocSecCategory extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录名")
    private String name;

    @Schema(description = "上级目录ID(0=主目录)")
    private Long parentId;

    @Schema(description = "管理角色ID")
    private Long manageRoleId;

    @Schema(description = "所属分部(DataScope 隔离维度)")
    private Long subCompanyId;

    @Schema(description = "是否开启水印")
    private Integer isOpenWater;

    @Schema(description = "编号前缀")
    private String coder;

    @Schema(description = "单文件上传大小限制(MB)")
    private Integer maxUploadFileSize;

    @Schema(description = "是否禁止下载")
    private Integer nodownload;

    @Schema(description = "是否开启版本管理")
    private Integer editionIsOpen;

    @Schema(description = "版本前缀")
    private String editionPrefix;

    @Schema(description = "读者是否可看历史版本")
    private Integer readerCanViewHistoryEdition;

    @Schema(description = "是否开启审批 0否 1审批工作流 2批准工作流")
    private Integer isOpenApproveWf;

    @Schema(description = "批准工作流ID")
    private Long approveWorkflowId;
}
