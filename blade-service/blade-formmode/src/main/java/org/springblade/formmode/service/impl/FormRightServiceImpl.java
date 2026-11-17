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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        // 未配置任何权限 → 默认放行（保持既有语义）
        if (rights == null || rights.isEmpty()) {
            return true;
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
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm"}, allEntries = true)
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
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm"}, allEntries = true)
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
        try {
            String tenant = currentTenant();
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
        // 命中用户的授权 → fieldId → rightvalue（多条命中取最宽松：可编辑 > 可见）
        Map<Integer, Integer> fieldRight = new LinkedHashMap<>();
        for (ModeFieldAuthorize a : auths) {
            if (a == null || a.getFieldid() == null || a.getRightvalue() == null) {
                continue;
            }
            if (!matchAuthorizeObject(a, uid, roleIds, deptIds, postIds, subcompanyIds)) {
                continue;
            }
            fieldRight.merge(a.getFieldid(), a.getRightvalue(), Math::max);
        }
        if (fieldRight.isEmpty()) {
            return new ArrayList<>();
        }

        // fieldId → 字段定义（modefieldauthorize 存的是 workflow_billfield.id，VO 需要字段名 + scope）
        Map<Integer, WorkflowBillField> fieldDefs = loadFieldDefs(fieldRight.keySet());

        List<FieldPermVO> result = new ArrayList<>();
        fieldRight.forEach((fid, rv) -> {
            WorkflowBillField def = fieldDefs.get(fid);
            if (def == null || def.getFieldname() == null || def.getFieldname().isEmpty()) {
                return; // 字段已删/查不到 → 跳过（与「幽灵字段」处理一致）
            }
            boolean visible = rv >= 1;
            boolean editable = rv >= 2;
            // 按字段归属派生作用域：主表 main / 明细表 dt{idx}，与审批态 scope 回退链对齐
            result.add(new FieldPermVO(scopeOfField(def), def.getFieldname(), visible, editable, null));
        });
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
        if (scope.isCanViewAll()) {
            return true;
        }
        if (Objects.equals(creatorIdOfRow, scope.getCreatorId())) {
            return true; // 创建人天然完全控制
        }
        // 单条共享仅 level>=2（可编辑）允许写
        return dataId != null && scope.getEditableSharedDataIds() != null
            && scope.getEditableSharedDataIds().contains(dataId);
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
        Integer objId = a.getObjid();
        if (objType == null || objId == null) {
            return false;
        }
        if (objId == OBJ_ID_ALL) {
            return true; // 0 = 全部/所有人
        }
        switch (objType) {
            case 4: // 人员
                return uid != null && uid.equals(objId.longValue());
            case 1: // 角色
                return roleIds.contains(objId.longValue());
            case 2: // 部门：includesub=1 时展开子孙部门（与模块权限同口径）
                return subjectResolver.matchDept(objId.longValue(), deptIds, a.getIncludesub());
            case 3: // 岗位
                return postIds.contains(objId.longValue());
            case 5: // 分部
                return subcompanyIds.contains(objId.longValue());
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
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm"}, allEntries = true)
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
            dataScopeService.saveRules(modeId, batch.getDataScopes());
            // 版本递增：Redis 版本号与三表提交同向推进（写入成功后 INCR）
            bumpRightVersion(modeId);
            // 结构化审计日志：权限配置变更可追溯（操作人 / 模块 / 三表条数 / 版本）
            BladeUser auditUser = AuthUtil.getUser();
            int rightCnt = batch.getRights() == null ? 0 : batch.getRights().size();
            int fieldCnt = batch.getFieldAuthorizes() == null ? 0 : batch.getFieldAuthorizes().size();
            int scopeCnt = batch.getDataScopes() == null ? 0 : batch.getDataScopes().size();
            log.info("[formmode][AUDIT] 权限配置变更. modeId={}, operator={}({}), rights={}, fields={}, scopes={}, version={}",
                modeId,
                auditUser == null ? "anonymous" : auditUser.getUserName(),
                auditUser == null ? null : auditUser.getUserId(),
                rightCnt, fieldCnt, scopeCnt, getRightVersion(modeId));
            return true;
        } catch (Exception e) {
            log.error("[formmode] 权限三表聚合保存失败，事务回滚. modeId={}, err={}", modeId, e.getMessage(), e);
            throw e;
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
