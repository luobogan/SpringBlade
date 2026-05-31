package org.springblade.formmode.enums;

import lombok.Getter;

/**
 * HTML字段类型枚举
 */
@Getter
public enum HtmlFieldType {

    TEXT(1, "单行文本", "varchar"),
    TEXTAREA(2, "多行文本", "text"),
    NUMBER(3, "数字", "decimal"),
    DATE(4, "日期", "varchar"),
    SELECT(5, "下拉框", "varchar"),
    CHECKBOX(7, "复选框", "varchar"),
    RADIO(8, "单选按钮", "varchar"),
    BROWSER(10, "浏览框", "varchar"),
    ATTACHMENT(11, "附件上传", "text"),
    HTML_EDITOR(14, "HTML编辑器", "text"),
    IMAGE(15, "图片框", "varchar"),
    PASSWORD(27, "密码框", "varchar"),
    FORMULA(36, "公式字段", "varchar");

    private final int code;
    private final String name;
    private final String dbType;

    HtmlFieldType(int code, String name, String dbType) {
        this.code = code;
        this.name = name;
        this.dbType = dbType;
    }

    public static HtmlFieldType fromCode(int code) {
        for (HtmlFieldType type : values()) {
            if (type.code == code) return type;
        }
        return TEXT;
    }
}
