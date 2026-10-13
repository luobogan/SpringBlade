package org.springblade.workflow.odoc.gbt;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 党政机关公文 15 文种（GB/T 9704-2012 适用）
 *
 * <p>每个文种映射到一套基准版式（standard / command / minutes）；命令与纪要的版式与
 * 通用公文差异较大（令号、纪要标志等），单独建模，其余 13 种共用 standard。</p>
 */
public enum Gbt9704DocType {

    JUEYI("JUEYI", "决议", "standard"),
    JUEDING("JUEDING", "决定", "standard"),
    MINGLING("MINGLING", "命令", "command"),
    GONGBAO("GONGBAO", "公报", "standard"),
    GONGGAO("GONGGAO", "公告", "standard"),
    TONGGAO("TONGGAO", "通告", "standard"),
    YIJIAN("YIJIAN", "意见", "standard"),
    TONGZHI("TONGZHI", "通知", "standard"),
    TONGBAO("TONGBAO", "通报", "standard"),
    BAOGAO("BAOGAO", "报告", "standard"),
    QINGSHI("QINGSHI", "请示", "standard"),
    PIFU("PIFU", "批复", "standard"),
    YIAN("YIAN", "议案", "standard"),
    HAN("HAN", "函", "standard"),
    JIYAO("JIYAO", "纪要", "minutes");

    private final String code;
    private final String name;
    private final String profileKey;

    Gbt9704DocType(String code, String name, String profileKey) {
        this.code = code;
        this.name = name;
        this.profileKey = profileKey;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProfileKey() {
        return profileKey;
    }
}
