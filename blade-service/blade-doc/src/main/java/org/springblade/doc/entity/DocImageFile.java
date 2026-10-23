package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档 ↔ 文件关联（对应 E9 DocImageFile）：附件/正文文件及版本。
 *
 * <p>docFileType：1正文图片 2附件 3Word 4Excel 5PPT 6WPS 7docx 8xlsx 9pptx 10et 11html视频。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_image_file")
@Schema(description = "文档文件关联")
public class DocImageFile extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "文件版本ID")
    private Long versionId;

    @Schema(description = "真实文件ID(imagefile)")
    private Long fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件类型 1正文图片 2附件 3-11 office等")
    private Integer docFileType;

    @Schema(description = "是否外部文件")
    private Integer isExtFile;

    @Schema(description = "外部文件OSS ID(系统通用附件上传)")
    private String ossFileId;

    @Schema(description = "外部文件OSS访问URL(系统通用附件上传)")
    private String ossFileUrl;

    @Schema(description = "附件本地相对路径(springBlade/uploads 下，含 docId/文件名)")
    private String filePath;

    @Schema(description = "是否使用模板")
    private Integer hasUsedTemplet;

    @Schema(description = "版本说明")
    private String versionDetail;
}
