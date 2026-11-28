package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 授权方案保存请求：一个方案只对应一个授权对象。 */
@Data
@Schema(description = "授权方案保存请求")
public class ModeAuthSchemeDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 内部使用：复制方案时允许同授权对象存在多个方案 */
    private Boolean allowDuplicate;
    /** 表单ID（workflow_bill.id），必填 */
    private Long formId;
    /** 模块ID；不传则按 formId 反查，无模块时用 formId */
    private Long modeId;
    private String schemeName;
    /** 1角色 2部门 3岗位 4人员 5分部 6矩阵 7全员 */
    private Integer subjectType;
    private Long subjectId;
    private String subjectName;
    private Integer includeSub;
    private String matrixValueField;
    private Integer showlevel;
    private Integer showlevel2;
    private Integer priority;

    private List<ModeRightInfoDTO> rights;
    private List<FieldAuthGroupDTO> fieldGroups;
    private List<ModeDataShareRuleDTO> dataScopes;
    private MenuSettingDTO menu;
    /** 表单建模列表的数据范围（与动态列表第四步数据范围独立） */
    private FormManageScopeDTO formManageScope;

    @Data
    @Schema(description = "模块权限行")
    public static class ModeRightInfoDTO implements Serializable {
        private Long id;
        private Integer righttype;
        private String objname;
        private Integer includesub;
        private Integer showlevel;
        private Integer showlevel2;
    }

    @Data
    @Schema(description = "数据范围规则")
    public static class ModeDataShareRuleDTO implements Serializable {
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
        /** 1全部 2本人 3所在机构 4所在机构及子级 5自定义 */
        private Integer scopeType;
        private String scopeColumn;
        private String scopeValue;
        /** 多选权限类型：1查看 2新增 3编辑 4删除 5详情 */
        private List<Integer> rightTypes;
        /** 多授权对象：objtype 1角色 2部门 3岗位 4人员 5分部 6矩阵 7全员 */
        private List<ScopeSubjectDTO> subjects;
    }

    @Data
    @Schema(description = "数据范围授权对象")
    public static class ScopeSubjectDTO implements Serializable {
        private Integer objtype;
        private Long objid;
        private Integer includeSub;
    }

    @Data
    @Schema(description = "表单建模数据范围")
    public static class FormManageScopeDTO implements Serializable {
        /** 1全部表单 2本人创建 3指定表单 */
        private Integer scopeType;
        /** 指定表单ID，逗号分隔 */
        private String scopeValue;
    }

    @Data
    @Schema(description = "菜单设置")
    public static class MenuSettingDTO implements Serializable {
        private Long menuId;
        private String formMenuName;
        /** 上级菜单ID；0=顶层 */
        private Long parentId;
        /** 路由地址；按方案保存时由后端生成 /formmode/formview/{formId}/scheme/{schemeId} */
        private String menuPath;
        /** 菜单图标（blade_menu.source） */
        private String menuIcon;
        /** 菜单别名 */
        private String menuAlias;
        /** 同级排序 */
        private Integer menuSort;
        /** 显示状态：1显示 0隐藏 */
        private Integer menuStatus;
        private List<MenuShareDTO> menuShares;
        private java.util.Map<String, String> roleMenuNames;
    }
}
