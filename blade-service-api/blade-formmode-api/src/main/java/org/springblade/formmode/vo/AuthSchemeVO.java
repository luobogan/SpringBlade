package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "授权方案")
public class AuthSchemeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long formId;
    private String schemeName;
    private Integer subjectType;
    private Long subjectId;
    private String subjectName;
    private Integer includeSub;
    private String matrixValueField;
    private Integer showlevel;
    private Integer showlevel2;
    private Integer priority;
    private List<RightItem> rights;
    private List<FieldGroupItem> fieldGroups;
    private List<ScopeItem> dataScopes;
    private MenuItem menu;

    @Data
    public static class RightItem implements Serializable {
        private Long id;
        private Integer righttype;
        private String objname;
        private Integer includesub;
        private Integer showlevel;
        private Integer showlevel2;
    }

    @Data
    public static class FieldGroupItem implements Serializable {
        private Long id;
        private String groupKey;
        private String groupName;
        private List<FieldItem> fields;
    }

    @Data
    public static class FieldItem implements Serializable {
        private Long fieldId;
        private Integer rightvalue;
    }

    @Data
    public static class ScopeItem implements Serializable {
        private Long id;
        private Integer type;
        private Integer objtype;
        private Long objid;
        private Integer includeSub;
        private Integer creatorSelf;
        private String conditionField;
        private String conditionOp;
        private String conditionValue;
        private Integer conditionEnabled;
        private String matrixValueField;
        private Integer showlevel;
        private Integer showlevel2;
        private Integer scopeType;
        private String scopeColumn;
        private String scopeValue;
        private List<Integer> rightTypes;
        private List<ScopeSubjectItem> subjects;
    }

    @Data
    public static class ScopeSubjectItem implements Serializable {
        private Integer objtype;
        private Long objid;
        private Integer includeSub;
    }

    @Data
    public static class MenuItem implements Serializable {
        private Long menuId;
        private String formMenuName;
        private Long parentId;
        private String menuPath;
        private String menuIcon;
        private String menuAlias;
        private Integer menuSort;
        private Integer menuStatus;
        private List<MenuShareItem> menuShares;
        private java.util.Map<String, String> roleMenuNames;
    }

    @Data
    public static class MenuShareItem implements Serializable {
        private Long id;
        private Integer shareType;
        private Long shareId;
        private Integer includeSub;
        private Integer showlevel;
        private Integer showlevel2;
    }
}
