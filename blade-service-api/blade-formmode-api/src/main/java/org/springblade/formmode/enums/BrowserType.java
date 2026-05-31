package org.springblade.formmode.enums;

import lombok.Getter;

/**
 * 浏览框类型枚举
 */
@Getter
public enum BrowserType {

    SINGLE(161, "自定义单选浏览框"),
    MULTI(162, "自定义多选浏览框"),
    DATE(2, "日期"),
    TIME(19, "时间"),
    DEPARTMENT(1, "部门"),
    USER(3, "人员"),
    CUSTOMER(4, "客户"),
    SUPPLIER(5, "供应商"),
    PROJECT(6, "项目"),
    CONTRACT(7, "合同");

    private final int code;
    private final String name;

    BrowserType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BrowserType fromCode(int code) {
        for (BrowserType type : values()) {
            if (type.code == code) return type;
        }
        return null;
    }
}
