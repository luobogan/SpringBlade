package org.springblade.portal.constant;

/**
 * 门户模块常量
 */
public interface PortalConstant {

    /**
     * 门户服务名称（Nacos 注册名）
     */
    String APPLICATION_PORTAL_NAME = "blade-portal";

    /**
     * 已登录即可访问（门户查看页、数据接口）
     * <p>与 WorkflowConstant.HAS_AUTH 同口径：框架里「已登录」是 {@code hasAuth()}，
     * <b>不是</b> {@code hasRole('user')}——后者会因角色名不匹配抛认证异常（401）。</p>
     */
    String HAS_AUTH = "hasAuth()";

    /**
     * 管理员角色表达式（历史口径）
     * <p>管理端门禁已统一为「HAS_AUTH + PortalRightUtil.check(权限码)」双层判定，
     * 不再使用本角色硬编码；保留供兼容与排查参考（对标 PortalRight §17.2）。</p>
     */
    String HAS_ADMIN = "hasRole('administrator')";

    /**
     * 元素默认显示条数（用户个性化未配置时）
     */
    int DEFAULT_PER_PAGE = 5;

    /* ==================== 门户元素类型（对标 hpelement.frommodule） ==================== */

    /**
     * 待办事项元素
     */
    String ELEMENT_TODO = "todo";

    /**
     * 通知公告元素（数据源 blade_notice）
     */
    String ELEMENT_NOTICE = "notice";

    /**
     * 消息提醒元素
     */
    String ELEMENT_MESSAGE = "message";

    /**
     * 图表元素（configJson 存 @ant-design/plots 配置）
     */
    String ELEMENT_CHART = "chart";

    /**
     * 快捷链接元素
     */
    String ELEMENT_LINKS = "links";

    /**
     * 富文本内容元素
     */
    String ELEMENT_RICH = "rich";

    /* ==================== 页面共享范围（对标 hpElementSettingDetail.sharelevel） ==================== */

    /**
     * 共享：全员
     */
    int SHARE_ALL = 1;

    /**
     * 共享：部门
     */
    int SHARE_DEPT = 2;

    /**
     * 共享：个人
     */
    int SHARE_USER = 3;

    /**
     * 共享：角色（对标 shareinnerhp type=2 角色）
     */
    int SHARE_ROLE = 4;

    /**
     * 共享：分部（对标 shareinnerhp type=6 分部；SpringBlade 无独立分部表，
     * 以「用户部门树最浅非零祖先」为分部口径，自身即顶层部门时分部=自身）
     */
    int SHARE_BRANCH = 5;

    /* ==================== 数据元素组织范围（数据按组织过滤，§22 数据型元素按组织过滤） ==================== */
    /**
     * 按租户过滤（默认，且始终生效——租户隔离是底线）
     */
    int ORG_SCOPE_TENANT = 1;
    /**
     * 按部门过滤（仅展示本用户所属部门创建的数据）
     */
    int ORG_SCOPE_DEPT = 2;
    /**
     * 按分部过滤（仅展示本用户所属分部及其下属部门创建的数据）
     */
    int ORG_SCOPE_BRANCH = 3;

    /* ==================== 门户业务审计类型（对标 PortalLogInfo，见 §13.4） ==================== */

    String LOG_PAGE_CREATE = "PAGE_CREATE";
    String LOG_PAGE_UPDATE = "PAGE_UPDATE";
    String LOG_PAGE_DEFAULT = "PAGE_DEFAULT";
    String LOG_PAGE_DELETE = "PAGE_DELETE";
    String LOG_ELEMENT_SAVE = "ELEMENT_SAVE";
    String LOG_ELEMENT_BATCH = "ELEMENT_BATCH";
    String LOG_ELEMENT_DELETE = "ELEMENT_DELETE";
    String LOG_ELEMENT_SETTING = "ELEMENT_SETTING";
    String LOG_DEFAULT_APPOINT = "DEFAULT_APPOINT";
    String LOG_DEFAULT_CLEAR = "DEFAULT_CLEAR";
    String LOG_PAGE_IMPORT = "PAGE_IMPORT";

