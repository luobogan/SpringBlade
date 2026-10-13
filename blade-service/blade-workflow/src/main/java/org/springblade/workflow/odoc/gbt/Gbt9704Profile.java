package org.springblade.workflow.odoc.gbt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GB/T 9704-2012 版式规则（一套完整页面布局描述）
 *
 * <p>作为 {@code wf_odoc_gbt_profile.profile_json} 的序列化载体；不依赖任何厂商 SDK 或 AI，
 * 是 L2 版式能力的地基（P0 前置项）。所有坐标/尺寸单位为毫米(mm)，页面以 A4 为基准。</p>
 */
@Data
public class Gbt9704Profile implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 标准版本（默认 GB/T 9704-2012） */
    @JsonProperty("standardVer")
    private String standardVer = "GB/T 9704-2012";

    /** 页面宽（mm，A4=210） */
    @JsonProperty("pageW")
    private double pageWidthMm = 210.0;

    /** 页面高（mm，A4=297） */
    @JsonProperty("pageH")
    private double pageHeightMm = 297.0;

    /** 上边距（mm，标准 37±1） */
    @JsonProperty("marginTop")
    private double marginTopMm = 37.0;

    /** 下边距（mm，标准 35±1） */
    @JsonProperty("marginBottom")
    private double marginBottomMm = 35.0;

    /** 左边距（mm，标准 28±1） */
    @JsonProperty("marginLeft")
    private double marginLeftMm = 28.0;

    /** 右边距（mm，标准 26±1） */
    @JsonProperty("marginRight")
    private double marginRightMm = 26.0;

    /** 正文行高（mm，3 号字约 7.2mm/行，22 行/页） */
    @JsonProperty("lineH")
    private double lineHeightMm = 7.2;

    /** 正文默认字体 */
    @JsonProperty("bodyFont")
    private String bodyFontFamily = "仿宋_GB2312";

    /** 正文默认字号（pt，3 号=16pt，但版式常用小三号，这里按 16） */
    @JsonProperty("bodySize")
    private double bodyFontSizePt = 16.0;

    /** 要素清单（按渲染顺序） */
    @JsonProperty("elements")
    private List<Gbt9704Element> elements = new ArrayList<>();

    public Gbt9704Profile() {
    }

    /** 序列化为 JSON（写入 profile_json 列） */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("GB/T 9704 profile serialize failed", e);
        }
    }

    /** 从 JSON 反序列化（读取 profile_json 列） */
    public static Gbt9704Profile fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Gbt9704Profile.class);
        } catch (Exception e) {
            throw new IllegalStateException("GB/T 9704 profile parse failed", e);
        }
    }
}
