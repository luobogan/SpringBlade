package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

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

    /**
     * 条件化授权条件（结构化，非 SQL 片段）：命中共享规则且配置了条件时，行还需满足条件。
     *
     * <p>字段名/操作符/值来自 modedatasharerule 的 condition_* 列，运行期按白名单校验后参数化拼接。</p>
     */
    private RowCondition rowCondition;

    /**
     * 单条共享给我的数据行 ID 集合（E9 modeDataShare 语义）。
     *
     * <p>行级过滤时与「本人创建」OR 叠加：creator=我 OR id IN (sharedDataIds)。
     * 空/null 表示无单条共享。</p>
     */
    private List<Long> sharedDataIds;

    /** 单条共享中「可编辑」（shareLevel>=2）的数据行 ID 集合 */
    private List<Long> editableSharedDataIds;

    /** 行级条件项 */
    @Data
    @Schema(description = "行级数据条件")
    public static class RowCondition {
        private String field;
        private String op;
        private String value;
    }
}
