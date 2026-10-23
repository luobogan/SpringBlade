package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档共享主表（对应 E9 DocShare）：一行=一条共享规则。
 *
 * <p>shareType：1人员 2分部 3部门 4角色 5所有人 6群组 7客户 8客户类型 10岗位 80创建人 81直接上级 82所有上级 84本分部 85本部门；
 * shareLevel：1查看 2编辑 3完全控制。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_share")
@Schema(description = "文档共享")
public class DocShare extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "共享对象类型")
    private Integer shareType;

    @Schema(description = "共享对象ID")
    private Long targetId;

    @Schema(description = "共享级别 1查看 2编辑 3完全控制")
    private Integer shareLevel;

    @Schema(description = "是否继承自目录默认共享")
    private Integer isSecDefaultShare;

    @Schema(description = "共享来源")
    private String shareSource;
}
