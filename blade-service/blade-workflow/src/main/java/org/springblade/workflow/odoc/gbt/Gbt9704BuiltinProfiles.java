package org.springblade.workflow.odoc.gbt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GB/T 9704 内置版式规则库（15 文种版式模板的数据源）
 *
 * <p>提供三套基准版式：standard（通用公文）、command（命令/令）、minutes（纪要）。
 * 15 文种中 13 种共用 standard，命令用 command，纪要用 minutes。
 * 坐标为近似公制定位（mm，A4），精确排版参数可在管理页按机关/文种微调。</p>
 *
 * <p>本类不依赖任何厂商 SDK 或 AI，是 L2 版式能力（PDF/OFD 渲染、版心校验）的地基；
 * 通过 {@link #seedProfiles()} 可批量生成 {@link Gbt9704Profile} 供数据库 {@code wf_odoc_gbt_profile}
 * 初始化种子数据（见 {@code Gbt9704ProfileSeed}）。</p>
 */
public final class Gbt9704BuiltinProfiles {

    /** 版式正文区左边界（mm） */
    private static final double L = 28.0;
    /** 版式正文区右边界（mm） */
    private static final double R = 184.0;
    /** 版式正文区宽（mm） */
    private static final double W = R - L;
    /** 标题字体 */
    private static final String TITLE_FONT = "方正小标宋简体";
    /** 仿宋（3 号字） */
    private static final String FANGSONG = "仿宋_GB2312";
    /** 标题字号（pt，约 2 号） */
    private static final double TITLE_SIZE = 22.0;
    /** 3 号字（pt） */
    private static final double SIZE_3 = 16.0;

    private Gbt9704BuiltinProfiles() {
    }

    /** standard 通用公文版式 */
    public static Gbt9704Profile standard() {
        Gbt9704Profile p = base();
        List<Gbt9704Element> e = p.getElements();
        e.add(el(Gbt9704ElementType.RED_HEAD, L, 37, W, 20, TITLE_FONT, TITLE_SIZE, "center", true, "redHeadText"));
        e.add(el(Gbt9704ElementType.RED_LINE, L, 58, W, 0, null, 0, "center", true, null));
        e.add(el(Gbt9704ElementType.DOC_NUMBER, L, 62, W, 8, FANGSONG, SIZE_3, "center", true, "number"));
        e.add(el(Gbt9704ElementType.SECRET_LEVEL, 140, 40, 44, 7, FANGSONG, SIZE_3, "right", false, "secretLevel"));
        e.add(el(Gbt9704ElementType.URGENCY, 140, 48, 44, 7, FANGSONG, SIZE_3, "right", false, "urgency"));
        e.add(el(Gbt9704ElementType.TITLE, L, 92, W, 30, TITLE_FONT, TITLE_SIZE, "center", true, "title"));
        e.add(el(Gbt9704ElementType.MAIN_SEND, L, 126, W, 8, FANGSONG, SIZE_3, "left", false, "mainSend"));
        e.add(el(Gbt9704ElementType.SIGNER, 110, 126, 74, 8, FANGSONG, SIZE_3, "right", false, "signer"));
        e.add(el(Gbt9704ElementType.BODY, L, 140, W, 90, FANGSONG, SIZE_3, "justify", true, "body"));
        e.add(el(Gbt9704ElementType.ATTACHMENT, L, 232, W, 8, FANGSONG, SIZE_3, "left", false, "attachment"));
        e.add(el(Gbt9704ElementType.ISSUE_ORG, 110, 244, 74, 8, FANGSONG, SIZE_3, "right", false, "issueOrg"));
        e.add(el(Gbt9704ElementType.ISSUE_DATE, 110, 252, 74, 8, FANGSONG, SIZE_3, "right", true, "writtenDate"));
        e.add(el(Gbt9704ElementType.SEAL, 110, 240, 74, 30, null, 0, "center", false, null));
        e.add(el(Gbt9704ElementType.REMARK, L, 256, W, 6, FANGSONG, 12.0, "left", false, "remark"));
        e.add(el(Gbt9704ElementType.COPY_SEND, L, 262, W, 8, FANGSONG, 12.0, "left", false, "copySend"));
        e.add(el(Gbt9704ElementType.PRINT_ORG, L, 270, W, 8, FANGSONG, 12.0, "left", true, "printOrg"));
        e.add(el(Gbt9704ElementType.PAGE_NO, L, 287, W, 6, FANGSONG, 12.0, "center", true, null));
        return p;
    }

    /** command 命令/令版式：版头为发文机关标志+令号，无红色分隔线 */
    public static Gbt9704Profile command() {
        Gbt9704Profile p = base();
        List<Gbt9704Element> e = p.getElements();
        e.add(el(Gbt9704ElementType.RED_HEAD, L, 45, W, 28, TITLE_FONT, 30.0, "center", true, "redHeadText"));
        e.add(el(Gbt9704ElementType.COMMAND_NUMBER, L, 78, W, 10, FANGSONG, SIZE_3, "center", true, "number"));
        e.add(el(Gbt9704ElementType.SECRET_LEVEL, 140, 40, 44, 7, FANGSONG, SIZE_3, "right", false, "secretLevel"));
        e.add(el(Gbt9704ElementType.URGENCY, 140, 48, 44, 7, FANGSONG, SIZE_3, "right", false, "urgency"));
        e.add(el(Gbt9704ElementType.TITLE, L, 100, W, 24, TITLE_FONT, TITLE_SIZE, "center", true, "title"));
        e.add(el(Gbt9704ElementType.MAIN_SEND, L, 130, W, 8, FANGSONG, SIZE_3, "left", false, "mainSend"));
        e.add(el(Gbt9704ElementType.BODY, L, 144, W, 80, FANGSONG, SIZE_3, "justify", true, "body"));
        e.add(el(Gbt9704ElementType.ISSUE_ORG, 110, 244, 74, 8, FANGSONG, SIZE_3, "right", false, "issueOrg"));
        e.add(el(Gbt9704ElementType.ISSUE_DATE, 110, 252, 74, 8, FANGSONG, SIZE_3, "right", true, "writtenDate"));
        e.add(el(Gbt9704ElementType.SEAL, 110, 240, 74, 30, null, 0, "center", false, null));
        e.add(el(Gbt9704ElementType.REMARK, L, 256, W, 6, FANGSONG, 12.0, "left", false, "remark"));
        e.add(el(Gbt9704ElementType.PAGE_NO, L, 287, W, 6, FANGSONG, 12.0, "center", true, null));
        return p;
    }

    /** minutes 纪要版式：纪要标志居中红色、不加分隔线、无主送时版式收敛 */
    public static Gbt9704Profile minutes() {
        Gbt9704Profile p = base();
        List<Gbt9704Element> e = p.getElements();
        e.add(el(Gbt9704ElementType.MINUTES_MARK, L, 40, W, 18, TITLE_FONT, 26.0, "center", true, "redHeadText"));
        e.add(el(Gbt9704ElementType.SECRET_LEVEL, 140, 36, 44, 7, FANGSONG, SIZE_3, "right", false, "secretLevel"));
        e.add(el(Gbt9704ElementType.URGENCY, 140, 44, 44, 7, FANGSONG, SIZE_3, "right", false, "urgency"));
        e.add(el(Gbt9704ElementType.TITLE, L, 72, W, 24, TITLE_FONT, TITLE_SIZE, "center", false, "title"));
        e.add(el(Gbt9704ElementType.MAIN_SEND, L, 102, W, 8, FANGSONG, SIZE_3, "left", false, "mainSend"));
        e.add(el(Gbt9704ElementType.BODY, L, 116, W, 100, FANGSONG, SIZE_3, "justify", true, "body"));
        e.add(el(Gbt9704ElementType.ISSUE_DATE, L, 244, W, 8, FANGSONG, SIZE_3, "center", true, "writtenDate"));
        e.add(el(Gbt9704ElementType.SEAL, L, 240, W, 30, null, 0, "center", false, null));
        e.add(el(Gbt9704ElementType.COPY_SEND, L, 256, W, 8, FANGSONG, 12.0, "left", false, "copySend"));
        e.add(el(Gbt9704ElementType.PRINT_ORG, L, 264, W, 8, FANGSONG, 12.0, "left", true, "printOrg"));
        e.add(el(Gbt9704ElementType.PAGE_NO, L, 287, W, 6, FANGSONG, 12.0, "center", true, null));
        return p;
    }

    /** 按文种代码取基准版式（未知代码回退 standard） */
    public static Gbt9704Profile forDocType(String docTypeCode) {
        if (docTypeCode == null) {
            return standard();
        }
        for (Gbt9704DocType t : Gbt9704DocType.values()) {
            if (t.getCode().equalsIgnoreCase(docTypeCode)) {
                return forProfileKey(t.getProfileKey());
            }
        }
        return standard();
    }

    /** 按基准 key 取版式 */
    public static Gbt9704Profile forProfileKey(String key) {
        if ("command".equals(key)) {
            return command();
        }
        if ("minutes".equals(key)) {
            return minutes();
        }
        return standard();
    }

    /** 全部基准版式（key -> profile） */
    public static Map<String, Gbt9704Profile> allBaseProfiles() {
        Map<String, Gbt9704Profile> m = new LinkedHashMap<>();
        m.put("standard", standard());
        m.put("command", command());
        m.put("minutes", minutes());
        return m;
    }

    /** 15 文种 -> 版式（用于生成数据库种子：每个文种一条 wf_odoc_gbt_profile） */
    public static Map<String, Gbt9704Profile> seedProfiles() {
        Map<String, Gbt9704Profile> m = new LinkedHashMap<>();
        for (Gbt9704DocType t : Gbt9704DocType.values()) {
            m.put(t.getCode(), forProfileKey(t.getProfileKey()));
        }
        return m;
    }

    private static Gbt9704Profile base() {
        Gbt9704Profile p = new Gbt9704Profile();
        p.setStandardVer("GB/T 9704-2012");
        p.setPageWidthMm(210.0);
        p.setPageHeightMm(297.0);
        p.setMarginTopMm(37.0);
        p.setMarginBottomMm(35.0);
        p.setMarginLeftMm(L);
        p.setMarginRightMm(210.0 - R);
        p.setLineHeightMm(7.2);
        p.setBodyFontFamily(FANGSONG);
        p.setBodyFontSizePt(SIZE_3);
        p.setElements(new ArrayList<>());
        return p;
    }

    private static Gbt9704Element el(Gbt9704ElementType type, double x, double y, double w,
                                     double h, String font, double size, String align,
                                     boolean required, String dataKey) {
        Gbt9704Element e = new Gbt9704Element(type, x, y, w, h, font, size, align, required, dataKey);
        e.setStaticText(null);
        return e;
    }
}
