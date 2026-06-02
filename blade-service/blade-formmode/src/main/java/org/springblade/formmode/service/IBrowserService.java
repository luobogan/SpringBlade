package org.springblade.formmode.service;

import java.util.List;
import java.util.Map;

/**
 * 浏览框服务接口
 *
 * 对应 ecology 的 FormModeBrowserUtil
 */
public interface IBrowserService {

    /**
     * 查询浏览框数据
     */
    List<Map<String, Object>> queryBrowserData(Long browserId, Map<String, Object> params);

    /**
     * 根据ID获取浏览框数据
     */
    Map<String, Object> getBrowserDataById(Long browserId, String value);

    /**
     * 根据名称搜索浏览框数据
     */
    List<Map<String, Object>> searchBrowserDataByName(Long browserId, String keyword, Map<String, Object> params);

    /**
     * 执行自定义浏览框SQL查询
     */
    List<Map<String, Object>> executeBrowserSql(String sqlText, Map<String, Object> params);

    /**
     * 获取浏览按钮类型完整分类列表（35+种）
     * 用于前端 BrowserTypePicker 分类选择弹窗
     */
    List<Map<String, Object>> getBrowserTypes();

}
