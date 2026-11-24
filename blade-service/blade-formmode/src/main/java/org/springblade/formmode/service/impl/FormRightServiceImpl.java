package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.entity.ModeDataShareSingle;
import org.springblade.formmode.mapper.ModeDataShareSingleMapper;
import org.springblade.formmode.mapper.ModeFieldAuthorizeMapper;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.ModeRightInfoMapper;
import org.springblade.formmode.mapper.WorkflowBillFieldMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.utils.RightSubjectResolver;
import org.springblade.formmode.vo.DataScope;
import org.springblade.formmode.vo.FieldPermVO;
import org.springblade.formmode.vo.RuntimeFieldPermVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 表单权限服务实现
 *
 * <p>对应 ecology 的 ModeRightService（79KB核心权限服务）。</p>
 *
 * <p><b>字段权限源优先级链</b>（渲染端必须遵守，详见
 * {@code springBlade/doc/md/字段权限优先级链.md}）：</p>
 * <pre>
 * wf_node_field_perm（节点级，blade-workflow）
 *   &gt; modefieldauthorize（角色/部门/岗位/人员级，formmode）
 *   &gt; fieldAttr（字段定义默认，workflow_billfield）
 *   &gt; Excel 单元格样式
 *   &gt; 默认
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormRightServiceImpl implements IFormRightService {

    private final ModeRightInfoMapper modeRightInfoMapper;
    private final ModeFieldAuthorizeMapper modeFieldAuthorizeMapper;
    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final IDataScopeService dataScopeService;
    private final RightSubjectResolver subjectResolver;
    private final ModeDataShareSingleMapper modeDataShareSingleMapper;
    private final StringRedisTemplate redisTemplate;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 字段权限查询异常时是否 fail-closed（默认 false）。
     *
     * <p>false（默认）：异常返回空列表 → 调用方按字段定义默认渲染，避免权限组件故障导致表单全空白；
     * true：异常返回「该模块全部字段隐藏」列表 → 安全优先，但权限组件故障会导致表单不可见。</p>
     */
    @Value("${blade.formmode.field-perm.fail-closed:false}")
    private boolean fieldPermFailClosed;

    /**
     * 维护档严格模式（默认 false，保持兼容）。
     *
     * <p>false：查看全量即可编辑全量（既有语义）；
     * true：查看全量后还需命中「维护」档(type=2)规则才可编辑他人数据（E9 语义）。</p>
     */
    @Value("${blade.formmode.maintain-scope.strict:false}")
    private boolean maintainScopeStrict;

    /** 安全档位：strict 时强制 fail-closed 开关为 true；compat（默认）保持兼容 */
    @Value("${blade.formmode.security.profile:compat}")
    private String securityProfile;

    @jakarta.annotation.PostConstruct
    public void applySecurityProfile() {
        if ("strict".equalsIgnoreCase(securityProfile)) {
            fieldPermFailClosed = true;
            maintainScopeStrict = true;
        }
    }

    /** 权限配置版本号 Redis key 前缀（按 modeId 隔离） */
    private static final String RIGHT_VERSION_KEY = "formmode:right:version:";

    /** 约定：objid=0 表示「全部/所有人」 */
    private static final long OBJ_ID_ALL = 0L;

    /**
     * 当前租户 ID（用于权限三表隔离）。
     *
     * <p>已实证：全局租户插件未生效（tenant=000000 可读到 tenant=999999 的配置），
     * 故在 formmode 权限查询中显式加 tenant_id 条件；取不到时回退默认租户 000000，
     * 与三表 tenant_id 默认值一致，兼容存量数据。</p>
     */
    public static String currentTenant() {
        return Func.toStr(SecureUtil.getTenantId(), "000000");
    }

    @Override
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "formmode:user-right-types",
        key = "T(org.springblade.formmode.service.impl.FormRightServiceImpl).currentTenant() + ':' + #modeId + ':' + #userId")
    public List<Integer> listUserRightTypes(Long modeId, Long userId) {
        List<Integer> result = new ArrayList<>();
        if (modeId == null) {
            return result;
        }
        BladeUser user = AuthUtil.getUser();
        // 管理员：拥有全部操作（与 checkRight 豁免口径一致）
        if (subjectResolver.isAdmin(user)) {
            result.addAll(java.util.Arrays.asList(1, 2, 3, 4, 5));
            return result;
        }
        try {
            // 只取「明确配置且当前用户命中」的权限码；未配置的 righttype 不返回
            // （与 checkRight 的 fail-open 语义不同：按钮驱动需要"显式授权"口径，否则收窄失效）
            List<ModeRightInfo> rights = modeRightInfoMapper.selectList(
                new LambdaQueryWrapper<ModeRightInfo>()
                    .eq(ModeRightInfo::getModeid, modeId)
                    .in(ModeRightInfo::getRighttype, java.util.Arrays.asList(1, 2, 3, 4, 5))
                    .and(w -> w.eq(ModeRightInfo::getTenantId, currentTenant()).or().isNull(ModeRightInfo::getTenantId))
            );
            if (rights == null || rights.isEmpty()) {
                return result; // 未配置 → 空，前端 fail-open
            }
            Long uid = (userId != null) ? userId : (user != null ? user.getUserId() : null);
            Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
            Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
            Set<Long> postIds = subjectResolver.resolvePostIds(uid);
            Set<Long> subcompanyIds = subjectResolver.resolveSubcompanyIds(uid);
            Set<Integer> hit = new java.util.LinkedHashSet<>();
            for (ModeRightInfo r : rights) {
                Integer rt = r.getRighttype();
                if (rt == null || hit.contains(rt)) continue;
                if (matchModeRightSubject(r, uid, roleIds, deptIds, postIds, subcompanyIds)) {
                    hit.add(rt);
                }
            }
            result.addAll(hit);
            java.util.Collections.sort(result);
        } catch (Exception e) {
            log.warn("[formmode] 查询用户模块权限码失败. modeId={}, err={}", modeId, e.getMessage());
            return new ArrayList<>(); // 失败空 → 前端 fail-open
        }
        return result;
    }

    @Override
    public boolean hasModelRightConfig(Long modeId) {
        if (modeId == null) return false;
        try {
            Long r = modeRightInfoMapper.selectCount(new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId));
            if (r != null && r > 0) return true;
            Long f = modeFieldAuthorizeMapper.selectCount(new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId));
            if (f != null && f > 0) return true;
            Long s = dataScopeService.countRules(modeId);
            return s != null && s > 0;
        } catch (Exception e) {
            log.warn("[formmode] 判定建模权限配置失败，按「未配置」处理(回退数据权限). modeId={}, err={}", modeId, e.getMessage());
            return false; // 异常回退数据权限（数据权限侧 fail-closed 兜底）
        }
    }

    /** 主体匹配（供 listUserRightTypes 使用；口径与 checkRight 的 switch 一致） */
    private boolean matchModeRightSubject(ModeRightInfo r, Long uid, Set<Long> roleIds,
                                          Set<Long> deptIds, Set<Long> postIds, Set<Long> subcompanyIds) {
        Integer objType = r.getObjtype();
        Long objId = r.getObjid();
        if (objType == null || objId == null) return false;
        long oid = objId;
        if (oid == OBJ_ID_ALL) return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
        boolean hit = switch (objType) {
            case 4 -> uid != null && uid == oid;
            case 1 -> roleIds.contains(oid);
            case 2 -> subjectResolver.matchDept(oid, deptIds, r.getIncludesub());
            case 3 -> postIds.contains(oid);
            case 5 -> subcompanyIds.contains(oid);
            default -> false;
        };
        return hit && seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
    }

    /** 密级判定：showlevel ≤ 用户密级 ≤ showlevel2（开关关闭/未配密级时不参与） */
    private boolean seclevelAllows(Integer showlevel, Integer showlevel2, Long userId) {
        if (!subjectResolver.isSeclevelEnabled() || (showlevel == null && showlevel2 == null)) {
            return true;
        }
        return subjectResolver.seclevelMatches(showlevel, showlevel2, subjectResolver.resolveSeclevel(userId));
    }

    @Override
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "formmode:right-check",
        key = "T(org.springblade.formmode.service.impl.FormRightServiceImpl).currentTenant() + ':' + #modeId + ':' + #rightType + ':' + #userId",
        unless = "#result == null")
    public boolean checkRight(Long modeId, Integer rightType, Long userId) {
        String tenant = currentTenant();
        List<ModeRightInfo> rights = modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
                .eq(ModeRightInfo::getRighttype, rightType)
                .and(w -> w.eq(ModeRightInfo::getTenantId, tenant).or().isNull(ModeRightInfo::getTenantId))
        );
        // 该 righttype 未配置：
        // - 整个模块都没配任何操作权限 → 默认放行（兼容语义）
        // - 模块已配其它操作权限但该操作未配 → strict 档位拒绝（与 listUserRightTypes 显式授权口径一致，
        //   避免"前端隐藏编辑按钮但直连 update 接口可编辑"）；compat 档位仍放行
        if (rights == null || rights.isEmpty()) {
            if (!fieldPermFailClosed && !maintainScopeStrict) {
                return true; // compat：保持既有语义
            }
            try {
                Long any = modeRightInfoMapper.selectCount(new LambdaQueryWrapper<ModeRightInfo>()
                    .eq(ModeRightInfo::getModeid, modeId)
                    .and(w -> w.eq(ModeRightInfo::getTenantId, tenant).or().isNull(ModeRightInfo::getTenantId)));
                return any == null || any == 0; // 模块有其它权限配置 → 拒绝
            } catch (Exception e) {
                log.warn("[formmode] 模块权限存在性判定失败. modeId={}, err={}", modeId, e.getMessage());
                return false; // strict 语义下异常拒绝
            }
        }

        // 解析当前用户授权维度（角色/部门来自登录态 BladeUser；岗位另查 blade_user.post_id）
        BladeUser user = AuthUtil.getUser();
        // 管理员豁免：与 DataScopeValueResolver 同源，拥有全部模块权限
        if (subjectResolver.isAdmin(user)) {
            return true;
        }
        Long uid = (userId != null) ? userId : (user != null ? user.getUserId() : null);
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
        Set<Long> postIds = subjectResolver.resolvePostIds(uid);
        Set<Long> subcompanyIds = subjectResolver.resolveSubcompanyIds(uid);

        for (ModeRightInfo r : rights) {
            Integer objType = r.getObjtype();
            Long objId = (r.getObjid() == null) ? null : r.getObjid().longValue();
            if (objType == null || objId == null) {
                continue;
            }
            // 0 约定为「全部」
            if (objId == OBJ_ID_ALL) {
                return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
            }
            switch (objType) {
                case 4: // 人员
                    if (uid != null && uid.equals(objId)) {
                        return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
                    }
                    break;
                case 1: // 角色
                    if (roleIds.contains(objId)) {
                        return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
                    }
                    break;
                case 2: // 部门：includesub=1 时展开子孙部门
                    if (subjectResolver.matchDept(objId, deptIds, r.getIncludesub())) {
                        return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
                    }
                    break;
                case 3: // 岗位：由 blade_user.post_id 解析
                    if (postIds.contains(objId)) {
                        return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
                    }
                    break;
                case 5: // 分部：由用户部门反查 blade_dept.subcompany_id（对标 E9 rolelevel=1）
                    if (subcompanyIds.contains(objId)) {
                        return seclevelAllows(r.getShowlevel(), r.getShowlevel2(), uid);
                    }
                    break;
                default:
                    break;
            }
        }
        // 已配置权限但当前用户未命中任何一条 → 无权限
        return false;
    }

    /**
     * 解析逗号分隔的 ID 串（如 token 中的 roleId/deptId/postId）
     */
    private static Set<Long> parseIds(String csv) {
        Set<Long> set = new HashSet<>();
        if (csv == null || csv.isBlank()) {
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

    @Override
    public List<ModeRightInfo> getModeRights(Long modeId) {
        String tenant = currentTenant();
        return modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
                .and(w -> w.eq(ModeRightInfo::getTenantId, tenant).or().isNull(ModeRightInfo::getTenantId))
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public boolean saveModeRights(Long modeId, List<ModeRightInfo> rights) {
        String tenant = currentTenant();
        // 删除旧权限（限定当前租户，避免误删异租户配置）
        modeRightInfoMapper.delete(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
                .eq(ModeRightInfo::getTenantId, tenant)
        );
        // 插入新的权限（tenant_id 由服务端强制写入，忽略前端传入）
        if (rights != null) {
            for (ModeRightInfo right : rights) {
                right.setModeid(modeId);
                right.setTenantId(tenant);
                modeRightInfoMapper.insert(right);
            }
        }
        return true;
    }

    @Override
    public List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId) {
        String tenant = currentTenant();
        return modeFieldAuthorizeMapper.selectList(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId)
                .and(w -> w.eq(ModeFieldAuthorize::getTenantId, tenant).or().isNull(ModeFieldAuthorize::getTenantId))
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes) {
        String tenant = currentTenant();
        modeFieldAuthorizeMapper.delete(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId)
                .eq(ModeFieldAuthorize::getTenantId, tenant)
        );
        if (authorizes != null) {
            for (ModeFieldAuthorize auth : authorizes) {
                auth.setModeid(modeId);
                auth.setTenantId(tenant);
                modeFieldAuthorizeMapper.insert(auth);
            }
        }
        return true;
    }

    @Override
    public List<ModeFieldAuthorize> getFieldAuthorizesBySubject(Integer objtype, Long objid) {
        if (objtype == null || objid == null) return new ArrayList<>();
        String tenant = currentTenant();
        try {
            return modeFieldAuthorizeMapper.selectList(new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getObjtype, objtype)
                .eq(ModeFieldAuthorize::getObjid, objid)
                .and(w -> w.eq(ModeFieldAuthorize::getTenantId, tenant).or().isNull(ModeFieldAuthorize::getTenantId))
                .orderByAsc(ModeFieldAuthorize::getModeid, ModeFieldAuthorize::getFieldid));
        } catch (Exception e) {
            log.warn("[formmode] 按主体查询字段权限失败. objtype={}, objid={}, err={}", objtype, objid, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public boolean saveFieldAuthorizesBySubject(org.springblade.formmode.dto.FieldSubjectSaveDTO batch) {
        if (batch == null || batch.getObjtype() == null || org.springblade.core.tool.utils.Func.isBlank(batch.getObjid())
            || batch.getModes() == null || batch.getModes().isEmpty()) {
            throw new ServiceException("按主体保存字段权限参数不完整");
        }
        long objId;
        try {
            objId = Long.parseLong(String.valueOf(batch.getObjid()).trim());
        } catch (Exception e) {
            throw new ServiceException("授权对象ID非法");
        }
        String tenant = currentTenant();
        // 1) 解析 modeId 与版本校验
        java.util.List<Long> modeIds = new ArrayList<>();
        java.util.Set<String> modeIdSet = new java.util.LinkedHashSet<>();
        for (org.springblade.formmode.dto.FieldSubjectModeDTO m : batch.getModes()) {
            if (m == null || org.springblade.core.tool.utils.Func.isBlank(m.getModeId())) continue;
            long modeId;
            try { modeId = Long.parseLong(String.valueOf(m.getModeId()).trim()); } catch (Exception e) { continue; }
            if (modeIdSet.add(String.valueOf(modeId))) modeIds.add(modeId);
            Long expected = batch.getModeVersions() == null ? null : batch.getModeVersions().get(String.valueOf(modeId));
            if (expected != null) {
                long current = getRightVersion(modeId);
                if (current != expected) {
                    throw new ServiceException("权限配置已被他人修改，请刷新后重试（modeId=" + modeId + "）");
                }
            }
        }
        if (modeIds.isEmpty()) throw new ServiceException("未提交任何有效模块");
        // 2) 查旧记录（只限当前主体 + 本次模块）
        List<ModeFieldAuthorize> olds = modeFieldAuthorizeMapper.selectList(new LambdaQueryWrapper<ModeFieldAuthorize>()
            .eq(ModeFieldAuthorize::getObjtype, batch.getObjtype())
            .eq(ModeFieldAuthorize::getObjid, objId)
            .in(ModeFieldAuthorize::getModeid, modeIds)
            .eq(ModeFieldAuthorize::getTenantId, tenant));
        java.util.Map<String, ModeFieldAuthorize> oldMap = new java.util.HashMap<>();
        for (ModeFieldAuthorize old : olds) {
            oldMap.put(old.getModeid() + "_" + old.getFieldid(), old);
        }
        java.util.Set<String> keep = new java.util.HashSet<>();
        int changed = 0;
        // 3) upsert 提交项
        for (org.springblade.formmode.dto.FieldSubjectModeDTO m : batch.getModes()) {
            if (m == null || org.springblade.core.tool.utils.Func.isBlank(m.getModeId())) continue;
            long modeId;
            try { modeId = Long.parseLong(String.valueOf(m.getModeId()).trim()); } catch (Exception e) { continue; }
            if (m.getFields() == null) continue;
            for (org.springblade.formmode.dto.FieldSubjectItemDTO f : m.getFields()) {
                if (f == null || f.getFieldId() == null || f.getRightvalue() == null) continue;
                int rv = f.getRightvalue();
                if (rv < 0 || rv > 2) continue;
                String key = modeId + "_" + f.getFieldId();
                keep.add(key);
                ModeFieldAuthorize exist = oldMap.get(key);
                if (exist != null) {
                    if (!Integer.valueOf(rv).equals(exist.getRightvalue())) {
                        exist.setRightvalue(rv);
                        modeFieldAuthorizeMapper.updateById(exist);
                        changed++;
                    }
                } else {
                    ModeFieldAuthorize add = new ModeFieldAuthorize();
                    add.setModeid(modeId);
                    add.setFieldid(f.getFieldId().intValue());
                    add.setObjtype(batch.getObjtype());
                    add.setObjid(objId);
                    add.setIncludesub(0);
                    add.setRightvalue(rv);
                    add.setTenantId(tenant);
                    modeFieldAuthorizeMapper.insert(add);
                    changed++;
                }
            }
        }
        // 4) 删除该主体在这些模块下未提交字段
        for (ModeFieldAuthorize old : olds) {
            String key = old.getModeid() + "_" + old.getFieldid();
            if (!keep.contains(key)) {
                modeFieldAuthorizeMapper.deleteById(old.getId());
                changed++;
            }
        }
        // 5) 版本推进 + 审计
        for (Long modeId : modeIds) bumpRightVersion(modeId);
        BladeUser auditUser = AuthUtil.getUser();
        log.info("[formmode][AUDIT] 按主体字段权限保存. objtype={}, objid={}, modes={}, changed={}, operator={}({})",
            batch.getObjtype(), objId, modeIds, changed,
            auditUser == null ? "anonymous" : auditUser.getUserName(),
            auditUser == null ? null : auditUser.getUserId());
        return true;
    }

    /**
     * 保存字段权限授权对象组：一个组绑定多个对象，字段权限只存一份。
     */
    private void saveFieldAuthGroups(Long modeId, List<org.springblade.formmode.dto.FieldAuthGroupDTO> groups) {
        String tenant = currentTenant();
        List<Long> oldGroups = jdbcTemplate.queryForList(
            "SELECT id FROM mode_field_auth_group WHERE modeid=? AND tenant_id=?", Long.class, modeId, tenant);
        if (oldGroups != null && !oldGroups.isEmpty()) {
            String marks = oldGroups.stream().map(String::valueOf).collect(Collectors.joining(","));
            jdbcTemplate.update("DELETE FROM modefieldauthorize WHERE group_id IN (" + marks + ")");
            jdbcTemplate.update("DELETE FROM mode_field_auth_subject WHERE group_id IN (" + marks + ")");
            jdbcTemplate.update("DELETE FROM mode_field_auth_group WHERE id IN (" + marks + ")");
        }
        if (groups == null || groups.isEmpty()) return;
        Long maxGroup = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM mode_field_auth_group", Long.class);
        long nextGroup = (maxGroup == null ? 0L : maxGroup) + 1;
        Long maxSubject = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM mode_field_auth_subject", Long.class);
        long nextSubject = (maxSubject == null ? 0L : maxSubject) + 1;
        Long maxField = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM modefieldauthorize", Long.class);
        long nextField = (maxField == null ? 0L : maxField) + 1;

        for (org.springblade.formmode.dto.FieldAuthGroupDTO group : groups) {
            if (group == null) continue;
            long groupId = nextGroup++;
            String name = org.springblade.core.tool.utils.Func.isBlank(group.getGroupName()) ? "授权对象组" : group.getGroupName().trim();
            String groupKey = org.springblade.core.tool.utils.Func.isBlank(group.getGroupKey())
                ? "group-" + groupId : group.getGroupKey().trim();
            jdbcTemplate.update("INSERT INTO mode_field_auth_group (id,tenant_id,modeid,group_name,group_key,create_time,update_time) VALUES (?,?,?,?,?,NOW(),NOW())",
                groupId, tenant, modeId, name, groupKey);
            if (group.getSubjects() != null) {
                for (org.springblade.formmode.dto.FieldAuthSubjectDTO s : group.getSubjects()) {
                    if (s == null || s.getObjtype() == null || org.springblade.core.tool.utils.Func.isBlank(s.getObjid())) continue;
                    long objId;
                    try { objId = Long.parseLong(String.valueOf(s.getObjid()).trim()); } catch (Exception e) { continue; }
                    jdbcTemplate.update("INSERT INTO mode_field_auth_subject (id,tenant_id,group_id,objtype,objid,includesub) VALUES (?,?,?,?,?,?)",
                        nextSubject++, tenant, groupId, s.getObjtype(), objId, s.getIncludesub() == null ? 0 : s.getIncludesub());
                }
            }
            if (group.getFields() != null) {
                for (org.springblade.formmode.dto.FieldSubjectItemDTO f : group.getFields()) {
                    if (f == null || f.getFieldId() == null || f.getRightvalue() == null) continue;
                    jdbcTemplate.update("INSERT INTO modefieldauthorize (id,modeid,group_id,fieldid,rightvalue,tenant_id) VALUES (?,?,?,?,?,?)",
                        nextField++, modeId, groupId, f.getFieldId().intValue(), f.getRightvalue(), tenant);
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> getFieldAuthGroups(Long modeId) {
        if (modeId == null) return new ArrayList<>();
        String tenant = currentTenant();
        try {
            List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT id, modeid, group_name, group_key FROM mode_field_auth_group WHERE modeid=? AND tenant_id=? ORDER BY id",
                modeId, tenant);
            if (groups == null || groups.isEmpty()) return new ArrayList<>();
            for (Map<String, Object> g : groups) {
                long gid = ((Number) g.get("id")).longValue();
                g.put("subjects", jdbcTemplate.queryForList(
                    "SELECT objtype,objid,includesub FROM mode_field_auth_subject WHERE group_id=? AND tenant_id=? ORDER BY objtype,objid",
                    gid, tenant));
                g.put("fields", jdbcTemplate.queryForList(
                    "SELECT fieldid,rightvalue FROM modefieldauthorize WHERE group_id=? AND tenant_id=? ORDER BY fieldid",
                    gid, tenant));
            }
            return groups;
        } catch (Exception e) {
            log.warn("[formmode] 查询字段权限组失败. modeId={}, err={}", modeId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 当前用户命中的字段权限组ID */
    private Set<Long> resolveMatchedFieldGroupIds(Long modeId, String tenant, Long uid,
                                                   Set<Long> roleIds, Set<Long> deptIds,
                                                   Set<Long> postIds, Set<Long> subcompanyIds) {
        Set<Long> result = new LinkedHashSet<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.group_id,s.objtype,s.objid,s.includesub FROM mode_field_auth_subject s "
                    + "JOIN mode_field_auth_group g ON g.id=s.group_id "
                    + "WHERE g.modeid=? AND g.tenant_id=? AND s.tenant_id=?",
                modeId, tenant, tenant);
            if (rows == null) return result;
            for (Map<String, Object> row : rows) {
                Object ot = row.get("objtype"), oi = row.get("objid");
                if (!(ot instanceof Number) || !(oi instanceof Number)) continue;
                int objtype = ((Number) ot).intValue();
                long objid = ((Number) oi).longValue();
                int includeSub = row.get("includesub") instanceof Number ? ((Number) row.get("includesub")).intValue() : 0;
                boolean hit = switch (objtype) {
                    case 4 -> uid != null && uid == objid;
                    case 1 -> objid == OBJ_ID_ALL || roleIds.contains(objid);
                    case 2 -> objid == OBJ_ID_ALL || subjectResolver.matchDept(objid, deptIds, includeSub);
                    case 3 -> objid == OBJ_ID_ALL || postIds.contains(objid);
                    case 5 -> objid == OBJ_ID_ALL || subcompanyIds.contains(objid);
                    default -> false;
                };
                if (hit && row.get("group_id") instanceof Number) {
                    result.add(((Number) row.get("group_id")).longValue());
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 查询用户字段权限组失败. modeId={}, err={}", modeId, e.getMessage());
        }
        return result;
    }

    @Override
    public Long resolveModeIdByFormId(Long formId) {
        if (formId == null) {
            return null;
        }
        // 1) 直连：modeinfo.billid = workflow_bill.id（同一 ID 体系）
        try {
            ModeInfo mi = modeInfoMapper.selectOne(new LambdaQueryWrapper<ModeInfo>()
                .eq(ModeInfo::getBillid, formId)
                .last("LIMIT 1"));
            if (mi != null) {
                return mi.getId();
            }
        } catch (Exception e) {
            log.warn("[formmode] 由表单ID反查模块ID失败. formId={}, err={}", formId, e.getMessage());
            return null;
        }
        // 2) 兜底：跨 ID 体系时按物理表名反查
        return resolveModeIdByTableName(formId);
    }

    /**
     * 按物理表名反查模块ID（跨 ID 体系兜底）。
     *
     * <p>场景：{@code modeinfo.billid} 存的是 ecology 迁移的小 billid（如 {@code 5}），
     * 而表单列表中的 {@code workflow_bill.id} 是雪花ID（如 {@code 2064530495200337922}），
     * 二者指向<b>同一张物理表</b>（如 {@code formtable_main_5}）。直连反查因此失败，
     * 需经「表单 → 表名 → 同表其它 bill 记录 → 模块」兜底。仍查不到则返回 null，
     * 由调用方按「无模块」处理（抽屉告警并禁用）。</p>
     */
    private Long resolveModeIdByTableName(Long formId) {
        try {
            WorkflowBill bill = workflowBillMapper.selectById(formId);
            String tableName = bill != null ? bill.getTableName() : null;
            if (tableName == null || tableName.isEmpty()) {
                return null;
            }
            List<WorkflowBill> sameTable = workflowBillMapper.selectList(
                new LambdaQueryWrapper<WorkflowBill>()
                    .eq(WorkflowBill::getTableName, tableName));
            if (sameTable == null || sameTable.isEmpty()) {
                return null;
            }
            List<Long> billIds = sameTable.stream()
                .map(WorkflowBill::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
            // 补充候选：由表名后缀推导 billid（formtable_main_5 → 5），
            // 覆盖「workflow_bill 中已无该 billid 记录」的迁移历史数据
            Long derived = parseTrailingId(tableName);
            if (derived != null && !billIds.contains(derived)) {
                billIds.add(derived);
            }
            if (billIds.isEmpty()) {
                return null;
            }
            ModeInfo mi = modeInfoMapper.selectOne(new LambdaQueryWrapper<ModeInfo>()
                .in(ModeInfo::getBillid, billIds)
                .last("LIMIT 1"));
            return mi == null ? null : mi.getId();
        } catch (Exception e) {
            log.warn("[formmode] 按表名反查模块ID失败. formId={}, err={}", formId, e.getMessage());
            return null;
        }
    }

    /** 从物理表名推导 billid：{@code formtable_main_5} → {@code 5}；后缀非数字返回 null */
    private Long parseTrailingId(String tableName) {
        if (tableName == null) {
            return null;
        }
        int idx = tableName.lastIndexOf('_');
        if (idx < 0 || idx == tableName.length() - 1) {
            return null;
        }
        try {
            return Long.parseLong(tableName.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Cacheable(cacheManager = "formmodeCacheManager", cacheNames = "formmode:field-perm",
        key = "T(org.springblade.formmode.service.impl.FormRightServiceImpl).currentTenant() + ':' + #modeId + ':' + T(org.springblade.core.secure.utils.SecureUtil).getUserId()")
    public List<FieldPermVO> listFieldPerm(Long modeId) {
        if (modeId == null) {
            return new ArrayList<>();
        }
        List<ModeFieldAuthorize> auths;
        String tenant = currentTenant();
        try {
            auths = modeFieldAuthorizeMapper.selectList(new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId)
                .and(w -> w.eq(ModeFieldAuthorize::getTenantId, tenant).or().isNull(ModeFieldAuthorize::getTenantId)));
        } catch (Exception e) {
            // 默认 fail-open：避免权限组件故障导致表单全空白；
            // 配置 blade.formmode.field-perm.fail-closed=true 时改为全字段隐藏（安全优先）。
            log.error("[formmode][AUDIT] 查询字段授权失败. modeId={}, failClosed={}, err={}",
                modeId, fieldPermFailClosed, e.getMessage(), e);
            return fieldPermFailClosed ? denyAllFieldPerm(modeId) : new ArrayList<>();
        }
        // 空 = 未配置字段授权：调用方按字段定义默认渲染（与流程侧「无节点权限」一致）
        if (auths == null || auths.isEmpty()) {
            return new ArrayList<>();
        }

        // 当前用户授权维度（与 checkRight 同源；Feign 透传 token 时由登录态保证）
        BladeUser user = AuthUtil.getUser();
        // 管理员豁免：字段权限不收窄（返回空 = 调用方按字段定义默认全量渲染）
        if (subjectResolver.isAdmin(user)) {
            return new ArrayList<>();
        }
        Long uid = user != null ? user.getUserId() : null;
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
        Set<Long> postIds = subjectResolver.resolvePostIds(uid);
        Set<Long> subcompanyIds = subjectResolver.resolveSubcompanyIds(uid);
        Set<Long> matchedGroupIds = resolveMatchedFieldGroupIds(modeId, tenant, uid, roleIds, deptIds, postIds, subcompanyIds);
        // 命中用户的授权 → fieldId → rightvalue（多条命中取最宽松：可编辑 > 可见）
        Map<Integer, Integer> fieldRight = new LinkedHashMap<>();
        for (ModeFieldAuthorize a : auths) {
            if (a == null || a.getFieldid() == null || a.getRightvalue() == null) {
                continue;
            }
            boolean hit;
            if (a.getGroupId() != null) {
                hit = matchedGroupIds.contains(a.getGroupId());
            } else {
                hit = matchAuthorizeObject(a, uid, roleIds, deptIds, postIds, subcompanyIds);
            }
            if (!hit) continue;
            fieldRight.merge(a.getFieldid(), a.getRightvalue(), Math::max);
        }
        // 兼容档位（默认）：保持既有语义——只返回命中字段，无命中则返回空（调用方按默认渲染）。
        if (fieldRight.isEmpty() && !fieldPermFailClosed && !maintainScopeStrict) {
            return new ArrayList<>();
        }
        // 严格档位：模块配置了字段授权但当前用户未命中 → 必须返回全部字段 + hidden，
        // 不能返回空让前端按「未配置」全放行（否则字段授权形同虚设）。
        // 注意：上面 auths.isEmpty()（模块完全没配）与 admin 豁免仍返回空 = 不收窄。
        Set<Integer> allFieldIds = new LinkedHashSet<>(fieldRight.keySet());
        allFieldIds.addAll(loadAllFieldIds(modeId));
        Map<Integer, WorkflowBillField> fieldDefs = loadFieldDefs(allFieldIds);

        List<FieldPermVO> result = new ArrayList<>();
        fieldDefs.forEach((fid, def) -> {
            if (def == null || def.getFieldname() == null || def.getFieldname().isEmpty()) {
                return; // 字段已删/查不到 → 跳过（与「幽灵字段」处理一致）
            }
            // 未命中授权的字段 → visible=false（拒绝）。命中授权的按 rightvalue 派生三态。
            Integer rv = fieldRight.get(fid);
            boolean visible = rv != null && rv >= 1;
            boolean editable = rv != null && rv >= 2;
            // 按字段归属派生作用域：主表 main / 明细表 dt{idx}，与审批态 scope 回退链对齐
            result.add(new FieldPermVO(scopeOfField(def), def.getFieldname(), visible, editable, null));
        });
        return result;
    }

    @Override
    public RuntimeFieldPermVO getRuntimeFieldPermByForm(Long formId, Integer layoutType) {
        RuntimeFieldPermVO result = new RuntimeFieldPermVO();
        int type = layoutType == null ? 0 : layoutType;
        result.setLayoutType(type);
        result.setFieldPerms(new ArrayList<>());
        if (formId == null) return result;

        Long modeId = resolveModeIdByFormId(formId);
        if (modeId == null) modeId = formId;
        result.setFieldPerms(listFieldPerm(modeId));

        try {
            BladeUser user = AuthUtil.getUser();
            // 管理员不收窄字段权限，也没有命中组概念 → 使用表单级通用布局
            if (subjectResolver.isAdmin(user)) return result;
            Long uid = user == null ? null : user.getUserId();
            Set<Long> roleIds = parseIds(user == null ? null : user.getRoleId());
            Set<Long> deptIds = parseIds(user == null ? null : user.getDeptId());
            Set<Long> postIds = subjectResolver.resolvePostIds(uid);
            Set<Long> subcompanyIds = subjectResolver.resolveSubcompanyIds(uid);
            Set<Long> matchedGroupIds = resolveMatchedFieldGroupIds(modeId, currentTenant(), uid, roleIds, deptIds, postIds, subcompanyIds);
            if (matchedGroupIds == null || matchedGroupIds.isEmpty()) return result;

            String marks = matchedGroupIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT id, group_key, group_name FROM mode_field_auth_group "
                    + "WHERE modeid=? AND tenant_id=? AND id IN (" + marks + ") ORDER BY id",
                modeId, currentTenant());
            if (groups == null || groups.isEmpty()) return result;

            // 多组命中时，优先选择已经配置了目标布局类型的组；否则按组ID顺序取第一个。
            String fallbackKey = null;
            String fallbackName = null;
            for (Map<String, Object> g : groups) {
                Object idObj = g.get("id");
                if (!(idObj instanceof Number)) continue;
                long gid = ((Number) idObj).longValue();
                String key = g.get("group_key") == null || String.valueOf(g.get("group_key")).trim().isEmpty()
                    ? "group-" + gid : String.valueOf(g.get("group_key")).trim();
                String name = g.get("group_name") == null ? "" : String.valueOf(g.get("group_name"));
                if (fallbackKey == null) {
                    fallbackKey = key;
                    fallbackName = name;
                }
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM form_layout WHERE form_id=? AND node_key=? AND layout_type=? AND status=1",
                    Long.class, formId, key, type);
                if (count != null && count > 0) {
                    result.setLayoutNodeKey(key);
                    result.setLayoutGroupName(name);
                    return result;
                }
            }
            result.setLayoutNodeKey(fallbackKey);
            result.setLayoutGroupName(fallbackName);
        } catch (Exception e) {
            log.warn("[formmode] 解析运行期布局组失败. formId={}, layoutType={}, err={}", formId, layoutType, e.getMessage());
        }
        return result;
    }

    /**
     * 解析「当前用户」对某模块的<b>数据范围</b>（行级数据权限，对应 {@code modedatasharerule}）。
     *
     * <p>复用 {@link #checkRight}/{@link #listFieldPerm} 同源的登录态维度（角色/部门来自
     * {@code BladeUser}）；岗位暂不支持（与字段权限同源残缺）。</p>
     *
     * @param modeId 模块ID（modeinfo.id）
     */
    @Override
    public DataScope resolveDataScope(Long modeId) {
        BladeUser user = AuthUtil.getUser();
        // 管理员豁免：数据范围全量可见
        if (subjectResolver.isAdmin(user)) {
            DataScope all = new DataScope();
            all.setCanViewAll(true);
            all.setCreatorId(user != null ? user.getUserId() : null);
            return all;
        }
        Long uid = user != null ? user.getUserId() : null;
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
        Set<Long> postIds = subjectResolver.resolvePostIds(uid);
        Long primaryDept = deptIds.isEmpty() ? null : deptIds.iterator().next();
        DataScope scope = dataScopeService.resolve(modeId, uid, primaryDept, new ArrayList<>(roleIds), new ArrayList<>(postIds));
        // 单条共享：查询共享给当前用户（人员/角色/部门/岗位）的数据行 ID。
        // sharedDataIds 同时用于列表过滤（creator OR id IN sharedIds）、详情读取与行级写校验。
        try {
            String tenant = currentTenant();
            List<ModeDataShareSingle> shares = modeDataShareSingleMapper.selectList(
                new LambdaQueryWrapper<ModeDataShareSingle>()
                    .eq(ModeDataShareSingle::getModeid, modeId)
                    .eq(ModeDataShareSingle::getTenantId, tenant));
            List<Long> sharedIds = new ArrayList<>();
            List<Long> editableIds = new ArrayList<>();
            Set<Long> subcompanyIds = subjectResolver.resolveSubcompanyIds(uid);
            for (ModeDataShareSingle s : shares) {
                if (s == null || s.getDataId() == null) continue;
                Integer t = s.getObjtype();
                Long o = s.getObjid();
                boolean hit = (t == 4 && uid != null && uid.equals(o))
                    || (t == 1 && roleIds.contains(o))
                    || (t == 2 && deptIds.contains(o))
                    || (t == 3 && postIds.contains(o))
                    || (t == 5 && subcompanyIds.contains(o));
                // 密级轴：单条共享命中后仍需过密级区间
                if (hit && !seclevelAllows(s.getShowlevel(), s.getShowlevel2(), uid)) {
                    hit = false;
                }
                if (hit) {
                    sharedIds.add(s.getDataId());
                    if (s.getShareLevel() != null && s.getShareLevel() >= 2) {
                        editableIds.add(s.getDataId());
                    }
                }
            }
            if (!sharedIds.isEmpty()) scope.setSharedDataIds(sharedIds);
            if (!editableIds.isEmpty()) scope.setEditableSharedDataIds(editableIds);
        } catch (Exception e) {
            log.warn("[formmode] 查询单条共享失败，忽略. modeId={}, err={}", modeId, e.getMessage());
        }
        return scope;
    }

    /**
     * 判断某行数据（创建人 creatorIdOfRow）是否在当前用户数据范围内（供详情/单条读取用）。
     */
    @Override
    public boolean inDataScope(Long modeId, Long creatorIdOfRow) {
        return inDataScope(modeId, creatorIdOfRow, null);
    }

    @Override
    public boolean inDataScope(Long modeId, Long creatorIdOfRow, Long dataId) {
        DataScope scope = resolveDataScope(modeId);
        if (scope.isCanViewAll()) {
            return true;
        }
        if (Objects.equals(creatorIdOfRow, scope.getCreatorId())) {
            return true;
        }
        // 单条共享：数据行 ID 命中共享给我的集合
        return dataId != null && scope.getSharedDataIds() != null && scope.getSharedDataIds().contains(dataId);
    }

    @Override
    public boolean canEditDataRow(Long modeId, Long creatorIdOfRow, Long dataId) {
        DataScope scope = resolveDataScope(modeId);
        // 创建人天然完全控制；单条共享仅 level>=2（可编辑）允许写
        if (Objects.equals(creatorIdOfRow, scope.getCreatorId())) {
            return true;
        }
        if (dataId != null && scope.getEditableSharedDataIds() != null
            && scope.getEditableSharedDataIds().contains(dataId)) {
            return true;
        }
        if (!scope.isCanViewAll()) {
            return false;
        }
        // 兼容模式（默认）：查看全量即可编辑全量（既有语义）
        if (!maintainScopeStrict) {
            return true;
        }
        // 严格模式：查看全量 ≠ 可维护全量，还需命中「维护」档(type=2)规则，
        // 对齐 E9「查看全量但只能维护本人」语义；未配维护档时只能改本人数据。
        BladeUser user = AuthUtil.getUser();
        if (subjectResolver.isAdmin(user)) {
            return true;
        }
        Long uid = user != null ? user.getUserId() : null;
        List<Long> roleIds = new ArrayList<>(parseIds(user != null ? user.getRoleId() : null));
        List<Long> jobIds = new ArrayList<>(subjectResolver.resolvePostIds(uid));
        Set<Long> deptSet = parseIds(user != null ? user.getDeptId() : null);
        Long deptId = deptSet.isEmpty() ? null : deptSet.iterator().next();
        return dataScopeService.canMaintainAll(modeId, uid, deptId, roleIds, jobIds);
    }

    @Override
    public boolean shareDataRow(Long modeId, Long dataId, Integer objtype, Long objid, Integer shareLevel,
                                String conditionField, String matrixValueField) {
        if (modeId == null || dataId == null || objtype == null || objid == null) {
            return false;
        }
        String tenant = currentTenant();
        ModeDataShareSingle exist = modeDataShareSingleMapper.selectOne(
            new LambdaQueryWrapper<ModeDataShareSingle>()
                .eq(ModeDataShareSingle::getModeid, modeId)
                .eq(ModeDataShareSingle::getDataId, dataId)
                .eq(ModeDataShareSingle::getObjtype, objtype)
                .eq(ModeDataShareSingle::getObjid, objid)
                .eq(ModeDataShareSingle::getTenantId, tenant)
                .last("LIMIT 1"));
        int level = (shareLevel == null || shareLevel < 1 || shareLevel > 2) ? 1 : shareLevel;
        if (exist == null) {
            ModeDataShareSingle s = new ModeDataShareSingle();
            s.setModeid(modeId);
            s.setDataId(dataId);
            s.setObjtype(objtype);
            s.setObjid(objid);
            s.setShareLevel(level);
            s.setConditionField(conditionField);
            s.setMatrixValueField(matrixValueField);
            s.setTenantId(tenant);
            s.setCreateTime(new java.util.Date());
            modeDataShareSingleMapper.insert(s);
        } else {
            exist.setShareLevel(level);
            exist.setConditionField(conditionField);
            exist.setMatrixValueField(matrixValueField);
            modeDataShareSingleMapper.updateById(exist);
        }
        return true;
    }

    @Override
    public boolean unshareDataRow(Long modeId, Long dataId, Integer objtype, Long objid) {
        if (modeId == null || dataId == null || objtype == null || objid == null) {
            return false;
        }
        modeDataShareSingleMapper.delete(new LambdaQueryWrapper<ModeDataShareSingle>()
            .eq(ModeDataShareSingle::getModeid, modeId)
            .eq(ModeDataShareSingle::getDataId, dataId)
            .eq(ModeDataShareSingle::getObjtype, objtype)
            .eq(ModeDataShareSingle::getObjid, objid)
            .eq(ModeDataShareSingle::getTenantId, currentTenant()));
        return true;
    }

    @Override
    public List<ModeDataShareSingle> listDataShares(Long modeId, Long dataId) {
        if (modeId == null || dataId == null) {
            return new ArrayList<>();
        }
        return modeDataShareSingleMapper.selectList(new LambdaQueryWrapper<ModeDataShareSingle>()
            .eq(ModeDataShareSingle::getModeid, modeId)
            .eq(ModeDataShareSingle::getDataId, dataId)
            .eq(ModeDataShareSingle::getTenantId, currentTenant()));
    }

    /**
     * 判断一条授权是否命中当前用户（与 {@link #checkRight} 同一套对象匹配规则）。
     *
     * <p>岗位(objtype=3)由 {@code blade_user.post_id} 解析；部门(objtype=2)精确匹配
     * （modefieldauthorize 表无 includesub 字段，字段级授权不支持子部门展开，
     * 模块级/数据范围级已支持）。</p>
     */
    private boolean matchAuthorizeObject(ModeFieldAuthorize a, Long uid, Set<Long> roleIds,
                                         Set<Long> deptIds, Set<Long> postIds, Set<Long> subcompanyIds) {
        Integer objType = a.getObjtype();
        Long objId = a.getObjid();
        if (objType == null || objId == null) {
            return false;
        }
        if (objId == OBJ_ID_ALL) {
            return true; // 0 = 全部/所有人
        }
        switch (objType) {
            case 4: // 人员
                return uid != null && uid.equals(objId);
            case 1: // 角色
                return roleIds.contains(objId);
            case 2: // 部门：includesub=1 时展开子孙部门（与模块权限同口径）
                return subjectResolver.matchDept(objId, deptIds, a.getIncludesub());
            case 3: // 岗位
                return postIds.contains(objId);
            case 5: // 分部
                return subcompanyIds.contains(objId);
            default:
                return false;
        }
    }

    /**
     * 批量查询字段ID → 字段定义；异常时返回空 Map（调用方跳过该字段）。
     *
     * <p>返回实体而非仅字段名，供调用方按 ismain/detailtable 派生字段权限 scope
     * （main / dt{idx}），与 workflow 侧 FieldPermVO 的作用域语义对齐。</p>
     */
    /**
     * 加载某模块对应表单的全部字段 ID（用于「已配置字段授权但未命中」时补全 hidden VO）。
     * 反查失败返回空集合（此时仅按 fieldRight 已命中的字段返回，不放大也不制造幽灵字段）。
     */
    private Set<Integer> loadAllFieldIds(Long modeId) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (modeId == null) return ids;
        try {
            ModeInfo mi = modeInfoMapper.selectById(modeId);
            Long billId = mi == null ? null : mi.getBillid();
            // 兜底：modeinfo 无记录时（迁移表单/前端以 formId 当 modeId），用 modeId 本身当 billid
            if (billId == null) billId = modeId;
            List<Integer> found = jdbcTemplate.queryForList(
                "SELECT id FROM workflow_billfield WHERE billid=? AND is_deleted=0", Integer.class, billId);
            if (found != null) ids.addAll(found);
        } catch (Exception e) {
            log.warn("[formmode] 加载全部字段ID失败，按仅已命中字段处理. modeId={}, err={}", modeId, e.getMessage());
        }
        return ids;
    }

    private Map<Integer, WorkflowBillField> loadFieldDefs(Set<Integer> fieldIds) {
        Map<Integer, WorkflowBillField> map = new LinkedHashMap<>();
        if (fieldIds == null || fieldIds.isEmpty()) {
            return map;
        }
        try {
            // 用 JdbcTemplate 直查（与模块内其它动态查询一致，绕开 BaseMapper 在此实体上的兼容问题）
            String in = fieldIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, fieldname, ismain, detailtable FROM workflow_billfield WHERE id IN (" + in + ")");
            for (Map<String, Object> row : rows) {
                Object idObj = row.get("id");
                Object nameObj = row.get("fieldname");
                if (!(idObj instanceof Number) || nameObj == null || String.valueOf(nameObj).isEmpty()) {
                    continue;
                }
                WorkflowBillField f = new WorkflowBillField();
                f.setId(((Number) idObj).intValue());
                f.setFieldname(String.valueOf(nameObj));
                Object ismainObj = row.get("ismain");
                f.setIsmain(ismainObj instanceof Number ? ((Number) ismainObj).intValue() : null);
                Object dtObj = row.get("detailtable");
                f.setDetailtable(dtObj instanceof Number ? ((Number) dtObj).intValue() : null);
                map.put(f.getId(), f);
            }
        } catch (Exception e) {
            log.warn("[formmode] 批量查询字段定义失败. fieldIds={}, err={}", fieldIds, e.getMessage());
        }
        return map;
    }

    /**
     * 由字段定义派生字段权限作用域。
     *
     * <p>ismain=1（或 detailtable 为空/0）→ main；ismain=0 → dt{detailtable}
     * （detailtable 为 1 起算的明细表索引，与 workflow 侧 scope 规范、ExcelPreviewPage
     * 的 /^dt\d+$/ 回退链一致）。</p>
     */
    private static String scopeOfField(WorkflowBillField f) {
        if (f == null || Integer.valueOf(1).equals(f.getIsmain())) {
            return "main";
        }
        Integer dt = f.getDetailtable();
        return (dt != null && dt > 0) ? "dt" + dt : "main";
    }

    /**
     * fail-closed：返回该模块全部字段「隐藏」的权限列表。
     *
     * <p>按 modeId → modeinfo.billid → workflow_billfield 查全部字段，逐字段生成
     * visible=false/editable=false 的 VO（含 main/dt{idx} 作用域），使前端所有字段隐藏。
     * 查不到字段定义时返回空列表（此时无从构造 deny，退化为 fail-open）。</p>
     */
    private List<FieldPermVO> denyAllFieldPerm(Long modeId) {
        List<FieldPermVO> deny = new ArrayList<>();
        if (modeId == null) {
            return deny;
        }
        try {
            ModeInfo mi = modeInfoMapper.selectById(modeId);
            Long billId = mi == null ? null : mi.getBillid();
            if (billId == null) {
                return deny;
            }
            List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(billId);
            if (fields == null) {
                return deny;
            }
            String tenant = currentTenant();
            for (WorkflowBillField f : fields) {
                if (f == null || f.getFieldname() == null || f.getFieldname().isEmpty()) {
                    continue;
                }
                // 字段定义也做租户过滤，避免跨租户字段被纳入
                if (f.getTenantId() != null && !f.getTenantId().equals(tenant)) {
                    continue;
                }
                deny.add(new FieldPermVO(scopeOfField(f), f.getFieldname(), false, false, null));
            }
            log.warn("[formmode][AUDIT] 字段权限 fail-closed 生效，全部字段隐藏. modeId={}, fields={}", modeId, deny.size());
        } catch (Exception e) {
            log.error("[formmode] 构造 fail-closed 字段权限失败. modeId={}, err={}", modeId, e.getMessage(), e);
            return new ArrayList<>();
        }
        return deny;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public boolean saveAll(Long modeId, org.springblade.formmode.dto.ModeRightBatchDTO batch) {
        if (modeId == null || batch == null) {
            return false;
        }
        // 乐观锁：前端带 expectedVersion 时先比对，不一致拒绝（防并发编辑静默覆盖）
        Long expected = batch.getExpectedVersion();
        if (expected != null) {
            long current = getRightVersion(modeId);
            if (current != expected) {
                throw new ServiceException("权限配置已被他人修改，请关闭后重新打开再编辑（版本 " + expected + " → " + current + "）");
            }
        }
        try {
            // 三表在同一事务内全量替换：任一失败整体回滚
            saveModeRights(modeId, batch.getRights());
            saveFieldAuthorizes(modeId, batch.getFieldAuthorizes());
            saveFieldAuthGroups(modeId, batch.getFieldGroups());
            dataScopeService.saveRules(modeId, batch.getDataScopes());
            // 方案C：一表单一菜单。按 formId 创建/更新表单数据菜单，授权给配置角色并写入角色级显示名。
            int menuGranted = 0;
            if (batch.getGrantMenuAccess() == null || batch.getGrantMenuAccess()) {
                menuGranted = ensureFormMenuAndGrant(modeId, batch);
            }
            // 版本递增：Redis 版本号与三表提交同向推进（写入成功后 INCR）
            bumpRightVersion(modeId);
            // 结构化审计日志：权限配置变更可追溯（操作人 / 模块 / 三表条数 / 版本）
            BladeUser auditUser = AuthUtil.getUser();
            int rightCnt = batch.getRights() == null ? 0 : batch.getRights().size();
            int fieldCnt = batch.getFieldAuthorizes() == null ? 0 : batch.getFieldAuthorizes().size();
            int scopeCnt = batch.getDataScopes() == null ? 0 : batch.getDataScopes().size();
            log.info("[formmode][AUDIT] 权限配置变更. modeId={}, operator={}({}), rights={}, fields={}, scopes={}, menuGranted={}, version={}",
                modeId,
                auditUser == null ? "anonymous" : auditUser.getUserName(),
                auditUser == null ? null : auditUser.getUserId(),
                rightCnt, fieldCnt, scopeCnt, menuGranted, getRightVersion(modeId));
            return true;
        } catch (Exception e) {
            log.error("[formmode] 权限三表聚合保存失败，事务回滚. modeId={}, err={}", modeId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 方案C：一表单一菜单。
     *
     * <p>按 formId 创建/更新 blade_menu（path=/formmode/formview/{formId}），
     * 重置该菜单的角色授权（来自模块权限 objtype=1），并写入角色级菜单显示名。</p>
     *
     * @return 本次授权的角色数
     */
    private int ensureFormMenuAndGrant(Long modeId, org.springblade.formmode.dto.ModeRightBatchDTO batch) {
        if (batch == null || org.springblade.core.tool.utils.Func.isBlank(batch.getFormId())) {
            log.warn("[formmode][MENU] 方案C菜单维护跳过：formId为空. modeId={}", modeId);
            return 0;
        }
        try {
            String formId = String.valueOf(batch.getFormId()).trim();
            if (!formId.matches("\\d+")) {
                log.warn("[formmode][MENU] 方案C菜单维护跳过：formId非法. modeId={}, formId={}", modeId, formId);
                return 0;
            }
            String code = "formdata_view_" + formId;
            String path = "/formmode/formview/" + formId;
            String tenantId = currentTenant();
            // 默认菜单名：优先前端传入，其次 workflow_bill.form_name，最后兜底
            String menuName = org.springblade.core.tool.utils.Func.isBlank(batch.getFormMenuName()) ? "" : batch.getFormMenuName().trim();
            if (org.springblade.core.tool.utils.Func.isBlank(menuName)) {
                List<String> names = jdbcTemplate.queryForList(
                    "SELECT form_name FROM workflow_bill WHERE id=?", String.class, Long.valueOf(formId));
                menuName = (names == null || names.isEmpty() || org.springblade.core.tool.utils.Func.isBlank(names.get(0)))
                    ? "表单数据" : names.get(0) + "数据";
            }
            // 1) 父菜单
            List<Long> parents = jdbcTemplate.queryForList(
                "SELECT id FROM blade_menu WHERE code='mode' AND path='/formmode' AND is_deleted=0 LIMIT 1", Long.class);
            long parentId = (parents == null || parents.isEmpty()) ? 0L : parents.get(0);
            // 2) 找/建菜单
            List<Long> menuIds = jdbcTemplate.queryForList(
                "SELECT id FROM blade_menu WHERE code=? AND is_deleted=0", Long.class, code);
            long menuId;
            if (menuIds == null || menuIds.isEmpty()) {
                Long max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM blade_menu", Long.class);
                menuId = (max == null ? 0L : max) + 1;
                jdbcTemplate.update(
                    "INSERT INTO blade_menu (id,parent_id,code,name,alias,path,source,sort,category,action,is_open,is_component,component_type,is_deleted,tenant_id) "
                        + "VALUES (?,?,?,?,?,?,'api',5,1,0,1,0,'bundled',0,?)",
                    menuId, parentId, code, menuName, "formdata_" + formId, path, tenantId);
            } else {
                menuId = menuIds.get(0);
                jdbcTemplate.update("UPDATE blade_menu SET name=?, path=?, parent_id=? WHERE id=?",
                    menuName, path, parentId, menuId);
            }
            // 3) 目标角色（objtype=1）
            java.util.Set<Long> roleIds = new java.util.LinkedHashSet<>();
            if (batch.getRights() != null) {
                for (ModeRightInfo r : batch.getRights()) {
                    if (r == null || r.getObjtype() == null || r.getObjid() == null || r.getObjtype() != 1) continue;
                    long rid = r.getObjid();
                    if (rid == 0L) {
                        // objid=0 = 全部角色
                        List<Long> allRoles = jdbcTemplate.queryForList(
                            "SELECT id FROM blade_role WHERE is_deleted=0", Long.class);
                        if (allRoles != null) roleIds.addAll(allRoles);
                    } else if (rid > 0) {
                        roleIds.add(rid);
                    }
                }
            }
            // 4) 重置该菜单的角色授权（建模权限为该菜单的唯一授权源）
            jdbcTemplate.update("DELETE FROM blade_role_menu WHERE menu_id=?", menuId);
            Long maxRm = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM blade_role_menu", Long.class);
            long nextRm = (maxRm == null ? 0L : maxRm) + 1;
            for (Long rid : roleIds) {
                jdbcTemplate.update("INSERT INTO blade_role_menu (id,role_id,menu_id) VALUES (?,?,?)", nextRm++, rid, menuId);
            }
            // 5) 重置角色级显示名（只保留本次提交且非空的值）
            jdbcTemplate.update("DELETE FROM blade_role_menu_name WHERE menu_id=?", menuId);
            Map<String, String> roleNames = batch.getRoleMenuNames();
            if (roleNames != null && !roleNames.isEmpty()) {
                Long maxRn = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM blade_role_menu_name", Long.class);
                long nextRn = (maxRn == null ? 0L : maxRn) + 1;
                for (Map.Entry<String, String> e : roleNames.entrySet()) {
                    if (org.springblade.core.tool.utils.Func.isBlank(e.getValue())) continue;
                    long rid;
                    try { rid = Long.parseLong(e.getKey()); } catch (Exception ex) { continue; }
                    if (!roleIds.contains(rid)) continue; // 只给已授权角色写别名
                    jdbcTemplate.update(
                        "INSERT INTO blade_role_menu_name (id,role_id,menu_id,name_override,tenant_id,create_time) VALUES (?,?,?,?,?,NOW())",
                        nextRn++, rid, menuId, e.getValue().trim(), tenantId);
                }
            }
            log.info("[formmode][AUDIT] 方案C表单菜单维护. modeId={}, formId={}, menuId={}, name={}, path={}, roles={}",
                modeId, formId, menuId, menuName, path, roleIds);
            return roleIds.size();
        } catch (Exception e) {
            log.warn("[formmode][MENU] 方案C表单菜单维护失败（不影响权限三表保存）. modeId={}, err={}", modeId, e.getMessage());
            return 0;
        }
    }

    /**
     * 把「表单数据」菜单（formdata_view）授权给模块权限中配置的角色（objtype=1）。
     *
     * <p>用 JdbcTemplate 幂等写 blade_role_menu（formmode 无 blade-system 依赖，不跨服务）。
     * 菜单不存在（目标环境未执行迁移脚本）时返回 0 并告警，不阻断权限保存。</p>
     *
     * @return 本次新授予的角色数
     */
    private int grantFormDataMenuToRoles(Long modeId, java.util.List<ModeRightInfo> rights) {
        if (rights == null || rights.isEmpty()) return 0;
        try {
            // 1) 找菜单
            List<Long> menus = jdbcTemplate.queryForList(
                "SELECT id FROM blade_menu WHERE code='formdata_view' AND is_deleted=0", Long.class);
            if (menus == null || menus.isEmpty()) {
                log.warn("[formmode] 表单数据菜单不存在，跳过菜单授权. modeId={}（请先执行 V2026.11.19_001 迁移）", modeId);
                return 0;
            }
            long menuId = menus.get(0);
            // 2) 提取角色 ID（objtype=1 且 objid>0，排除 0=所有人）
            java.util.Set<Long> roleIds = new java.util.LinkedHashSet<>();
            for (ModeRightInfo r : rights) {
                if (r == null || r.getObjtype() == null || r.getObjid() == null) continue;
                if (r.getObjtype() == 1 && r.getObjid() > 0) roleIds.add(r.getObjid().longValue());
            }
            if (roleIds.isEmpty()) return 0;
            // 3) 当前最大 id（幂等插入起点）
            Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM blade_role_menu", Long.class);
            long nextId = (maxId == null ? 0L : maxId) + 1;
            int granted = 0;
            for (Long roleId : roleIds) {
                Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM blade_role_menu WHERE role_id=? AND menu_id=?", Integer.class, roleId, menuId);
                if (exists != null && exists > 0) continue;
                jdbcTemplate.update("INSERT INTO blade_role_menu (id, role_id, menu_id) VALUES (?,?,?)",
                    nextId++, roleId, menuId);
                granted++;
            }
            if (granted > 0) {
                log.info("[formmode][AUDIT] 表单数据菜单授权. modeId={}, menuId={}, roles={}, granted={}",
                    modeId, menuId, roleIds, granted);
            }
            return granted;
        } catch (Exception e) {
            log.warn("[formmode] 表单数据菜单授权失败（不影响权限配置保存）. modeId={}, err={}", modeId, e.getMessage());
            return 0;
        }
    }

    @Override
    public long getRightVersion(Long modeId) {
        if (modeId == null) {
            return 0L;
        }
        try {
            String v = redisTemplate.opsForValue().get(RIGHT_VERSION_KEY + modeId);
            return v == null ? 0L : Long.parseLong(v);
        } catch (Exception e) {
            // Redis 不可用时降级为 0（每次保存都会 version=0→1，乐观锁失效但不阻断业务）
            log.warn("[formmode] 读取权限版本失败，按 0 处理. modeId={}, err={}", modeId, e.getMessage());
            return 0L;
        }
    }

    /** 版本递增（Redis 不可用静默跳过） */
    private void bumpRightVersion(Long modeId) {
        try {
            redisTemplate.opsForValue().increment(RIGHT_VERSION_KEY + modeId);
        } catch (Exception e) {
            log.warn("[formmode] 权限版本递增失败. modeId={}, err={}", modeId, e.getMessage());
        }
    }

    @Override
    public List<Long> getUserVisibleModeIds(Long userId) {
        // 查询用户可以看见的模块ID列表
        // 实际项目中需要关联用户角色/部门维度查询
        return List.of();
    }

}
