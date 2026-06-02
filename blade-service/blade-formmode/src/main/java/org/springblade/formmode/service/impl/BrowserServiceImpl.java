package org.springblade.formmode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeBrowser;
import org.springblade.formmode.mapper.ModeBrowserMapper;
import org.springblade.formmode.service.IBrowserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览框服务实现
 *
 * 对应 ecology 的 FormModeBrowserUtil
 */
@Service
@RequiredArgsConstructor
public class BrowserServiceImpl implements IBrowserService {

    private final ModeBrowserMapper modeBrowserMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> queryBrowserData(Long browserId, Map<String, Object> params) {
        ModeBrowser browser = modeBrowserMapper.selectById(browserId);
        if (browser == null) {
            return List.of();
        }

        // SQL数据源
        if ("0".equals(browser.getDatafrom()) && browser.getSqltext() != null && !browser.getSqltext().isBlank()) {
            return jdbcTemplate.queryForList(browser.getSqltext());
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getBrowserDataById(Long browserId, String value) {
        ModeBrowser browser = modeBrowserMapper.selectById(browserId);
        if (browser == null || browser.getSqltext() == null) {
            return null;
        }

        // 添加WHERE条件查询单条
        String sql = browser.getSqltext();
        String keyField = browser.getKeyfield() != null ? browser.getKeyfield() : "id";
        sql = "SELECT * FROM (" + sql + ") _b WHERE " + keyField + " = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, value);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<Map<String, Object>> searchBrowserDataByName(Long browserId, String keyword, Map<String, Object> params) {
        ModeBrowser browser = modeBrowserMapper.selectById(browserId);
        if (browser == null || browser.getSqltext() == null) {
            return List.of();
        }

        String showField = browser.getShowfield() != null ? browser.getShowfield() : "name";
        String sql = browser.getSqltext();

        // 添加LIKE搜索条件
        if (keyword != null && !keyword.isBlank()) {
            boolean hasWhere = sql.toUpperCase().contains("WHERE");
            sql += (hasWhere ? " AND " : " WHERE ") + showField + " LIKE '%" + keyword.replace("'", "''") + "%'";
        }

        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> executeBrowserSql(String sqlText, Map<String, Object> params) {
        return jdbcTemplate.queryForList(sqlText);
    }

    @Override
    public List<Map<String, Object>> getBrowserTypes() {
        List<Map<String, Object>> result = new ArrayList<>();

        // ========== 人员类别 ==========
        Map<String, Object> category1 = new HashMap<>();
        category1.put("categoryId", "personnel");
        category1.put("categoryName", "人员");
        List<Map<String, Object>> personnelTypes = new ArrayList<>();
        addType(personnelTypes, 1, "人力资源", "personnel");
        addType(personnelTypes, 161, "多人力资源", "personnel");
        addType(personnelTypes, 162, "应聘人", "personnel");
        addType(personnelTypes, 163, "多角色", "personnel");
        addType(personnelTypes, 165, "人力资源条件", "personnel");
        addType(personnelTypes, 166, "角色人员", "personnel");
        addType(personnelTypes, 167, "分权单人力资源", "personnel");
        addType(personnelTypes, 168, "分权多人力资源", "personnel");
        category1.put("types", personnelTypes);
        result.add(category1);

        // ========== 组织类别 ==========
        Map<String, Object> category2 = new HashMap<>();
        category2.put("categoryId", "organization");
        category2.put("categoryName", "组织");
        List<Map<String, Object>> orgTypes = new ArrayList<>();
        addType(orgTypes, 2, "部门", "organization");
        addType(orgTypes, 17, "多部门", "organization");
        addType(orgTypes, 18, "分部", "organization");
        addType(orgTypes, 19, "分权单部门", "organization");
        addType(orgTypes, 20, "分权多部门", "organization");
        addType(orgTypes, 21, "分权单分部", "organization");
        addType(orgTypes, 22, "分权多分部", "organization");
        addType(orgTypes, 23, "多分部", "organization");
        addType(orgTypes, 25, "办公地点", "organization");
        category2.put("types", orgTypes);
        result.add(category2);

        // ========== 流程类别 ==========
        Map<String, Object> category3 = new HashMap<>();
        category3.put("categoryId", "workflow");
        category3.put("categoryName", "流程");
        List<Map<String, Object>> workflowTypes = new ArrayList<>();
        addType(workflowTypes, 30, "流程", "workflow");
        addType(workflowTypes, 31, "多流程", "workflow");
        addType(workflowTypes, 32, "归档流程", "workflow");
        category3.put("types", workflowTypes);
        result.add(category3);

        // ========== 文档类别 ==========
        Map<String, Object> category4 = new HashMap<>();
        category4.put("categoryId", "document");
        category4.put("categoryName", "文档");
        List<Map<String, Object>> docTypes = new ArrayList<>();
        addType(docTypes, 24, "文档", "document");
        addType(docTypes, 26, "多文档", "document");
        category4.put("types", docTypes);
        result.add(category4);

        // ========== 系统类别 ==========
        Map<String, Object> category5 = new HashMap<>();
        category5.put("categoryId", "system");
        category5.put("categoryName", "系统");
        List<Map<String, Object>> sysTypes = new ArrayList<>();
        addType(sysTypes, 98, "日期", "system");
        addType(sysTypes, 99, "时间", "system");
        addType(sysTypes, 100, "省份", "system");
        addType(sysTypes, 101, "币种", "system");
        addType(sysTypes, 102, "城市", "system");
        addType(sysTypes, 103, "区县", "system");
        addType(sysTypes, 104, "年份", "system");
        addType(sysTypes, 105, "语言", "system");
        category5.put("types", sysTypes);
        result.add(category5);

        // ========== 客户类别 ==========
        Map<String, Object> category6 = new HashMap<>();
        category6.put("categoryId", "customer");
        category6.put("categoryName", "客户");
        List<Map<String, Object>> customerTypes = new ArrayList<>();
        addType(customerTypes, 16, "相关客户", "customer");
        category6.put("types", customerTypes);
        result.add(category6);

        // ========== 项目类别 ==========
        Map<String, Object> category7 = new HashMap<>();
        category7.put("categoryId", "project");
        category7.put("categoryName", "项目");
        List<Map<String, Object>> projectTypes = new ArrayList<>();
        addType(projectTypes, 8, "项目", "project");
        category7.put("types", projectTypes);
        result.add(category7);

        // ========== 资产类别 ==========
        Map<String, Object> category8 = new HashMap<>();
        category8.put("categoryId", "asset");
        category8.put("categoryName", "资产");
        List<Map<String, Object>> assetTypes = new ArrayList<>();
        addType(assetTypes, 57, "附件", "asset");
        category8.put("types", assetTypes);
        result.add(category8);

        // ========== 自定义类别 ==========
        Map<String, Object> category9 = new HashMap<>();
        category9.put("categoryId", "custom");
        category9.put("categoryName", "自定义");
        List<Map<String, Object>> customTypes = new ArrayList<>();
        addType(customTypes, 164, "自定义浏览按钮", "custom");
        addType(customTypes, 256, "自定义树形单选", "custom");
        addType(customTypes, 257, "自定义树形多选", "custom");
        category9.put("types", customTypes);
        result.add(category9);

        return result;
    }

    /**
     * 辅助方法：添加浏览按钮类型
     */
    private void addType(List<Map<String, Object>> list, Integer typeId, String label, String categoryId) {
        Map<String, Object> type = new HashMap<>();
        type.put("typeId", typeId);
        type.put("label", label);
        type.put("categoryId", categoryId);
        list.add(type);
    }

}
