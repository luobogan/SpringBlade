package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文正文文档（对应 E9 docdetail）
 *
 * <p>一份公文正文的主记录，与流程实例通过 {@code instanceId} 关联；{@code docEditionId}
 * 冗余指向当前版本便于取最新；{@code hasUsedTemplet} 标记是否已套红。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_doc")
@Schema(description = "公文正文文档")
public class WfOdocDoc extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "正文标题")
    private String title;

    @Schema(description = "存放目录ID")
    private Long secCategory;

    @Schema(description = "文档状态 9草稿 1正式")
    private Integer docStatus;

    @Schema(description = "当前版本ID(冗余,便于取最新)")
    private Long docEditionId;

    @Schema(description = "是否已套红 0否 1是")
    private Integer hasUsedTemplet;

    @Schema(description = "当前编辑模板ID")
    private Long editMouldId;

    @Schema(description = "是否测试流程产生的数据(可一键清理)")
    private Integer isTest;

}
