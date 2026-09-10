package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 表单字段描述（condition 驱动）。
 *
 * <p>前端按此描述动态渲染表单控件，新增/调整字段只需改后端字段清单，无需前端改代码。
 * 该设计对齐 ecology「路径设置」的 conditioninfo 机制（后端下发字段元数据）。</p>
 */
@Data
@Schema(description = "表单字段描述（condition 驱动）")
public class FormFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段键（提交时使用）")
    private String key;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "控件类型：input/textarea/number/select/switch/formSelect/hidden")
    private String control;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "占位提示")
    private String placeholder;

    @Schema(description = "默认值")
    private Object defaultValue;

    @Schema(description = "静态选项（select / formSelect 的类型选项）")
    private List<OptionVO> options;

    @Schema(description = "动态数据源键（select 需要动态取数时）：formList 等")
    private String dataSource;

    @Schema(description = "下拉是否可搜索")
    private Boolean searchable;

    @Schema(description = "是否允许新增（显示 + 按钮）")
    private Boolean allowAdd;

    @Schema(description = "最大长度（input / textarea）")
    private Integer maxLength;

    @Schema(description = "最小值（number）")
    private Integer min;

    @Schema(description = "最大值（number）")
    private Integer max;

    @Schema(description = "文本域行数")
    private Integer rows;

    @Schema(description = "底部提示文案")
    private String tip;

    @Schema(description = "条件可见：依赖其他字段值显示/隐藏")
    private VisibleWhenVO visibleWhen;

    @Schema(description = "互斥校验：当 key 字段值 equals / in 命中时，本字段校验不通过（如自由流程与某路径类型不可同选）")
    private VisibleWhenVO conflict;

    @Schema(description = "互斥校验不通过时的提示文案")
    private String conflictMessage;

    @Schema(description = "只读（对齐 ecology viewAttr<3）：渲染时控件禁用")
    private Boolean readOnly;

    @Schema(description = "校验规则串，对齐 ecology SearchConditionItem.rules，如 required|stringLength:200")
    private String rules;

    @Schema(description = "select 渲染形态：1普通下拉 2多选 3单选；对齐 ecology detailtype")
    private Integer detailtype;

    @Schema(description = "浏览框参数（control=browser 时使用），对齐 ecology BrowserBean")
    private BrowserParamVO browser;

    @Schema(description = "值拆分映射（如 formSelect 拆成 formType / formId 两个字段）")
    private EmitsVO emits;

    @Data
    @Schema(description = "下拉选项")
    public static class OptionVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Object value;
        private String label;
    }

    @Data
    @Schema(description = "条件可见规则：当 key 字段的值 equals / in 命中时可见")
    public static class VisibleWhenVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String key;
        private Object equals;
        private List<Object> in;
    }

    @Data
    @Schema(description = "值拆分映射")
    public static class EmitsVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String typeKey;
        private String idKey;
    }

    @Data
    @Schema(description = "浏览框参数（对齐 ecology BrowserBean）")
    public static class BrowserParamVO implements Serializable {
        private static final long serialVersionUID = 1L;
        @Schema(description = "浏览框类型，如 workflowType / wfFormBrowser / doc")
        private String type;
        @Schema(description = "图标")
        private String icon;
        @Schema(description = "图标背景色")
        private String iconBgcolor;
        @Schema(description = "是否显示新增按钮（对齐 ecology hasAddBtn）")
        private Boolean hasAdd;
        @Schema(description = "是否多选（对齐 ecology multiple）")
        private Boolean multiple;
    }

}
