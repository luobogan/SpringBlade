package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统菜单多主体可见范围。
 *
 * <p>share_type：1角色 2人员 3部门 4分部 5岗位 6安全级别 7全员。</p>
 */
@Data
@Schema(description = "系统菜单可见范围")
public class MenuShareDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID（新增可不传）")
    private Long id;

    @Schema(description = "菜单ID（blade_menu.id）")
    private Long menuId;

    @Schema(description = "共享类型：1角色 2人员 3部门 4分部 5岗位 6安全级别 7全员")
    private Integer shareType;

    @Schema(description = "共享对象ID（角色/人员/部门/分部/岗位；全员可为0）")
    private Long shareId;

    @Schema(description = "是否包含子部门：1是 0否")
    private Integer includeSub;

    @Schema(description = "安全级别下限")
    private Integer showlevel;

    @Schema(description = "安全级别上限")
    private Integer showlevel2;
}
