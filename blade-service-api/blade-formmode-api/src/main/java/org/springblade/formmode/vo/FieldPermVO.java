package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字段权限项（运行期消费，**非流程场景**）。
 *
 * <p><b>为什么在 formmode-api 再建一个同构 VO</b>：流程侧的
 * {@code org.springblade.workflow.vo.FieldPermVO} 位于 {@code blade-workflow-api}，
 * 而依赖方向是 {@code blade-workflow → blade-formmode-api}，
 * formmode <b>不能反向依赖</b> workflow-api（否则循环依赖）。</p>
 *
 * <p>两个 VO 的字段名与语义<b>完全对齐</b>（scope/fieldName/visible/editable/required/perm），
 * 因此前端 {@code NodePermissionResolver}（签名 {@code (fieldName, scope?) => {readonly, hidden, required}}）
 * 可<b>无差别消费</b>两侧数据，无需为流程/非流程写两套解析逻辑。</p>
 *
 * <p><b>数据来源差异</b>：流程侧来自 {@code wf_node_field_perm}（def_id + node_key + scope，
 * 三维度 is_visible/is_editable/is_required 完整）；本 VO 来自 {@code modefieldauthorize}
 * （modeid + objtype/objid，仅 rightvalue 1可见/2可编辑，<b>无 required 维度</b>），
 * 故 {@code required} 恒为 null，由前端回退到字段定义（{@code workflow_billfield}）的必填配置。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字段权限项（非流程场景）")
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

    /** 作用域。非流程场景固定 {@code main}（modefieldauthorize 无 scope 概念） */
    @Schema(description = "作用域：main（非流程场景无明细行级 scope，固定 main）")
    private String scope;

    /** 字段名（与 data-excelp-field / formData 的 key 对齐，前端据此定位控件） */
    @Schema(description = "字段名（workflow_billfield.field_name）")
    private String fieldName;

    @Schema(description = "字段是否显示")
    private Boolean visible;

    @Schema(description = "字段是否可编辑")
    private Boolean editable;

    /** 恒为 null（modefieldauthorize 无必填维度），前端回退字段定义 */
    @Schema(description = "字段是否必填（非流程场景恒 null，前端回退字段定义）")
    private Boolean required;

    /** 兼容派生列：0隐藏 1只读 2可编辑 3必填（由三维度推导） */
    @Schema(description = "兼容派生列：0隐藏 1只读 2可编辑 3必填")
    private Integer perm;

    public FieldPermVO(String scope, String fieldName, Boolean visible, Boolean editable, Boolean required) {
        this.scope = scope;
        this.fieldName = fieldName;
        this.visible = visible;
        this.editable = editable;
        this.required = required;
        this.perm = derivePerm();
    }

    /** 由旧的 perm 单值反推三维度（存量兼容） */
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
}