    /* ==================== 门户权限码（category=2 权限按钮，供 PortalRightUtil 校验，见 §13.4） ==================== */

    /** 门户页面维护 */
    String PERM_PAGE_MAINT = "portal_page_maint";
    /** 门户元素维护 */
    String PERM_ELEMENT_MAINT = "portal_element_maint";
    /** 设为默认门户 */
    String PERM_PAGE_DEFAULT = "portal_page_default";

    /* ==================== 角色别名（blade_role.role_alias） ==================== */

    /**
     * 超级管理员（PortalRightUtil 对 administrator 角色直通）
     */
    String ROLE_ADMINISTRATOR = "administrator";

    /* ==================== 门户菜单类型（对标 portalmenu §14.2） ==================== */

    /** 前端菜单（左侧导航，对标 leftmenu） */
    String MENU_TYPE_FRONT = "front";
    /** 顶部/后端导航菜单（对标 topmenu） */
    String MENU_TYPE_TOP = "top";
    /** 工具栏「更多」菜单 */
    String MENU_TYPE_TOOLBAR = "toolbar";
    /** 快捷入口菜单 */
    String MENU_TYPE_QUICK = "quick";

    /* ==================== 门户菜单链接模式 ==================== */

    /** 站内路由 */
    int LINK_MODE_INTERNAL = 1;
    /** 外部链接 */
    int LINK_MODE_EXTERNAL = 2;

    /* ==================== 门户菜单审计类型 ==================== */

    String LOG_MENU_CREATE = "MENU_CREATE";
    String LOG_MENU_UPDATE = "MENU_UPDATE";
    String LOG_MENU_DELETE = "MENU_DELETE";
    String LOG_MENU_SHARE = "MENU_SHARE";

    /* ==================== 门户菜单权限码 ==================== */

    /** 门户菜单维护 */
    String PERM_MENU_MAINT = "portal_menu_maint";

    /* ==================== 门户细分权限码（对标泛微 PortalRight 14 码 §17.2） ==================== */

    /** 门户主题维护（对标 MAIN_PORTAL→SystemTemplate:Edit） */
    String PERM_THEME_MAINT = "portal_theme_maint";
    /** 门户布局模板维护（对标 HOMEPAGE→homepage:Maint） */
    String PERM_LAYOUT_MAINT = "portal_layout_maint";
    /** 门户独立元素库维护（对标 MATERIAL_LIB 之外的元素库治理） */
    String PERM_ELEMENT_LIB_MAINT = "portal_element_lib_maint";
    /** 登录页皮肤维护（对标 LOGIN_PORTAL→LoginPageMaint） */
    String PERM_LOGIN_TEMPLATE_MAINT = "portal_login_template_maint";
    /** 门户应用设置维护（对标 portalsetting 全局单例） */
    String PERM_SETTING_MAINT = "portal_setting_maint";
    /** 快捷搜索维护（对标 quicksearchmanage） */
    String PERM_QUICK_SEARCH_MAINT = "portal_quick_search_maint";
    /** 门户插件维护（对标 pluginmanage） */
    String PERM_PLUGIN_MAINT = "portal_plugin_maint";
    /** 门户样式库/模板库维护（对标 menustylelib/elementstylelib/headlinestyle/elementtemplate） */
    String PERM_STYLE_LIB_MAINT = "portal_style_lib_maint";
    /** 门户素材库维护（对标 MATERIAL_LIB→MaterialLibrary:Maint） */
    String PERM_MATERIAL_MAINT = "portal_material_maint";
    /** 门户审计日志查看 */
    String PERM_LOG_VIEW = "portal_log_view";
    /** 门户个人设置管理（对标 accountmore 管理侧） */
    String PERM_ACCOUNT_MAINT = "portal_account_maint";

    /* ==================== 门户应用设置（对标 portalsetting §16.3） ==================== */

