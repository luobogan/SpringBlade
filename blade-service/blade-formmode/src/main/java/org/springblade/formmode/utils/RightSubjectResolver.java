package org.springblade.formmode.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.StringUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 建模权限「授权主体」统一解析器。
 *
 * <p>把此前散落在 {@code FormRightServiceImpl} / {@code ModeDataShareRuleServiceImpl}
 * 中「未实现」的三个维度收敛到一处，供模块权限 / 字段权限 / 数据范围共用：</p>
 * <ul>
 *   <li><b>管理员豁免</b>：roleName 含 admin → 拥有全部权限（与 DataScopeValueResolver 同源）；</li>
 *   <li><b>子部门展开</b>：部门授权带 includesub 时，展开当前部门 + 全部子孙部门；</li>
 *   <li><b>岗位维度</b>：从 blade_user.post_id 解析当前用户岗位（BladeUser 不承载 postId）。</li>
 * </ul>
 *
 * <p>所有查询均 fail-safe：异常只告警并返回空集合，不放大权限也不阻断业务。</p>
 */
@Slf4j
@Component
public class RightSubjectResolver {

    private final JdbcTemplate jdbcTemplate;

    public RightSubjectResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 当前用户是否为管理员（与 {@code DataScopeValueResolver#resolve} 的判定一致） */
    public boolean isAdmin(BladeUser user) {
        return user != null && StringUtil.containsAny(user.getRoleName(), RoleConstant.ADMIN);
    }

    /**
     * 解析当前用户的岗位 ID 集合（blade_user.post_id，逗号分隔多值）。
     *
     * <p>BladeUser 未承载 postId，故按 userId 直接查库；查不到 / 异常返回空集合。</p>
     */
    public Set<Long> resolvePostIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        try {
            List<String> rows = jdbcTemplate.queryForList(
                "SELECT post_id FROM blade_user WHERE id = ? AND is_deleted = 0", String.class, userId);
            Set<Long> ids = new LinkedHashSet<>();
            if (rows != null) {
                for (String csv : rows) {
                    if (StringUtil.isBlank(csv)) {
                        continue;
                    }
                    for (String part : csv.split(",")) {
                        String t = part.trim();
                        if (!t.isEmpty()) {
                            try {
                                ids.add(Long.parseLong(t));
                            } catch (NumberFormatException ignored) {
                                // 忽略非数字片段
                            }
                        }
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("[formmode] 解析用户岗位失败，按无岗位处理. userId={}, err={}", userId, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 部门授权命中判定：精确匹配或（includesub=1 时）展开子孙部门。
     *
     * @param objId        授权的部门 ID
     * @param deptIds      当前用户所属部门集合
     * @param includeSub   是否包含子部门（1=是）
     */
    public boolean matchDept(Long objId, Set<Long> deptIds, Integer includeSub) {
        if (objId == null || deptIds == null || deptIds.isEmpty()) {
            return false;
        }
        if (deptIds.contains(objId)) {
            return true;
        }
        if (!Integer.valueOf(1).equals(includeSub)) {
            return false;
        }
        for (Long deptId : deptIds) {
            if (deptId != null && descendantDeptIds(deptId).contains(objId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前用户的部门集合（若 needSub=true 则含全部子孙部门），用于「数据范围」等需要整体集合的场景。
     */
    public Set<Long> resolveDeptIds(Set<Long> ownDeptIds, boolean needSub) {
        Set<Long> result = new LinkedHashSet<>();
        if (ownDeptIds == null) {
            return result;
        }
        result.addAll(ownDeptIds);
        if (needSub) {
            for (Long deptId : ownDeptIds) {
                if (deptId != null) {
                    result.addAll(descendantDeptIds(deptId));
                }
            }
        }
        return result;
    }

    /**
     * 查询某部门的全部子孙部门 id（两端补逗号匹配 ancestors，避免漏直属子部门 / 误判前缀相同的部门）。
     */
    public List<Long> descendantDeptIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM blade_dept WHERE is_deleted = 0 AND CONCAT(',', ancestors, ',') LIKE ?",
                "%," + deptId + ",%");
            List<Long> ids = new ArrayList<>();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object id = row.get("id");
                    if (id instanceof Number) {
                        ids.add(((Number) id).longValue());
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("[formmode] 查询子孙部门失败，仅按本部门匹配. deptId={}, err={}", deptId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 逗号分隔字符串 → Long 集合（句柄级复用） */
    public static Set<Long> parseIds(String csv) {
        Set<Long> set = new HashSet<>();
        if (StringUtil.isBlank(csv)) {
            return set;
        }
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                try {
                    set.add(Long.parseLong(t));
                } catch (NumberFormatException ignored) {
                    // 忽略非数字片段
                }
            }
        }
        return set;
    }
}
