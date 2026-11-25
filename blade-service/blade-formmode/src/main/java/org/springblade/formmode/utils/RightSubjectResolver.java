package org.springblade.formmode.utils;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 建模权限「授权主体」统一解析器。
 *
 * <p>把此前散落在 {@code FormRightServiceImpl} / {@code ModeDataShareRuleServiceImpl}
 * 中「未实现」的三个维度收敛到一处，供模块权限 / 字段权限 / 数据范围共用：</p>
 * <ul>
 *   <li><b>管理员豁免</b>：真实角色 ID 命中 administrator/admin → 拥有全部权限；
 *       不信任 token 中可能残留的 roleName；</li>
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

    /**
     * 密级轴开关（默认 false）。
     *
     * <p>密级需在 blade_user.security_level 初始化后才生效；未初始化时开启会导致
     * 所有人密级=0，showlevel>0 的规则谁都命不中。业务定义密级标准后再开启。</p>
     */
    @Value("${blade.formmode.seclevel.enforce:false}")
    private boolean seclevelEnforce;

    public RightSubjectResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 密级轴是否启用 */
    public boolean isSeclevelEnabled() {
        return seclevelEnforce;
    }

    /**
     * 解析当前用户密级（blade_user.security_level，0~100）。
     *
     * <p>运行时查库（BladeUser 外部 jar 无 seclevel claim）。开关关闭或未查到 → 返回 null（不参与过滤）。</p>
     */
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "subject:seclevel", key = "#userId",
        unless = "#result == null")
    public Integer resolveSeclevel(Long userId) {
        if (!seclevelEnforce || userId == null) {
            return null;
        }
        try {
            List<Integer> levels = jdbcTemplate.queryForList(
                "SELECT security_level FROM blade_user WHERE id = ? AND is_deleted = 0", Integer.class, userId);
            return (levels == null || levels.isEmpty()) ? null : levels.get(0);
        } catch (Exception e) {
            log.warn("[formmode] 查询用户密级失败，按不参与过滤处理. userId={}, err={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 密级区间判定：showlevel ≤ userSeclevel ≤ ISNULL(showlevel2, 9999)。
     *
     * @return 开关关闭 / 规则未配密级 / 区间命中 → true；不命中 → false（fail-closed）
     */
    public boolean seclevelMatches(Integer showlevel, Integer showlevel2, Integer userSeclevel) {
        if (!seclevelEnforce) {
            return true; // 开关关闭：密级轴不参与
        }
        if (showlevel == null && showlevel2 == null) {
            return true; // 规则未配密级：不参与
        }
        if (userSeclevel == null) {
            return false; // 启用了但用户无密级：拒绝（fail-closed）
        }
        int lower = showlevel == null ? 0 : showlevel;
        int upper = showlevel2 == null ? 9999 : showlevel2;
        return userSeclevel >= lower && userSeclevel <= upper;
    }

    /**
     * 当前用户是否为管理员。
     *
     * <p>只认真实角色 ID：token 的 roleName 可能保留历史角色名（例如 yjl 的 role_id 为空，
     * 但 roleName 仍含 administrator/admin），如果继续按角色名放行，会导致字段权限/
     * 数据范围被错误跳过。roleId 为空时再查一次 blade_user，避免 token 裁剪导致误判。</p>
     */
    public boolean isAdmin(BladeUser user) {
        if (user == null) return false;
        Set<Long> roleIds = parseRoleIds(user.getRoleId());
        if (roleIds.isEmpty() && user.getUserId() != null) {
            try {
                List<String> rows = jdbcTemplate.queryForList(
                    "SELECT role_id FROM blade_user WHERE id=? AND is_deleted=0", String.class, user.getUserId());
                if (rows != null && !rows.isEmpty()) {
                    roleIds = parseRoleIds(rows.get(0));
                }
            } catch (Exception e) {
                log.warn("[formmode] 管理员角色反查失败，按非管理员处理. userId={}, err={}", user.getUserId(), e.getMessage());
                return false;
            }
        }
        if (roleIds.isEmpty()) return false;
        String marks = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<String> aliases = jdbcTemplate.queryForList(
                "SELECT role_alias FROM blade_role WHERE is_deleted=0 AND id IN (" + marks + ")", String.class);
            return aliases != null && aliases.stream().anyMatch(a ->
                RoleConstant.ADMIN.equalsIgnoreCase(a) || "admin".equalsIgnoreCase(a));
        } catch (Exception e) {
            log.warn("[formmode] 管理员角色校验失败，按非管理员处理. roleIds={}, err={}", marks, e.getMessage());
            return false;
        }
    }

    private Set<Long> parseRoleIds(String csv) {
        Set<Long> ids = new HashSet<>();
        if (StringUtil.isBlank(csv)) return ids;
        for (String part : csv.split(",")) {
            String v = part == null ? null : part.trim();
            if (v == null || v.isEmpty()) continue;
            try {
                ids.add(Long.valueOf(v));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /**
     * 解析当前用户的岗位 ID 集合（blade_user.post_id，逗号分隔多值）。
     *
     * <p>BladeUser 未承载 postId，故按 userId 直接查库；查不到 / 异常返回空集合。</p>
     */
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "subject:post", key = "#userId")
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
     * 解析当前用户的分部 ID 集合（blade_dept.subcompany_id，按用户所属部门反查）。
     *
     * <p>对标 E9 rolelevel=1（分部级）授权主体；用户无部门 / 部门无分部时返回空集合。</p>
     */
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "subject:subcompany", key = "#userId")
    public Set<Long> resolveSubcompanyIds(Long userId) {
        Set<Long> result = new java.util.LinkedHashSet<>();
        if (userId == null) {
            return result;
        }
        try {
            List<String> deptCsvs = jdbcTemplate.queryForList(
                "SELECT dept_id FROM blade_user WHERE id = ? AND is_deleted = 0", String.class, userId);
            if (deptCsvs == null) return result;
            for (String csv : deptCsvs) {
                for (String d : StringUtil.isBlank(csv) ? new String[0] : csv.split(",")) {
                    String t = d.trim();
                    if (t.isEmpty()) continue;
                    try {
                        List<Long> subIds = jdbcTemplate.queryForList(
                            "SELECT subcompany_id FROM blade_dept WHERE id = ? AND is_deleted = 0 AND subcompany_id IS NOT NULL",
                            Long.class, Long.parseLong(t));
                        if (subIds != null) result.addAll(subIds);
                    } catch (NumberFormatException ignored) { }
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 解析用户分部失败，按无分部处理. userId={}, err={}", userId, e.getMessage());
        }
        return result;
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
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "subject:dept-descendants", key = "#deptId")
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