    /** 自动刷新范围：整页 */
    int SETTING_REFRESH_SCOPE_PAGE = 1;
    /** 自动刷新范围：指定元素 */
    int SETTING_REFRESH_SCOPE_ELEMENT = 2;
    /** RSS 读取类型：客户端 */
    int RSSTYPE_CLIENT = 1;
    /** RSS 读取类型：服务器 */
    int RSSTYPE_SERVER = 2;
    /** 默认自动刷新间隔（分钟） */
    int DEFAULT_REFRESH_MINS = 5;
    /** 字体：大 */
    int FONT_LARGE = 1;
    /** 字体：中 */
    int FONT_MEDIUM = 2;
    /** 字体：默认 */
    int FONT_DEFAULT = 3;
    /** 布局：舒适 */
    int LAYOUT_COMFORT = 1;
    /** 布局：标准 */
    int LAYOUT_STANDARD = 2;
    /** 布局：紧凑 */
    int LAYOUT_COMPACT = 3;

    /* ==================== 样式库/模板库/素材库枚举（对标 §14.2E/§15.3B/§15.4C） ==================== */

    /** 菜单样式：横向（对标 hpMenuStyle menuh） */
    String MENU_STYLE_H = "menuh";
    /** 菜单样式：纵向（对标 hpMenuStyle menuv） */
    String MENU_STYLE_V = "menuv";
    /** 元素样式分类：元素样式（对标 hpMenuStyle menustyletype='element'） */
    String ELEMENT_STYLE_KIND_ELEMENT = "element";
    /** 元素样式分类：头条样式（对标 headlinestyle §15.3 B4） */
    String ELEMENT_STYLE_KIND_HEADLINE = "headline";
    /** 新闻模板：内部新闻（对标 PAGENEWSTEMPLATE inner） */
    String NEWS_TEMPLATE_INNER = "inner";
    /** 新闻模板：外部新闻（对标 PAGENEWSTEMPLATE outer） */
    String NEWS_TEMPLATE_OUTER = "outer";
    /** 素材类型：图片 */
    String MATERIAL_IMAGE = "image";
    /** 素材类型：文件 */
    String MATERIAL_FILE = "file";
    /** 插件类型：JS 脚本 */
    String PLUGIN_TYPE_JS = "js";
    /** 插件类型：CSS 样式 */
    String PLUGIN_TYPE_CSS = "css";
    /** 插件类型：组件 */
    String PLUGIN_TYPE_COMPONENT = "component";

    /* ==================== 新增模块审计类型（对标 PortalLogInfo §13.4） ==================== */

    String LOG_SETTING_SAVE = "SETTING_SAVE";
    String LOG_SETTING_SYNC = "SETTING_SYNC";
    String LOG_ACCOUNT_SETTING = "ACCOUNT_SETTING";
    String LOG_QUICK_SEARCH_SAVE = "QUICK_SEARCH_SAVE";
    String LOG_QUICK_SEARCH_DELETE = "QUICK_SEARCH_DELETE";
    String LOG_QUICK_SEARCH_SHARE = "QUICK_SEARCH_SHARE";
    String LOG_PLUGIN_SAVE = "PLUGIN_SAVE";
    String LOG_PLUGIN_DELETE = "PLUGIN_DELETE";
    String LOG_MENU_STYLE_SAVE = "MENU_STYLE_SAVE";
    String LOG_MENU_STYLE_DELETE = "MENU_STYLE_DELETE";
    String LOG_ELEMENT_STYLE_SAVE = "ELEMENT_STYLE_SAVE";
    String LOG_ELEMENT_STYLE_DELETE = "ELEMENT_STYLE_DELETE";
    String LOG_ELEMENT_TEMPLATE_SAVE = "ELEMENT_TEMPLATE_SAVE";
    String LOG_ELEMENT_TEMPLATE_DELETE = "ELEMENT_TEMPLATE_DELETE";
    String LOG_MATERIAL_SAVE = "MATERIAL_SAVE";
    String LOG_MATERIAL_DELETE = "MATERIAL_DELETE";
    String LOG_NEWS_TEMPLATE_SAVE = "NEWS_TEMPLATE_SAVE";
    String LOG_NEWS_TEMPLATE_DELETE = "NEWS_TEMPLATE_DELETE";
    String LOG_PAGE_IMPORT_FAIL = "PAGE_IMPORT_FAIL";
    String LOG_PAGE_DUPLICATE = "PAGE_DUPLICATE";
}
