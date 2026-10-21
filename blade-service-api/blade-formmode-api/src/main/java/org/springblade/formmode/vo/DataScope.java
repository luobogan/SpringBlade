package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据范围解析结果（行级数据权限）。
 *
 * <ul>
 *   <li>{@code canViewAll=true}：命中共享规则/管理员，可见该模块<b>全部</b>数据；</li>
 *   <li>{@code canViewAll=false}：未命中，默认仅可见 {@code creatorId} 本人创建的数据。</li>
 * </ul>
 */
@Data
@Schema(description = "数据范围解析结果")
public class DataScope {

    @Schema(description = "是否可见该模块全部数据（命中共享规则）")
    private boolean canViewAll;

    @Schema(description = "仅本人创建可见时的创建人ID（canViewAll=false 时有效）")
    private Long creatorId;
}
