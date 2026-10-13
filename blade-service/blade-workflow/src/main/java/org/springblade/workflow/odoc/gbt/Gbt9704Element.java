package org.springblade.workflow.odoc.gbt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * GB/T 9704 单个版式要素的定位与样式
 *
 * <p>坐标/尺寸单位统一为毫米(mm)，以页面左上角为原点；与公文字体（GB/T 9704 强制使用
 * 3 号仿宋 GB2312 等）通过 {@code fontFamily} / {@code fontSizePt} 表达。dataKey 指向
 * 表单字段或 {@code wf_odoc_meta.meta_key}，渲染时取值填充。</p>
 */
@Data
public class Gbt9704Element implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 要素类型 */
    @JsonProperty("type")
    private Gbt9704ElementType type;

    /** 左上角 X（mm，相对页面） */
    @JsonProperty("x")
    private double xMm;

    /** 左上角 Y（mm，相对页面） */
    @JsonProperty("y")
    private double yMm;

    /** 宽度（mm），0=自适应 */
    @JsonProperty("w")
    private double widthMm;

    /** 高度（mm），0=自适应 */
    @JsonProperty("h")
    private double heightMm;

    /** 字体族（如 仿宋_GB2312 / 方正小标宋简体） */
    @JsonProperty("font")
    private String fontFamily;

    /** 字号（pt） */
    @JsonProperty("size")
    private double fontSizePt;

    /** 对齐：left / center / right / justify */
    @JsonProperty("align")
    private String align;

    /** 是否必出要素（缺数据时报缺项，不渲染留白） */
    @JsonProperty("required")
    private boolean required;

    /** 数据源键：表单字段名或 wf_odoc_meta.meta_key；空=静态文本/图形(如红头线) */
    @JsonProperty("dataKey")
    private String dataKey;

    /** 静态文本（dataKey 为空时使用，如红头机关名由模板决定时） */
    @JsonProperty("staticText")
    private String staticText;

    public Gbt9704Element() {
    }

    public Gbt9704Element(Gbt9704ElementType type, double xMm, double yMm, double widthMm,
                          double heightMm, String fontFamily, double fontSizePt, String align,
                          boolean required, String dataKey) {
        this.type = type;
        this.xMm = xMm;
        this.yMm = yMm;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.fontFamily = fontFamily;
        this.fontSizePt = fontSizePt;
        this.align = align;
        this.required = required;
        this.dataKey = dataKey;
    }
}
