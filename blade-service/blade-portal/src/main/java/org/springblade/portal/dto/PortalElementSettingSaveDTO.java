package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户元素个性化设置入参
 *
 * <p>对标泛微 hpElementSettingDetail：元素定义（blade_portal_element）是全局的，
 * 每个用户可覆盖显示条数 / 是否显示标题等。userId 取当前登录人，不接受入参传入
 * （防止越权改他人设置）。</p>
 */
@Data
@Schema(name = "门户元素个性化设置", description = "当前用户对某个门户元素的个性化设置")
public class PortalElementSettingSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "元素ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long elementId;

    @Schema(name = "显示条数", description = "为空或<=0 表示用元素默认值")
    private Integer perPage;

    @Schema(name = "是否显示标题（0否 1是）")
    private Integer showTitle;

    @Schema(name = "元素样式覆盖JSON字符串", description = "用户级样式覆盖（与元素定义 style 合并，后者优先）；传空字符串=清除个人样式覆盖，传 null=不改变")
    private String styleJson;
}
