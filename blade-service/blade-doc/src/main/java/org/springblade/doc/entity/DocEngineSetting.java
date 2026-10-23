package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 内容引擎 - 编辑器设置（键值对）。
 *
 * <p>当前仅使用 {@code default_engine}：默认编辑方案，取值 {@code univer} / {@code wps}；
 * 当 editor=auto 时由后端据此选择实际方案（WPS 还需已配置 appId/appSecret 才生效）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_engine_setting")
@Schema(description = "内容引擎-编辑器设置")
public class DocEngineSetting extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设置键")
    private String settingKey;

    @Schema(description = "设置值")
    private String settingValue;

    @Schema(description = "说明")
    private String remark;
}
