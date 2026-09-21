package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点字段权限项（前端渲染用）。
 *
 * <p>对齐 {@code data-excelp-scope}（{@code main} / {@code dt{idx}} / {@code dt{idx}_r{row}}）与
 * {@code data-excelp-field} 定位锚点。</p>
 *
 * <p><b>三维度 vs perm</b>：ecology {@code workflow_nodeform} 用三列独立表达字段属性
 * （{@code isview} 显示 / {@code iseditable} 可编辑 / {@code ismandatory} 必填），
 * 本项目此前压成单列 {@code perm}（0隐藏/1只读/2可编辑/3必填）会**丢失组合维度**
 * （如「显示且不可编辑且非必填」与「显示+可编辑+必填」之外的大量组合无法表达）。</p>
 *
 * <p>本 VO 同时携带两套表示，保证平滑演进：</p>
 * <ul>
 *   <li>{@code visible / editable / required} —— <b>权威</b>三维度，前端以此渲染；</li>
 *   <li>{@code perm} —— <b>兼容派生列</b>，由三维度推导，供尚未升级的消费方（旧前端 / 巡检）使用。</li>
 * </ul>
 */
@Data
@Schema(description = "节点字段权限项")
public class FieldPermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** perm 兼容列取值：隐藏 */
    public static final int PERM_HIDDEN = 0;
    /** perm 兼容列取值：只读 */
    public static final int PERM_READONLY = 1;
    /** perm 兼容列取值：可编辑 */
    public static final int PERM_EDITABLE = 2;
    /** perm 兼容列取值：必填 */
    public static final int PERM_REQUIRED = 3;

    @Schema(description = "作用域：main | dt{idx} | dt{idx}_r{row}（与 data-excelp-scope 对齐；dt{idx}_r{row} 为 B5 行级）")
    private String scope;

    @Schema(description = "字段名（与 data-excelp-field 对齐）")
    private String fieldName;

    @Schema(description = "字段是否显示")
    private Boolean visible;

    @Schema(description = "字段是否可编辑")
    private Boolean editable;

    @Schema(description = "字段是否必填")
    private Boolean required;

    @Schema(description = "兼容派生列：0隐藏 1只读 2可编辑 3必填（由 visible/editable/required 推导，读取时可缺省）")
    private Integer perm;

    public FieldPermVO() {
    }

    /** 兼容构造：以旧的 perm 单值创建（三维度按 perm 反推） */
    public FieldPermVO(String scope, String fieldName, Integer perm) {
        this.scope = scope;
        this.fieldName = fieldName;
        applyPerm(perm);
    }

    /** 以三维度创建 */
    public FieldPermVO(String scope, String fieldName, Boolean visible, Boolean editable, Boolean required) {
        this.scope = scope;
        this.fieldName = fieldName;
        this.visible = visible;
        this.editable = editable;
        this.required = required;
        this.perm = derivePerm();
    }

    /**
     * 由旧的 perm 单值反推三维度（<b>存量兼容</b>：三列全 NULL 的历史行走这条）。
     *
     * <p>规则与迁移脚本 {@code V2026.09.21_001} 的回填一致：
     * 0→(隐藏) / 1→(显示,只读) / 2→(显示,可编辑) / 3→(显示,可编辑,必填)；perm 为空按「可编辑」。</p>
     */
    public void applyPerm(Integer p) {
        int v = (p == null) ? PERM_EDITABLE : p;
        this.perm = v;
        this.visible = v >= PERM_READONLY;
        this.editable = v >= PERM_EDITABLE;
        this.required = v == PERM_REQUIRED;
    }

    /** 由三维度派生 perm 兼容列 */
    public Integer derivePerm() {
        boolean vis = Boolean.TRUE.equals(visible);
        boolean edt = Boolean.TRUE.equals(editable);
        boolean req = Boolean.TRUE.equals(required);
        if (!vis) {
            return PERM_HIDDEN;
        }
        if (req) {
            return PERM_REQUIRED;
        }
        return edt ? PERM_EDITABLE : PERM_READONLY;
    }

    /** 三维度是否全部缺省（用于判定是否需要回退到 perm） */
    public boolean threeDimsAbsent() {
        return visible == null && editable == null && required == null;
    }

}
