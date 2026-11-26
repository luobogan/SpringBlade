package org.springblade.formmode.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.formmode.dto.FieldAuthGroupDTO;
import org.springblade.formmode.dto.MenuShareDTO;
import org.springblade.formmode.dto.ModeAuthSchemeDTO;
import org.springblade.formmode.service.IAuthSchemeService;
import org.springblade.formmode.utils.DataScopeRuleUtil;
import org.springblade.formmode.utils.RightSubjectResolver;
import org.springblade.formmode.vo.AuthSchemeVO;
import org.springblade.system.datascope.DataScopeValueResolver;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSchemeServiceImpl implements IAuthSchemeService {

    private final JdbcTemplate jdbcTemplate;
    private final RightSubjectResolver subjectResolver;
    private final StringRedisTemplate redisTemplate;

    private static String tenant() {
        return Func.toStr(SecureUtil.getTenantId(), "000000");
    }

    /**
     * 保存授权方案后只清理当前模块、当前租户的运行期权限缓存。
     *
     * <p>缓存配置对 cacheName 再加了一层 "formmode:" 前缀，实际 key 形如
     * {@code formmode:formmode:field-perm::000000:{modeId}:{userId}}，因此这里按
     * 完整前缀匹配删除，避免全量清缓存影响其它表单。</p>
     */
    private void clearCurrentModeCache(long modeId) {
        String tenant = tenant();
        String prefix = "formmode:formmode:";
        String suffix = "::" + tenant + ":" + modeId + ":*";
        String[] names = {"right-check", "field-perm", "user-right-types"};
        try {
            for (String name : names) {
                Set<String> keys = redisTemplate.keys(prefix + name + suffix);
                if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("[formmode] 清理当前模块权限缓存失败. modeId={}, err={}", modeId, e.getMessage());
        }
    }

    private static Set<Long> ids(String csv) {
        Set<Long> set = new LinkedHashSet<>();
        if (Func.isBlank(csv)) return set;
        for (String p : csv.split(",")) {
            if (Func.isBlank(p)) continue;
            try { set.add(Long.parseLong(p.trim())); } catch (Exception ignored) { }
        }
        return set;
    }

    @Override
    public List<AuthSchemeVO> listByForm(Long formId) {
        List<AuthSchemeVO> out = new ArrayList<>();
        if (formId == null) return out;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM mode_auth_scheme WHERE tenant_id=? AND form_id=? AND status=1 ORDER BY priority,id",
            tenant(), formId);
        for (Map<String, Object> r : rows) out.add(toVO(r));
        return out;
    }

    @Override
    public AuthSchemeVO get(Long schemeId) {
        if (schemeId == null) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM mode_auth_scheme WHERE tenant_id=? AND id=? AND status=1", tenant(), schemeId);
        return rows.isEmpty() ? null : toVO(rows.get(0));
    }

    private AuthSchemeVO toVO(Map<String, Object> r) {
        AuthSchemeVO v = new AuthSchemeVO();
        v.setId(n(r.get("id")));
        v.setFormId(n(r.get("form_id")));
        v.setSchemeName(s(r.get("scheme_name")));
        v.setSubjectType(i(r.get("subject_type")));
        v.setSubjectId(n(r.get("subject_id")));
        v.setSubjectName(s(r.get("subject_name")));
        v.setIncludeSub(i(r.get("include_sub")));
        v.setMatrixValueField(s(r.get("matrix_value_field")));
        v.setShowlevel(i(r.get("showlevel")));
        v.setShowlevel2(i(r.get("showlevel2")));
        v.setPriority(i(r.get("priority")));
        Long id = v.getId();
        String t = tenant();

        List<AuthSchemeVO.RightItem> rights = new ArrayList<>();
        for (Map<String, Object> x : jdbcTemplate.queryForList(
            "SELECT id,righttype,objname,includesub,showlevel,showlevel2 FROM moderightinfo WHERE tenant_id=? AND scheme_id=? ORDER BY id", t, id)) {
            AuthSchemeVO.RightItem it = new AuthSchemeVO.RightItem();
            it.setId(n(x.get("id"))); it.setRighttype(i(x.get("righttype"))); it.setObjname(s(x.get("objname")));
            it.setIncludesub(i(x.get("includesub"))); it.setShowlevel(i(x.get("showlevel"))); it.setShowlevel2(i(x.get("showlevel2")));
            rights.add(it);
        }
        v.setRights(rights);

        List<AuthSchemeVO.FieldGroupItem> groups = new ArrayList<>();
        for (Map<String, Object> g : jdbcTemplate.queryForList(
            "SELECT id,group_key,group_name FROM mode_field_auth_group WHERE tenant_id=? AND scheme_id=? ORDER BY id", t, id)) {
            Long gid = n(g.get("id"));
            AuthSchemeVO.FieldGroupItem gi = new AuthSchemeVO.FieldGroupItem();
            gi.setId(gid); gi.setGroupKey(s(g.get("group_key"))); gi.setGroupName(s(g.get("group_name")));
            List<AuthSchemeVO.FieldItem> fs = new ArrayList<>();
            for (Map<String, Object> f : jdbcTemplate.queryForList(
                "SELECT fieldid,rightvalue FROM modefieldauthorize WHERE tenant_id=? AND group_id=? ORDER BY fieldid", t, gid)) {
                AuthSchemeVO.FieldItem fi = new AuthSchemeVO.FieldItem();
                fi.setFieldId(n(f.get("fieldid"))); fi.setRightvalue(i(f.get("rightvalue"))); fs.add(fi);
            }
            gi.setFields(fs); groups.add(gi);
        }
        v.setFieldGroups(groups);

        List<AuthSchemeVO.ScopeItem> scopes = new ArrayList<>();
        for (Map<String, Object> x : jdbcTemplate.queryForList(
            "SELECT id,type,objtype,objid,include_sub,creator_self,condition_field,condition_op,condition_value,condition_enabled,matrix_value_field,showlevel,showlevel2,scope_type,scope_column,scope_value,right_types,scope_subjects FROM modedatasharerule WHERE tenant_id=? AND scheme_id=? ORDER BY id", t, id)) {
            AuthSchemeVO.ScopeItem it = new AuthSchemeVO.ScopeItem();
            it.setId(n(x.get("id"))); it.setType(i(x.get("type"))); it.setObjtype(i(x.get("objtype"))); it.setObjid(n(x.get("objid")));
            it.setIncludeSub(i(x.get("include_sub"))); it.setCreatorSelf(i(x.get("creator_self")));
            it.setConditionField(s(x.get("condition_field"))); it.setConditionOp(s(x.get("condition_op"))); it.setConditionValue(s(x.get("condition_value")));
            it.setConditionEnabled(i(x.get("condition_enabled"))); it.setMatrixValueField(s(x.get("matrix_value_field")));
            it.setShowlevel(i(x.get("showlevel"))); it.setShowlevel2(i(x.get("showlevel2")));
            it.setScopeType(i(x.get("scope_type"))); it.setScopeColumn(s(x.get("scope_column"))); it.setScopeValue(s(x.get("scope_value")));
            String rawRightTypes = s(x.get("right_types"));
            List<Integer> parsedRightTypes = DataScopeRuleUtil.parseRightTypes(rawRightTypes);
            if (!DataScopeRuleUtil.isExplicitNoRights(rawRightTypes)
                && Func.isBlank(rawRightTypes) && parsedRightTypes.isEmpty()) {
                int legacy = i(x.get("type")) == 2 ? 2 : 1;
                parsedRightTypes = legacy == 2 ? java.util.Arrays.asList(1, 3, 4) : java.util.Arrays.asList(1);
            }
            it.setRightTypes(parsedRightTypes);
            List<AuthSchemeVO.ScopeSubjectItem> subjects = new ArrayList<>();
            for (DataScopeRuleUtil.ScopeSubject sub : DataScopeRuleUtil.parseSubjects(s(x.get("scope_subjects")))) {
                AuthSchemeVO.ScopeSubjectItem si = new AuthSchemeVO.ScopeSubjectItem();
                si.setObjtype(sub.getObjtype()); si.setObjid(sub.getObjid()); si.setIncludeSub(sub.getIncludeSub());
                subjects.add(si);
            }
            it.setSubjects(subjects);
            scopes.add(it);
        }
        v.setDataScopes(scopes);

        AuthSchemeVO.MenuItem menu = new AuthSchemeVO.MenuItem();
        List<Map<String, Object>> menuRows = jdbcTemplate.queryForList(
            "SELECT id,parent_id,name,alias,path,source,sort,status FROM blade_menu WHERE tenant_id=? AND code=? AND is_deleted=0 ORDER BY id LIMIT 1",
            t, "formdata_view_" + v.getFormId());
        if (!menuRows.isEmpty()) {
            Map<String, Object> m = menuRows.get(0);
            menu.setMenuId(n(m.get("id")));
            menu.setFormMenuName(s(m.get("name")));
            menu.setParentId(n(m.get("parent_id")));
            menu.setMenuPath(s(m.get("path")));
            menu.setMenuIcon(s(m.get("source")));
            menu.setMenuAlias(s(m.get("alias")));
            menu.setMenuSort(i(m.get("sort")));
            menu.setMenuStatus(i(m.get("status")));
        }
        List<Map<String, Object>> shares = jdbcTemplate.queryForList(
            "SELECT id,share_type,share_id,include_sub,showlevel,showlevel2 FROM blade_menu_share WHERE tenant_id=? AND scheme_id=? ORDER BY id", t, id);
        List<AuthSchemeVO.MenuShareItem> ms = new ArrayList<>();
        for (Map<String, Object> x : shares) {
            AuthSchemeVO.MenuShareItem m = new AuthSchemeVO.MenuShareItem();
            m.setId(n(x.get("id"))); m.setShareType(i(x.get("share_type"))); m.setShareId(n(x.get("share_id")));
            m.setIncludeSub(i(x.get("include_sub"))); m.setShowlevel(i(x.get("showlevel"))); m.setShowlevel2(i(x.get("showlevel2")));
            ms.add(m);
        }
        menu.setMenuShares(ms);
        v.setMenu(menu);

        List<Map<String, Object>> formScopeRows = jdbcTemplate.queryForList(
            "SELECT scope_type,scope_value FROM form_manage_auth_scope WHERE tenant_id=? AND scheme_id=? AND is_deleted=0 ORDER BY id LIMIT 1",
            t, id);
        if (!formScopeRows.isEmpty()) {
            AuthSchemeVO.FormManageScopeItem formScope = new AuthSchemeVO.FormManageScopeItem();
            formScope.setScopeType(i(formScopeRows.get(0).get("scope_type")));
            formScope.setScopeValue(s(formScopeRows.get(0).get("scope_value")));
            v.setFormManageScope(formScope);
        }
        return v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ModeAuthSchemeDTO dto) {
        if (dto == null || dto.getSubjectType() == null) throw new IllegalArgumentException("授权对象类型不能为空");
        int type = dto.getSubjectType();
        if (type < 1 || type > 7) throw new IllegalArgumentException("授权对象类型非法");
        if (type != 7 && dto.getSubjectId() == null) throw new IllegalArgumentException("授权对象不能为空");
        long subjectId = type == 7 ? 0L : dto.getSubjectId();
        if (type == 6 && Func.isBlank(dto.getMatrixValueField())) throw new IllegalArgumentException("矩阵方案必须配置矩阵结果字段");
        validateScopes(dto);

        if (dto.getFormId() == null) throw new IllegalArgumentException("表单ID不能为空");
        String t = tenant();
        long formId = dto.getFormId();
        long modeId = dto.getModeId() == null ? resolveModeId(formId) : dto.getModeId();
        long schemeId = dto.getId() == null ? max("SELECT COALESCE(MAX(id),0) FROM mode_auth_scheme") + 1 : dto.getId();
        String name = Func.isBlank(dto.getSchemeName()) ? ("授权方案-" + type + "-" + subjectId) : dto.getSchemeName().trim();

        List<Map<String, Object>> dup = jdbcTemplate.queryForList(
            "SELECT id FROM mode_auth_scheme WHERE tenant_id=? AND mode_id=? AND subject_type=? AND subject_id=? AND status=1 AND id<>?",
            t, modeId, type, subjectId, schemeId);
        if (!dup.isEmpty()) throw new IllegalArgumentException("同一授权对象只能存在一个有效方案");

        if (dto.getId() == null) {
            jdbcTemplate.update("INSERT INTO mode_auth_scheme(id,tenant_id,mode_id,form_id,scheme_name,subject_type,subject_id,subject_name,include_sub,matrix_value_field,showlevel,showlevel2,priority,status,version,create_user,create_time,update_user,update_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,1,1,?,NOW(),?,NOW())",
                schemeId, t, modeId, formId, name, type, subjectId, dto.getSubjectName(), nvl(dto.getIncludeSub()), dto.getMatrixValueField(), dto.getShowlevel(), dto.getShowlevel2(), dto.getPriority() == null ? 100 : dto.getPriority(), SecureUtil.getUserId(), SecureUtil.getUserId());
        } else {
            jdbcTemplate.update("UPDATE mode_auth_scheme SET mode_id=?,form_id=?,scheme_name=?,subject_name=?,include_sub=?,matrix_value_field=?,showlevel=?,showlevel2=?,priority=?,version=version+1 WHERE id=? AND tenant_id=?",
                modeId, formId, name, dto.getSubjectName(), nvl(dto.getIncludeSub()), dto.getMatrixValueField(), dto.getShowlevel(), dto.getShowlevel2(), dto.getPriority() == null ? 100 : dto.getPriority(), schemeId, t);
        }

        // 保存方案时只重建权限数据，不直接删除 Univer 布局。
        // 字段权限变化但 groupKey 不变时，已设计的布局必须保留。
        List<String> oldGroupKeys = jdbcTemplate.queryForList(
            "SELECT group_key FROM mode_field_auth_group WHERE tenant_id=? AND scheme_id=? AND group_key IS NOT NULL AND group_key<>''",
            String.class, t, schemeId);
        deleteSchemeChildren(schemeId, formId, false, false);

        jdbcTemplate.update("UPDATE form_manage_auth_scope SET is_deleted=1 WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        if (dto.getFormManageScope() != null) {
            int fsType = dto.getFormManageScope().getScopeType() == null ? 1 : dto.getFormManageScope().getScopeType();
            if (fsType < 1 || fsType > 3) throw new IllegalArgumentException("表单建模数据范围类型非法");
            String fsValue = Func.isBlank(dto.getFormManageScope().getScopeValue()) ? null : dto.getFormManageScope().getScopeValue().trim();
            if (fsType == 3 && Func.isBlank(fsValue)) throw new IllegalArgumentException("指定表单不能为空");
            jdbcTemplate.update("INSERT INTO form_manage_auth_scope(id,tenant_id,scheme_id,form_id,subject_type,subject_id,scope_type,scope_value,status,is_deleted,create_user,create_time,update_user,update_time) VALUES (?,?,?,?,?,?,?,?,1,0,?,NOW(),?,NOW())",
                max("SELECT COALESCE(MAX(id),0) FROM form_manage_auth_scope") + 1, t, schemeId, formId, type, subjectId,
                fsType, fsValue, SecureUtil.getUserId(), SecureUtil.getUserId());
        }

        if (dto.getRights() != null) {
            long rid = max("SELECT COALESCE(MAX(id),0) FROM moderightinfo");
            for (ModeAuthSchemeDTO.ModeRightInfoDTO r : dto.getRights()) {
                jdbcTemplate.update("INSERT INTO moderightinfo(id,modeid,righttype,objtype,objid,objname,includesub,showlevel,showlevel2,tenant_id,scheme_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    ++rid, modeId, r.getRighttype(), type, subjectId, r.getObjname(), nvl(r.getIncludesub()), r.getShowlevel(), r.getShowlevel2(), t, schemeId);
            }
        }

        long gid = max("SELECT COALESCE(MAX(id),0) FROM mode_field_auth_group");
        long fid = max("SELECT COALESCE(MAX(id),0) FROM modefieldauthorize");
        long sid = max("SELECT COALESCE(MAX(id),0) FROM mode_field_auth_subject");
        if (dto.getFieldGroups() != null) {
            for (FieldAuthGroupDTO g : dto.getFieldGroups()) {
                long groupId = ++gid;
                String key = Func.isBlank(g.getGroupKey()) ? ("scheme-" + schemeId + "-group-" + groupId) : g.getGroupKey();
                jdbcTemplate.update("INSERT INTO mode_field_auth_group(id,tenant_id,modeid,group_name,group_key,scheme_id,create_time,update_time) VALUES (?,?,?,?,?,?,NOW(),NOW())",
                    groupId, t, modeId, Func.isBlank(g.getGroupName()) ? "字段权限组" : g.getGroupName(), key, schemeId);
                jdbcTemplate.update("INSERT INTO mode_field_auth_subject(id,tenant_id,group_id,objtype,objid,includesub) VALUES (?,?,?,?,?,?)",
                    ++sid, t, groupId, type, subjectId, nvl(dto.getIncludeSub()));
                if (g.getFields() != null) {
                    for (org.springblade.formmode.dto.FieldSubjectItemDTO f : g.getFields()) {
                        jdbcTemplate.update("INSERT INTO modefieldauthorize(id,modeid,fieldid,objtype,objid,rightvalue,tenant_id,group_id,scheme_id) VALUES (?,?,?,?,?,?,?,?,?)",
                            ++fid, modeId, f.getFieldId(), type, subjectId, f.getRightvalue(), t, groupId, schemeId);
                    }
                }
            }
        }

        if (dto.getDataScopes() != null) {
            long scrollId = max("SELECT COALESCE(MAX(id),0) FROM modedatasharerule");
            for (ModeAuthSchemeDTO.ModeDataShareRuleDTO r : dto.getDataScopes()) {
                String rightTypes = DataScopeRuleUtil.joinRightTypes(r.getRightTypes());
                jdbcTemplate.update("INSERT INTO modedatasharerule(id,modeid,type,objtype,objid,include_sub,creator_self,condition_field,condition_op,condition_value,condition_enabled,matrix_value_field,showlevel,showlevel2,scope_type,scope_column,scope_value,right_types,scope_subjects,tenant_id,scheme_id,create_time,update_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                    ++scrollId, modeId, r.getType() == null ? 1 : r.getType(), type, subjectId, nvl(r.getIncludeSub()), nvl(r.getCreatorSelf()),
                    r.getConditionField(), r.getConditionOp(), r.getConditionValue(), nvl(r.getConditionEnabled()), r.getMatrixValueField(),
                    r.getShowlevel(), r.getShowlevel2(), r.getScopeType(), r.getScopeColumn(), r.getScopeValue(),
                    rightTypes, DataScopeRuleUtil.toJson(r.getSubjects()), t, schemeId);
            }
        }

        // 只删除本次不再使用的旧 groupKey 对应布局；字段配置变化但组不变时布局保留。
        List<String> newGroupKeys = new ArrayList<>();
        if (dto.getFieldGroups() != null) {
            for (FieldAuthGroupDTO g : dto.getFieldGroups()) {
                if (!Func.isBlank(g.getGroupKey())) newGroupKeys.add(g.getGroupKey().trim());
            }
        }
        for (String oldKey : oldGroupKeys) {
            if (oldKey != null && !newGroupKeys.contains(oldKey)) {
                jdbcTemplate.update("DELETE FROM form_layout WHERE tenant_id=? AND node_key=?", t, oldKey);
            }
        }

        saveMenu(dto, schemeId, formId, type, subjectId);
        clearCurrentModeCache(modeId);
        return schemeId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> saveBatch(List<ModeAuthSchemeDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        for (ModeAuthSchemeDTO dto : dtos) ids.add(save(dto));
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public Long copy(Long schemeId) {
        AuthSchemeVO src = get(schemeId);
        if (src == null) throw new IllegalArgumentException("授权方案不存在");
        ModeAuthSchemeDTO dto = new ModeAuthSchemeDTO();
        dto.setSchemeName(src.getSchemeName() + "（副本）");
        dto.setSubjectType(src.getSubjectType()); dto.setSubjectId(src.getSubjectId()); dto.setSubjectName(src.getSubjectName());
        dto.setIncludeSub(src.getIncludeSub()); dto.setMatrixValueField(src.getMatrixValueField());
        dto.setShowlevel(src.getShowlevel()); dto.setShowlevel2(src.getShowlevel2()); dto.setPriority(src.getPriority() + 1);
        if (src.getFormManageScope() != null) {
            ModeAuthSchemeDTO.FormManageScopeDTO scope = new ModeAuthSchemeDTO.FormManageScopeDTO();
            scope.setScopeType(src.getFormManageScope().getScopeType());
            scope.setScopeValue(src.getFormManageScope().getScopeValue());
            dto.setFormManageScope(scope);
        }
        return save(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long schemeId) {
        AuthSchemeVO vo = get(schemeId);
        if (vo == null) return false;
        deleteSchemeChildren(schemeId, vo.getFormId(), true, true);
        jdbcTemplate.update("DELETE FROM mode_auth_scheme WHERE tenant_id=? AND id=?", tenant(), schemeId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheManager = "formmodeCacheManager", cacheNames = {"formmode:right-check", "formmode:field-perm", "formmode:user-right-types"}, allEntries = true)
    public int deleteBatch(List<Long> schemeIds) {
        if (schemeIds == null || schemeIds.isEmpty()) return 0;
        int deleted = 0;
        for (Long schemeId : new LinkedHashSet<>(schemeIds)) {
            if (schemeId == null) continue;
            if (delete(schemeId)) deleted++;
        }
        return deleted;
    }

    private void deleteSchemeChildren(long schemeId, Long formId, boolean includeSchemeMenu, boolean deleteLayouts) {
        String t = tenant();
        List<Long> groupIds = jdbcTemplate.queryForList("SELECT id FROM mode_field_auth_group WHERE tenant_id=? AND scheme_id=?", Long.class, t, schemeId);
        List<String> groupKeys = jdbcTemplate.queryForList("SELECT group_key FROM mode_field_auth_group WHERE tenant_id=? AND scheme_id=?", String.class, t, schemeId);
        if (!groupIds.isEmpty()) {
            String marks = String.join(",", Collections.nCopies(groupIds.size(), "?"));
            jdbcTemplate.update("DELETE FROM modefieldauthorize WHERE tenant_id=? AND group_id IN (" + marks + ")", toArray(t, groupIds));
            jdbcTemplate.update("DELETE FROM mode_field_auth_subject WHERE tenant_id=? AND group_id IN (" + marks + ")", toArray(t, groupIds));
        }
        if (deleteLayouts && !groupKeys.isEmpty()) {
            String marks = String.join(",", Collections.nCopies(groupKeys.size(), "?"));
            List<Object> args = new ArrayList<>(); args.add(t); args.addAll(groupKeys);
            jdbcTemplate.update("DELETE FROM form_layout WHERE tenant_id=? AND node_key IN (" + marks + ")", args.toArray());
        }
        jdbcTemplate.update("DELETE FROM mode_field_auth_group WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        jdbcTemplate.update("DELETE FROM moderightinfo WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        jdbcTemplate.update("DELETE FROM modedatasharerule WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        jdbcTemplate.update("DELETE FROM blade_menu_share WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        jdbcTemplate.update("DELETE FROM form_manage_auth_scope WHERE tenant_id=? AND scheme_id=?", t, schemeId);
    }

    private void saveMenu(ModeAuthSchemeDTO dto, long schemeId, long formId, int type, long subjectId) {
        String t = tenant();
        ModeAuthSchemeDTO.MenuSettingDTO setting = dto.getMenu() == null ? new ModeAuthSchemeDTO.MenuSettingDTO() : dto.getMenu();
        String code = "formdata_view_" + formId;
        long viewMenuId = ensureMenuId(code, t);
        long parentMenuId = setting.getParentId() == null ? 0L : setting.getParentId();
        validateMenuParent(viewMenuId, parentMenuId, t);

        String menuName = Func.isBlank(setting.getFormMenuName()) ? "表单数据" : setting.getFormMenuName().trim();
        String defaultPath = "/formmode/formview/" + formId;
        if (Func.isNotBlank(setting.getMenuPath()) && !defaultPath.equals(setting.getMenuPath().trim())) {
            throw new IllegalArgumentException("表单数据菜单路径必须为 " + defaultPath);
        }
        String menuPath = defaultPath;
        String menuIcon = Func.isBlank(setting.getMenuIcon()) ? "file-text" : setting.getMenuIcon().trim();
        String menuAlias = Func.isBlank(setting.getMenuAlias()) ? menuName : setting.getMenuAlias().trim();
        int menuSort = setting.getMenuSort() == null ? 5 : setting.getMenuSort();
        int menuStatus = setting.getMenuStatus() == null ? 1 : setting.getMenuStatus();

        parentMenuId = ensureMenu(code, menuPath, menuName, menuAlias, menuIcon, parentMenuId, 1, 0, menuSort, menuStatus, t);
        ensureMenu("formdata_aae_" + formId, "/formmode/formview/aae", "表单新增编辑页", "表单新增编辑页",
            "edit", parentMenuId, 2, 1, menuSort + 1, menuStatus, t);
        List<Long> menuIds = Arrays.asList(parentMenuId, ensureMenuId("formdata_aae_" + formId, t));
        int shareType = menuShareType(type);
        jdbcTemplate.update("DELETE FROM blade_menu_share WHERE tenant_id=? AND scheme_id=?", t, schemeId);
        long shareId = max("SELECT COALESCE(MAX(id),0) FROM blade_menu_share");
        for (Long menuId : menuIds) {
            jdbcTemplate.update("INSERT INTO blade_menu_share(id,tenant_id,menu_id,share_type,share_id,include_sub,showlevel,showlevel2,status,is_deleted,scheme_id,create_user,create_time) VALUES (?,?,?,?,?,?,?,?,1,0,?,?,NOW())",
                ++shareId, t, menuId, shareType, subjectId, nvl(dto.getIncludeSub()), dto.getShowlevel(), dto.getShowlevel2(), schemeId, SecureUtil.getUserId());
        }
        if (type == 1) {
            // 兼容旧角色菜单链路：只补当前角色授权，不删除其它方案写入的授权。
            for (Long menuId : menuIds) {
                Long exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM blade_role_menu WHERE role_id=? AND menu_id=?", Long.class, subjectId, menuId);
                if (exists != null && exists > 0) continue;
                long rm = max("SELECT COALESCE(MAX(id),0) FROM blade_role_menu");
                jdbcTemplate.update("INSERT INTO blade_role_menu(id,role_id,menu_id) VALUES (?,?,?)", ++rm, subjectId, menuId);
            }
        }
    }

    private long ensureMenu(String code, String path, String name, String alias, String icon,
                            long parent, int category, int component, int sort, int status, String t) {
        long id = ensureMenuId(code, t);
        jdbcTemplate.update("UPDATE blade_menu SET name=?,alias=?,path=?,parent_id=?,category=?,is_component=?,is_open=1,source=?,sort=?,status=?,is_deleted=0 WHERE id=?",
            name, alias, path, parent, category, component, icon, sort, status, id);
        return id;
    }

    private void validateMenuParent(long menuId, long parentId, String t) {
        if (parentId == 0L) return;
        if (parentId == menuId) throw new IllegalArgumentException("上级菜单不能是当前菜单");
        List<Long> parents = jdbcTemplate.queryForList(
            "SELECT id FROM blade_menu WHERE tenant_id=? AND id=? AND category=1 AND is_deleted=0", Long.class, t, parentId);
        if (parents.isEmpty()) throw new IllegalArgumentException("上级菜单不存在或不是普通菜单");

        Set<Long> visited = new HashSet<>();
        long current = parentId;
        while (current != 0L) {
            if (current == menuId) throw new IllegalArgumentException("上级菜单不能是当前菜单的子菜单");
            if (!visited.add(current)) throw new IllegalArgumentException("上级菜单存在循环引用");
            List<Long> next = jdbcTemplate.queryForList("SELECT parent_id FROM blade_menu WHERE tenant_id=? AND id=? AND is_deleted=0",
                Long.class, t, current);
            if (next.isEmpty()) break;
            current = next.get(0) == null ? 0L : next.get(0);
        }
    }

    private long ensureMenuId(String code, String t) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM blade_menu WHERE tenant_id=? AND code=? AND is_deleted=0", Long.class, t, code);
        if (!ids.isEmpty()) return ids.get(0);
        long id = max("SELECT COALESCE(MAX(id),0) FROM blade_menu") + 1;
        jdbcTemplate.update("INSERT INTO blade_menu(id,tenant_id,parent_id,code,name,path,category,action,is_open,is_component,source,sort,is_deleted) VALUES (?,?,0,?,?,?,?,0,1,?,'api',5,0)",
            id, t, code, code, "/", categoryPlaceholder(code), componentPlaceholder(code));
        return id;
    }

    private static int categoryPlaceholder(String code) { return code.contains("_aae_") ? 2 : 1; }
    private static int componentPlaceholder(String code) { return code.contains("_aae_") ? 1 : 0; }

    private static int menuShareType(int subjectType) {
        switch (subjectType) {
            case 1: return 1;  // 角色
            case 4: return 2;  // 人员
            case 2: return 3;  // 部门
            case 5: return 4;  // 分部
            case 3: return 5;  // 岗位
            case 7: return 7;  // 全员
            case 6: return 8;  // 矩阵（扩展编码）
            default: return 9;
        }
    }

    private void validateScopes(ModeAuthSchemeDTO dto) {
        if (dto.getDataScopes() == null) return;
        for (ModeAuthSchemeDTO.ModeDataShareRuleDTO r : dto.getDataScopes()) {
            if (r.getRightTypes() != null) {
                for (Integer right : r.getRightTypes()) {
                    if (right == null || right < 1 || right > 4) throw new IllegalArgumentException("数据范围权限类型非法");
                }
            }
            Integer st = r.getScopeType();
            if (st == null) continue;
            if (st < 1 || st > 5) throw new IllegalArgumentException("数据范围类型非法");
            if ((st == 2 || st == 3 || st == 4) && Func.isBlank(r.getScopeColumn())) throw new IllegalArgumentException("本人/机构范围必须配置权限列");
            if (st == 5) {
                if (Func.isBlank(r.getScopeValue())) throw new IllegalArgumentException("自定义权限值域不能为空");
                String resolved = r.getScopeValue().replaceAll("\\$\\{[^}]+\\}", "1");
                if (DataScopeValueResolver.parseCustomScope(resolved).isRejected()) throw new IllegalArgumentException("自定义权限值域非法：" + r.getScopeValue());
            }
        }
    }

    @Override
    public List<Long> resolveMatchedSchemeIds(Long modeId) {
        List<Long> out = new ArrayList<>();
        if (modeId == null) return out;
        BladeUser user = AuthUtil.getUser();
        if (user == null) return out;
        Set<Long> roles = ids(user.getRoleId());
        Set<Long> depts = ids(user.getDeptId());
        Set<Long> posts = subjectResolver.resolvePostIds(user.getUserId());
        Set<Long> branches = subjectResolver.resolveSubcompanyIds(user.getUserId());
        Integer seclevel = resolveUserSeclevel(user.getUserId());
        for (Map<String, Object> r : jdbcTemplate.queryForList(
            "SELECT id,subject_type,subject_id,include_sub,showlevel,showlevel2 FROM mode_auth_scheme WHERE tenant_id=? AND mode_id=? AND status=1 ORDER BY priority,id", tenant(), modeId)) {
            int type = i(r.get("subject_type"));
            long subjectId = n(r.get("subject_id"));
            boolean hit = false;
            if (type == 7) hit = true;
            else if (type == 1) hit = roles.contains(subjectId);
            else if (type == 2) hit = subjectResolver.matchDept(subjectId, depts, nvl(i(r.get("include_sub"))));
            else if (type == 3) hit = posts.contains(subjectId);
            else if (type == 4) hit = Objects.equals(user.getUserId(), subjectId);
            else if (type == 5) hit = branches.contains(subjectId);
            else if (type == 6) hit = matrixContainsUser(subjectId, s(r.get("matrix_value_field")), user.getUserId());
            if (!hit) continue;
            if (r.get("showlevel") != null || r.get("showlevel2") != null) {
                if (seclevel == null) continue;
                int lo = i(r.get("showlevel")), hi = r.get("showlevel2") == null ? Integer.MAX_VALUE : i(r.get("showlevel2"));
                if (seclevel < lo || seclevel > hi) continue;
            }
            out.add(n(r.get("id")));
        }
        return out;
    }

    private Integer resolveUserSeclevel(Long userId) {
        if (userId == null) return null;
        try {
            List<Integer> rows = jdbcTemplate.queryForList(
                "SELECT security_level FROM blade_user WHERE id=? AND is_deleted=0", Integer.class, userId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matrixContainsUser(Long matrixId, String valueField, Long userId) {
        if (matrixId == null || userId == null) return false;
        try {
            List<String> rows = jdbcTemplate.queryForList(
                "SELECT data_json FROM wf_matrix_data WHERE matrix_id=? AND is_deleted=0", String.class, matrixId);
            for (String json : rows) {
                if (json != null && json.contains(String.valueOf(userId))) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private long resolveModeId(long formId) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM modeinfo WHERE billid=?", Long.class, formId);
        return ids.isEmpty() ? formId : ids.get(0);
    }

    private static long n(Object v) { return v instanceof Number ? ((Number) v).longValue() : 0L; }
    private static int i(Object v) { return v instanceof Number ? ((Number) v).intValue() : 0; }
    private static String s(Object v) { return v == null ? null : String.valueOf(v); }
    private static int nvl(Integer v) { return v == null ? 0 : v; }
    private static Object[] toArray(Object first, Collection<?> rest) {
        List<Object> a = new ArrayList<>(); a.add(first); a.addAll(rest); return a.toArray();
    }
    private long max(String sql) { Long v = jdbcTemplate.queryForObject(sql, Long.class); return v == null ? 0L : v; }
}
