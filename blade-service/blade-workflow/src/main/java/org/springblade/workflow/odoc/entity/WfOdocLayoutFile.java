package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文版式文件（与 wf_odoc_doc_version 平行）
 *
 * <p>承载正文定稿/版式形态：L1 仅 PDF（format=12），L2 叠加 OFD（format=13），同一张表、
 * 同一套查询与前端分发；srcVersionId 溯源到 docx 版本。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_layout_file")
@Schema(description = "公文版式文件")
public class WfOdocLayoutFile extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "来源docx版本ID")
    private Long srcVersionId;

    @Schema(description = "版式格式 12PDF 13OFD")
    private Integer format;

    @Schema(description = "版式文件ID")
    private Long fileId;

    @Schema(description = "转换厂商 shuke/yozo/fuxin/fuxin-gsdk")
    private String vendor;

    @Schema(description = "版式阶段 3版式态 4签发态 5归档态")
    private Integer layoutStage;

    @Schema(description = "是否定稿(定稿后不可再编辑docx)")
    private Integer isFinal;

}
