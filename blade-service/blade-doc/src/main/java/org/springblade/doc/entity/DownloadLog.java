package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 下载日志（对应 E9 DownloadLog）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("download_log")
@Schema(description = "下载日志")
public class DownloadLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "下载人")
    private Long operateUser;

    @Schema(description = "客户端地址")
    private String clientAddress;
}
