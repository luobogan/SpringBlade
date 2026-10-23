package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 文档操作日志（对应 E9 DocDetailLog）。
 *
 * <p>operateType：0阅读 1新增 2修改 13发布 14版本 18签出 20签入 21打印 22下载 23共享。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_detail_log")
@Schema(description = "文档操作日志")
public class DocDetailLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "操作类型")
    private Integer operateType;

    @Schema(description = "操作人")
    private Long operateUser;

    @Schema(description = "操作描述")
    private String operateDesc;

    @Schema(description = "客户端地址")
    private String clientAddress;
}
