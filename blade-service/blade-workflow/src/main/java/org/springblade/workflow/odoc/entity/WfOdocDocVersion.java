package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文正文版本（对应 E9 docimagefile）
 *
 * <p>每次在线编辑保存、套红、取消套红回滚都在此产生新版本；{@code hasUsedTemplet}
 * 标记本版本是否已套红，取消套红回滚依赖版本历史。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_doc_version")
@Schema(description = "公文正文版本")
public class WfOdocDocVersion extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "版本序号(自增,按doc)")
    private Integer versionId;

    @Schema(description = "文件ID(指向文件服务)")
    private Long fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型 3doc 7docx 12pdf 13ofd")
    private Integer docFileType;

    @Schema(description = "本版本是否已套红 0否 1是")
    private Integer hasUsedTemplet;

    @Schema(description = "是否外部附件(不参与套红)")
    private Integer isExtFile;

    @Schema(description = "版本说明(如:套红前取消痕迹版本)")
    private String versionDetail;

}
