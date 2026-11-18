package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.mapper.ModeDataShareRuleMapper;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.utils.MatrixSubjectResolver;
import org.springblade.formmode.utils.RightSubjectResolver;
import org.springblade.formmode.vo.DataScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 行级数据范围解析实现（对标 ecology modedatasharerule，精简子集）。
 *
 * <p>2026-11-17 补齐：① 管理员全量豁免；② 部门 includeSub 子部门展开；
 * ③ 岗位 objtype=3 解析（此前 jobIds 虽传入但仅在精确匹配分支使用，现完整支持）；
 * ④ creatorSelf 语义（共享规则命中时仍可强制「仅本人创建」）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeDataShareRuleServiceImpl implements IDataScopeService {

    private final ModeDataShareRuleMapper mapper;
    private final RightSubjectResolver subjectResolver;
    private final MatrixSubjectResolver matrixSubjectResolver;
    private final JdbcTemplate jdbcTemplate;

    /** 查看权限类型（modedatasharerule.type=1） */
    private static final int TYPE_VIEW = 1;
    /** 维护权限类型（modedatasharerule.type=2）：命中者可编辑/删除他人数据 */
    private static final int TYPE_MAINTAIN = 2;
    /**
     * 授权对象类型（2026-10-20 已与 moderightinfo / modefieldauthorize 统一）：
     * <b>1角色 2部门 3岗位 4人员</b>。
     *
     * <p>迁移前本表用 1人员 2部门 3角色 4岗位；存量数据由
     * {@code V2026.10.20_005} 脚本完成值域映射，二者必须同步上线。</p>
     */
    private static final int OBJ_ROLE = 1;
    private static final int OBJ_DEPT = 2;
    private static final int OBJ_JOB = 3;
    private static final int OBJ_PERSON = 4;
    /** 分部主体（对标 E9 rolelevel=1；SB 扩展值，非 E9 原生编码） */
    private static final int OBJ_SUBCOMPANY = 5;
    /** 矩阵主体（SB 扩展值；数据范围层按「当前用户部门」为条件值解析，非行字段） */
    private static final int OBJ_MATRIX = 7;

    /** 当前租户 ID（全局租户插件未生效，显式隔离；取不到回退默认租户） */
    private static String currentTenant() {
        return Func.toStr(SecureUtil.getTenantId(), "000000");
    }

    @Override
    public DataScope resolve(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds) {
        DataScope scope = new DataScope();
        scope.setCanViewAll(false);
        scope.setCreatorId(userId);
        if (modeId == null || userId == null) {
            // 防御：无模块/用户 → 仅 creator 自阅（creatorId 可能为 null，则实际全不可见）
            return scope;
        }

        // 管理员豁免：可见全部数据（与 checkRight / DataScopeValueResolver 同源）
        BladeUser user = AuthUtil.getUser();
        if (subjectResolver.isAdmin(user)) {
            scope.setCanViewAll(true);
            return scope;
        }

        List<ModeDataShareRule> rules;
        try {
            String tenant = currentTenant();
            rules = mapper.selectList(new LambdaQueryWrapper<ModeDataShareRule>()
                .eq(ModeDataShareRule::getModeid, modeId)
                .eq(ModeDataShareRule::getType, TYPE_VIEW)
                .and(w -> w.eq(ModeDataShareRule::getTenantId, tenant).or().isNull(ModeDataShareRule::getTenantId)));
        } catch (Exception e) {
            log.warn("[formmode] 数据范围规则加载失败，按仅 creator 自阅兜底. modeId={}, err={}",
                modeId, e.getMessage());
            return scope;
        }
        // 部门集合：优先用当前登录用户的全部部门（支持多人多部门 + includeSub 展开），
        // 入参 deptId 仅作为兜底（Feign/单测无登录态时）。
        Set<Long> deptIdSet = currentDeptIds(deptId);
        Set<Long> jobIdSet = jobIds == null ? Set.of() : Set.copyOf(jobIds);
        // 规则按 id 升序加载（见 listRules），命中多条时取第一条：与 E9「allow 并集」方向一致
        // （任一规则命中即可见），但条件化授权只采用首条命中的条件——配置时应避免多条带条件的规则并存，
        // 否则后续规则的条件不生效。这是刻意简化：多条件 OR/AND 组合语义需业务确认后再扩展。
        for (ModeDataShareRule r : rules) {
            boolean matrixHit = Integer.valueOf(OBJ_MATRIX).equals(r.getObjtype())
                && matrixRuleHit(r, userId, deptIdSet);
            if (matrixHit || matchRule(r, userId, deptIdSet, roleIds, jobIdSet, subjectResolver)) {
                // 密级轴：showlevel ≤ 用户密级 ≤ showlevel2（开关关闭/未配密级时不参与）
                if (!subjectResolver.seclevelMatches(r.getShowlevel(), r.getShowlevel2(),
                    subjectResolver.resolveSeclevel(userId))) {
                    continue;
                }
                // creatorSelf=1：即便命中共享规则也强制「仅本人创建」
                if (Integer.valueOf(1).equals(r.getCreatorSelf())) {
                    scope.setCanViewAll(false);
                    scope.setCreatorId(userId);
                } else {
                    scope.setCanViewAll(true);
                }
                // 条件化授权：命中主体规则后，行还需满足结构化条件（字段/操作/值齐全才生效）
                if (Integer.valueOf(1).equals(r.getConditionEnabled())
                    && r.getConditionField() != null && !r.getConditionField().isBlank()
                    && r.getConditionOp() != null && !r.getConditionOp().isBlank()
                    && r.getConditionValue() != null) {
                    DataScope.RowCondition cond = new DataScope.RowCondition();
                    cond.setField(r.getConditionField());
                    cond.setOp(r.getConditionOp());
                    cond.setValue(r.getConditionValue());
                    scope.setRowCondition(cond);
                }
                return scope;
            }
        }
        return scope;
    }

    @Override
    public boolean inScope(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds,
                           Long creatorIdOfRow) {
        DataScope scope = resolve(modeId, userId, deptId, roleIds, jobIds);
        if (scope.isCanViewAll()) {
            return true;
        }
        return Objects.equals(creatorIdOfRow, scope.getCreatorId());
    }

    @Override
    public boolean canMaintainAll(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds) {
        if (modeId == null || userId == null) {
            return false;
        }
        // 管理员：可维护全部（与 resolve 同源）
        if (subjectResolver.isAdmin(AuthUtil.getUser())) {
            return true;
        }
        try {
            List<ModeDataShareRule> rules = mapper.selectList(new LambdaQueryWrapper<ModeDataShareRule>()
                .eq(ModeDataShareRule::getModeid, modeId)
                .eq(ModeDataShareRule::getType, TYPE_MAINTAIN)
                .and(w -> w.eq(ModeDataShareRule::getTenantId, currentTenant()).or().isNull(ModeDataShareRule::getTenantId)));
            if (rules == null || rules.isEmpty()) {
                return false; // 未配维护档 → 只能维护本人
            }
            Set<Long> deptIdSet = currentDeptIds(deptId);
            Set<Long> jobIdSet = jobIds == null ? Set.of() : Set.copyOf(jobIds);
            for (ModeDataShareRule r : rules) {
                boolean matrixHit = Integer.valueOf(OBJ_MATRIX).equals(r.getObjtype())
                    && matrixRuleHit(r, userId, deptIdSet);
                if ((matrixHit || matchRule(r, userId, deptIdSet, roleIds, jobIdSet, subjectResolver))
                    && subjectResolver.seclevelMatches(r.getShowlevel(), r.getShowlevel2(),
                        subjectResolver.resolveSeclevel(userId))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 维护档规则判定失败，按不可维护他人数据处理. modeId={}, err={}",
                modeId, e.getMessage());
        }
        return false;
    }

    /**
     * 单条规则是否命中（纯函数 + 可注入的子部门解析器，便于单测）。
     *
     * @param deptIds   当前用户部门集合（单部门场景为单元素集合）
     * @param jobIds    当前用户岗位集合
     * @param resolver  子部门解析器；为 null 时 includeSub 不展开（兼容旧调用/单测）
     */
    public static boolean matchRule(ModeDataShareRule r, Long userId, Set<Long> deptIds,
                                    List<Long> roleIds, Set<Long> jobIds, RightSubjectResolver resolver) {
        if (r == null || r.getObjtype() == null || r.getObjid() == null) {
            return false;
        }
        Long objId = r.getObjid();
        switch (r.getObjtype()) {
            case OBJ_PERSON:
                return Objects.equals(objId, userId);
            case OBJ_DEPT:
                if (resolver != null) {
                    return resolver.matchDept(objId, deptIds, r.getIncludeSub());
                }
                return deptIds != null && deptIds.contains(objId);
            case OBJ_ROLE:
                return roleIds != null && roleIds.contains(objId);
            case OBJ_JOB:
                return jobIds != null && jobIds.contains(objId);
            case OBJ_SUBCOMPANY:
                return resolver != null && userId != null
                    && resolver.resolveSubcompanyIds(userId).contains(objId);
            default:
                return false;
        }
    }

    /** 兼容旧签名（无子部门展开），供既有单测调用 */
    public static boolean matchRule(ModeDataShareRule r, Long userId, Long deptId,
                                    List<Long> roleIds, List<Long> jobIds) {
        return matchRule(r, userId, deptId == null ? Set.of() : Set.of(deptId),
            roleIds, jobIds == null ? Set.of() : Set.copyOf(jobIds), null);
    }

    /** 当前登录用户的全部部门 ID；无登录态时回退到入参 deptId */
    private Set<Long> currentDeptIds(Long fallbackDeptId) {
        BladeUser user = AuthUtil.getUser();
        Set<Long> ids = user == null ? Set.of() : RightSubjectResolver.parseIds(user.getDeptId());
        if (ids.isEmpty() && fallbackDeptId != null) {
            return Set.of(fallbackDeptId);
        }
        return ids;
    }

    /**
     * 数据范围层的矩阵主体判定（静态口径）。
     *
     * <p>数据范围规则是模块级、无 dataId 行上下文，故用「当前用户所属部门 ID」作为条件值
     * 构造虚拟行，按 conditionField 作为 key 交给矩阵解析器。真正的「按数据行字段值解析」
     * 在单条共享（modedatashare_single, objtype=7）中实现。</p>
     */
    private boolean matrixRuleHit(ModeDataShareRule r, Long userId, Set<Long> deptIdSet) {
        if (r.getObjid() == null || r.getConditionField() == null || r.getConditionField().isBlank()
            || deptIdSet == null || deptIdSet.isEmpty()) {
            return false;
        }
        try {
            for (Long deptId : deptIdSet) {
                Map<String, Object> virtualRow = new HashMap<>();
                virtualRow.put(r.getConditionField().trim().toLowerCase(), deptId);
                if (matrixSubjectResolver.matches(r.getObjid(), r.getConditionField(),
                    r.getMatrixValueField(), virtualRow, userId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 数据范围矩阵判定失败，按无权限. ruleId={}, err={}", r.getId(), e.getMessage());
        }
        return false;
    }

    @Override
    public List<ModeDataShareRule> listRules(Long modeId) {
        if (modeId == null) {
            return new ArrayList<>();
        }
        try {
            String tenant = currentTenant();
            return mapper.selectList(new LambdaQueryWrapper<ModeDataShareRule>()
                .eq(ModeDataShareRule::getModeid, modeId)
                .and(w -> w.eq(ModeDataShareRule::getTenantId, tenant).or().isNull(ModeDataShareRule::getTenantId))
                .orderByAsc(ModeDataShareRule::getId));
        } catch (Exception e) {
            log.warn("[formmode] 查询数据范围规则失败. modeId={}, err={}", modeId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean saveRules(Long modeId, List<ModeDataShareRule> rules) {
        if (modeId == null) {
            return false;
        }
        try {
            String tenant = currentTenant();
            mapper.delete(new LambdaQueryWrapper<ModeDataShareRule>()
                .eq(ModeDataShareRule::getModeid, modeId)
                .eq(ModeDataShareRule::getTenantId, tenant));
            if (rules != null) {
                for (ModeDataShareRule r : rules) {
                    if (r == null) {
                        continue;
                    }
                    r.setModeid(modeId);
                    r.setTenantId(tenant); // 服务端强制写入当前租户，忽略前端传入
                    r.setId(null); // 由 @TableId(ASSIGN_ID) 重新生成，避免主键冲突
                    mapper.insert(r);
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("[formmode] 保存数据范围规则失败. modeId={}, err={}", modeId, e.getMessage());
            return false;
        }
    }
}
