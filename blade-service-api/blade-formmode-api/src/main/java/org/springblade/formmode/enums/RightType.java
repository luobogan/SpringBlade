package org.springblade.formmode.enums;

import lombok.Getter;

/**
 * 权限类型枚举
 */
@Getter
public enum RightType {

    VIEW(1, "可见权限"),
    CREATE(2, "新建权限"),
    EDIT(3, "编辑权限"),
    DELETE(4, "删除权限"),
    DETAIL(5, "详情权限"),
    IMPORT(6, "导入权限"),
    EXPORT(7, "导出权限");

    private final int code;
    private final String name;

    RightType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RightType fromCode(int code) {
        for (RightType type : values()) {
            if (type.code == code) return type;
        }
        return VIEW;
    }
}
