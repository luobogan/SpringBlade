package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文元数据（写入 OFD 元数据区，档案检索用，对应 E9 getMetaData）
 *
 * <p>以 key/value 形式结构化存储公文要素（issuedNum/title/secretLevel/urgency 等），
 * 不依赖 OFD 即可检索，L1 同样可用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_meta")
@Schema(description = "公文元数据(OFD结构化)")
public class WfOdocMeta extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文ID")
    private Long docId;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "元数据键 如 issuedNum/title/secretLevel/urgency/writtenDate/issuedUnit")
    private String metaKey;

    @Schema(description = "元数据值")
    private String metaValue;

}
