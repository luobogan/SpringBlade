package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 外部交换平台设置（对接其他 OA，对应 E9 odoc_exchange_com_admin/com_* 配置）。
 *
 * <p>type：ws=WebService / ftp=FTP。真正收发由 {@code IOdocExchangeClient} 适配器完成，
 * 适配器实现属环境相关（依赖对方平台契约），此处仅持久化连接配置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_exchange_setting")
@Schema(description = "外部交换平台设置")
public class WfOdocExchangeSetting extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "平台编码(唯一)")
    private String platformCode;

    @Schema(description = "平台名称")
    private String platformName;

    @Schema(description = "交换方式 ws/ftp")
    private String type;

    @Schema(description = "服务地址(ws endpoint 或 ftp host:port)")
    private String endpoint;

    @Schema(description = "应用标识 appKey")
    private String appKey;

    @Schema(description = "密钥(加密存储)")
    private String appSecret;

    @Schema(description = "是否启用 0否 1是")
    private Integer enabled;

}
