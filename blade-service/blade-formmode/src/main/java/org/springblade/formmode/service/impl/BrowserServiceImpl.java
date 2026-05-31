package org.springblade.formmode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeBrowser;
import org.springblade.formmode.mapper.ModeBrowserMapper;
import org.springblade.formmode.service.IBrowserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

}
