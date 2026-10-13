package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文流程设置（流程级开关）
 *
 * <p>承载收发文流程的「流程级」配置：正文字段、套红/签章/清稿/打印/定稿/转版式节点、
 * layoutFormat 三层开关（0 不启用 / 12 PDF / 13 OFD）。是 L1/L2 共存与流程级灰度的核心。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_process_set")
@Schema(description = "公文流程设置")
public class WfOdocProcessSet extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "公文类型 0非公文 1发文 2收文 3签报")
    private Integer officalType;

    @Schema(description = "正文字段(表单存doc_id)")
    private String docFieldKey;

    @Schema(description = "标题字段")
    private String docTitleFieldKey;

    @Schema(description = "套红节点(对应 useTempletNode)")
    private String taohongNodeKey;

    @Schema(description = "签章节点,逗号分隔")
    private String signatureNodeKeys;

    @Schema(description = "清稿节点")
    private String cleanCopyNodeKeys;

    @Schema(description = "打印节点")
    private String printNodeKeys;

    @Schema(description = "文号字段")
    private String numberFieldKey;

    @Schema(description = "默认正文档目录")
    private Long defaultSecCategory;

    @Schema(description = "是否按权限选套红模板")
    private Integer isPermissionTemp;

    @Schema(description = "默认套红模板")
    private Long taohongMouldId;

    @Schema(description = "套红节点提交前是否强制套红")
    private Integer mustTaohong;

    @Schema(description = "定稿节点")
    private String finalizeNodeKey;

    @Schema(description = "转版式节点")
    private String layoutNodeKey;

    @Schema(description = "签章节点")
    private String signNodeKey;

    @Schema(description = "版式格式 12PDF 13OFD 0=不启用")
    private Integer layoutFormat;

    @Schema(description = "OFD厂商(空=取全局配置)")
    private String ofdVendor;

    @Schema(description = "是否必须电子签章")
    private Integer sealRequired;

    @Schema(description = "签章位置配置(页/坐标/章型) JSON")
    private String sealConfig;

}
