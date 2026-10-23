package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档主表（对应 E9 DocDetail）。
 *
 * <p>docStatus 状态机：0草稿/1生效/2生效(审批或发布后)/3审批中/4退回/5归档/6待发布/7失效/8作废/9流程草稿；
 * 旧版本升级时 isHistory=1、docStatus=7。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_detail")
@Schema(description = "文档主表")
public class DocDetail extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属目录 secCategoryId")
    private Long secCategoryId;

    @Schema(description = "文档标题")
    private String docSubject;

    @Schema(description = "文档状态 0-9")
    private Integer docStatus;

    @Schema(description = "版本族ID")
    private Long docEditionId;

    @Schema(description = "版次号")
    private Integer docEdition;

    @Schema(description = "是否历史版本")
    private Integer isHistory;

    @Schema(description = "正文文件ID")
    private Long mainFileId;

    @Schema(description = "正文内容ID(关联 DocDetailContent)")
    private Long contentId;

    @Schema(description = "所属分部(DataScope 隔离维度)")
    private Long subCompanyId;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "是否有效")
    private Integer isValid;

    @Schema(description = "发布选用的显示模板ID")
    private Long selectedPubMouldId;

    @Schema(description = "签出状态 0未签出 1已签出")
    private Integer checkOutStatus;

    @Schema(description = "签出人")
    private Long checkOutUserId;

    @Schema(description = "审批流程实例ID(发起审批时回填)")
    private Long approveInstId;
}
