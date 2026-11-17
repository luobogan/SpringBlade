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

    @Schema(description = "字段权限列表")
    private List<ModeFieldAuthorize> fieldAuthorizes;

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
}
