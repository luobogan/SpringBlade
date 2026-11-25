package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 模块权限「一次性保存」请求：模块权限 + 字段权限 + 数据范围三表同一事务写入。
 *
 * <p>此前前端用 Promise.all 并发调三个独立接口、后端各自事务，任一失败会留下
 * 「模块权限已改、字段权限未改」的跨表不一致中间态。聚合端点保证三表原子提交。</p>
 */
@Data
@Schema(description = "模块权限批量保存请求（三表原子）")
public class ModeRightBatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模块权限列表")
    private List<ModeRightInfo> rights;

    @Schema(description = "字段权限列表（老的单主体格式）")
    private List<ModeFieldAuthorize> fieldAuthorizes;

    @Schema(description = "字段权限授权对象组（多选对象共享一份字段权限）")
    private List<FieldAuthGroupDTO> fieldGroups;

    @Schema(description = "数据范围规则列表")
    private List<ModeDataShareRule> dataScopes;

    /**
     * 乐观锁版本号：前端加载配置时从 GET /right/version/{modeId} 取得，保存时原样带回。
     *
     * <p>null 表示「跳过版本校验」（兼容旧调用方 / 脚本）。非空时服务端比对当前版本，
     * 不一致说明配置已被他人修改，拒绝保存并提示刷新，避免全量替换静默覆盖。</p>
     */
    @Schema(description = "乐观锁版本号（null=跳过校验）")
    private Long expectedVersion;

    /**
     * 是否把「表单数据」菜单授权给模块权限里配置的角色（objtype=1）。
     *
     * <p>null / true：授权（默认）；false：不授权。授权后这些角色的用户可从
     * 「表单建模 → 表单数据」菜单进入表单选择器查看该表数据。</p>
     */
    @Schema(description = "是否同步授权表单数据菜单给配置角色（null/true=是）")
    private Boolean grantMenuAccess;

    /** 表单ID（方案C：一表单一菜单；字符串避免雪花ID精度丢失） */
    @Schema(description = "表单ID（用于创建一表单一菜单）")
    private String formId;

    /** 该表单的数据菜单默认显示名（空则用表单名/默认名） */
    @Schema(description = "该表单数据菜单默认显示名")
    private String formMenuName;

    /** 角色级菜单显示名：roleId字符串 -> 该角色下菜单名（空串=删除别名） */
    @Schema(description = "角色级菜单显示名")
    private java.util.Map<String, String> roleMenuNames;

    /** 表单数据菜单可见范围（多主体） */
    @Schema(description = "表单数据菜单可见范围")
    private java.util.List<MenuShareDTO> menuShares;
}
