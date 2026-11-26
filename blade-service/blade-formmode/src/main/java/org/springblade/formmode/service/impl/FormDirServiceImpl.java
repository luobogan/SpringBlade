package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.entity.FormDirAuth;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.ModeTreeField;
import org.springblade.formmode.mapper.FormDirAuthMapper;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.ModeTreeFieldMapper;
import org.springblade.formmode.service.IFormDirService;
import org.springblade.formmode.utils.RightSubjectResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 表单建模模块目录服务实现 */
@Service
@RequiredArgsConstructor
public class FormDirServiceImpl implements IFormDirService {

    private final ModeTreeFieldMapper modeTreeFieldMapper;
    private final FormDirAuthMapper formDirAuthMapper;
    private final ModeInfoMapper modeInfoMapper;
    private final org.springblade.formmode.mapper.WorkflowBillMapper workflowBillMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RightSubjectResolver subjectResolver;

    @Override
    public List<ModeTreeField> tree() {
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        List<ModeTreeField> rows = modeTreeFieldMapper.selectList(new LambdaQueryWrapper<ModeTreeField>()
            .eq(ModeTreeField::getTenantId, tenant)
            .orderByAsc(ModeTreeField::getParentid, ModeTreeField::getDsporder, ModeTreeField::getId));
        rows.sort(Comparator.comparingInt((ModeTreeField d) -> d.getLevel() == null ? 1 : d.getLevel())
            .thenComparing(d -> d.getDsporder() == null ? 0 : d.getDsporder()));
        if (!subjectResolver.isAdmin(AuthUtil.getUser())) {
            rows.removeIf(d -> d.getId() != null && !canView(d.getId()));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ModeTreeField dir) {
        if (dir == null || StringUtil.isBlank(dir.getName())) throw new IllegalArgumentException("目录名称不能为空");
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        dir.setName(dir.getName().trim());
        dir.setTenantId(tenant);
        if (dir.getParentid() == null) dir.setParentid(0L);
        if (dir.getLevel() == null) dir.setLevel(dir.getParentid() == 0L ? 1 : 2);
        if (dir.getDsporder() == null) dir.setDsporder(0);
        if (dir.getStatus() == null) dir.setStatus(1);
        if (dir.getId() == null) {
            dir.setId(System.currentTimeMillis());
            dir.setCreateUser(SecureUtil.getUserId());
            dir.setCreateTime(java.time.LocalDateTime.now());
            modeTreeFieldMapper.insert(dir);
        } else {
            dir.setUpdateUser(SecureUtil.getUserId());
            dir.setUpdateTime(java.time.LocalDateTime.now());
            modeTreeFieldMapper.updateById(dir);
        }
        return dir.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        if (id == null) return false;
        long children = modeTreeFieldMapper.selectCount(new LambdaQueryWrapper<ModeTreeField>()
            .eq(ModeTreeField::getParentid, id));
        if (children > 0) throw new IllegalArgumentException("请先删除子目录");
        long used = modeInfoMapper.selectCount(new LambdaQueryWrapper<ModeInfo>()
            .eq(ModeInfo::getModeTreeField, id));
        if (used > 0) throw new IllegalArgumentException("目录下仍有表单，请先移动表单");
        modeTreeFieldMapper.deleteById(id);
        formDirAuthMapper.delete(new LambdaQueryWrapper<FormDirAuth>().eq(FormDirAuth::getDirId, id));
        return true;
    }

    @Override
    public List<FormDirAuth> listAuth(Long dirId) {
        if (dirId == null) return new ArrayList<>();
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        return formDirAuthMapper.selectList(new LambdaQueryWrapper<FormDirAuth>()
            .eq(FormDirAuth::getTenantId, tenant)
            .eq(FormDirAuth::getDirId, dirId)
            .orderByAsc(FormDirAuth::getSchemeId, FormDirAuth::getPriority, FormDirAuth::getSubjectType,
                FormDirAuth::getSubjectId, FormDirAuth::getRightType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAuth(Long dirId, List<FormDirAuth> auths) {
        if (dirId == null) throw new IllegalArgumentException("目录ID不能为空");
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        formDirAuthMapper.delete(new LambdaQueryWrapper<FormDirAuth>()
            .eq(FormDirAuth::getTenantId, tenant).eq(FormDirAuth::getDirId, dirId));
        if (auths != null) {
            for (FormDirAuth auth : auths) {
                if (auth == null || auth.getSubjectType() == null || auth.getRightType() == null) continue;
                auth.setId(null);
                auth.setTenantId(tenant);
                auth.setDirId(dirId);
                auth.setSubjectId(auth.getSubjectId() == null ? 0L : auth.getSubjectId());
                auth.setIncludeSub(auth.getIncludeSub() == null ? 0 : auth.getIncludeSub());
                auth.setScopeType(auth.getScopeType() == null ? 1 : auth.getScopeType());
                auth.setScopeValue(auth.getScopeValue());
                auth.setSchemeId(auth.getSchemeId() == null ? auth.getId() : auth.getSchemeId());
                auth.setSchemeName(StringUtil.isBlank(auth.getSchemeName()) ? ("目录权限方案-" + auth.getSchemeId()) : auth.getSchemeName().trim());
                auth.setPriority(auth.getPriority() == null ? 100 : auth.getPriority());
                auth.setShowlevel(auth.getShowlevel());
                auth.setShowlevel2(auth.getShowlevel2());
                auth.setScopeColumn(auth.getScopeColumn());
                auth.setStatus(1);
                formDirAuthMapper.insert(auth);
            }
        }
        return true;
    }

    @Override
    public boolean canView(Long dirId) {
        if (dirId == null) return false;
        BladeUser user = AuthUtil.getUser();
        if (subjectResolver.isAdmin(user)) return true;
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        List<FormDirAuth> auths = formDirAuthMapper.selectList(new LambdaQueryWrapper<FormDirAuth>()
            .eq(FormDirAuth::getTenantId, tenant)
            .eq(FormDirAuth::getDirId, dirId));
        if (auths.isEmpty()) return true;
        Long uid = user == null ? null : user.getUserId();
        Set<Long> roleIds = parseIds(user == null ? null : user.getRoleId());
        Set<Long> deptIds = parseIds(user == null ? null : user.getDeptId());
        Set<Long> postIds = subjectResolver.resolvePostIds(uid);
        Set<Long> branchIds = subjectResolver.resolveSubcompanyIds(uid);
        for (FormDirAuth a : auths) {
            if (a.getSubjectType() == null || a.getRightType() == null || a.getRightType() < 1) continue;
            long oid = a.getSubjectId() == null ? 0L : a.getSubjectId();
            boolean hit = switch (a.getSubjectType()) {
                case 7 -> true;
                case 4 -> uid != null && uid == oid;
                case 1 -> roleIds.contains(oid);
                case 2 -> subjectResolver.matchDept(oid, deptIds, a.getIncludeSub() == null ? 0 : a.getIncludeSub());
                case 3 -> postIds.contains(oid);
                case 5 -> branchIds.contains(oid);
                default -> false;
            };
            if (!hit) continue;
            if (a.getShowlevel() != null || a.getShowlevel2() != null) {
                if (!subjectResolver.seclevelMatches(a.getShowlevel(), a.getShowlevel2(), subjectResolver.resolveSeclevel(uid))) continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Long> filterFormsByDirAuth(Long dirId, List<Long> formIds) {
        if (dirId == null || formIds == null || formIds.isEmpty()) return formIds;
        BladeUser user = AuthUtil.getUser();
        if (subjectResolver.isAdmin(user)) return formIds;
        String tenant = StringUtil.isBlank(SecureUtil.getTenantId()) ? "000000" : SecureUtil.getTenantId();
        List<FormDirAuth> auths = formDirAuthMapper.selectList(new LambdaQueryWrapper<FormDirAuth>()
            .eq(FormDirAuth::getTenantId, tenant)
            .eq(FormDirAuth::getDirId, dirId));
        if (auths.isEmpty()) return formIds;
        Long uid = user == null ? null : user.getUserId();
        Set<Long> roleIds = parseIds(user == null ? null : user.getRoleId());
        Set<Long> deptIds = parseIds(user == null ? null : user.getDeptId());
        Set<Long> postIds = subjectResolver.resolvePostIds(uid);
        Set<Long> branchIds = subjectResolver.resolveSubcompanyIds(uid);
        Set<Long> allowed = new LinkedHashSet<>();
        for (FormDirAuth auth : auths) {
            if (auth.getSubjectType() == null || auth.getRightType() == null) continue;
            long oid = auth.getSubjectId() == null ? 0L : auth.getSubjectId();
            boolean hit = switch (auth.getSubjectType()) {
                case 7 -> true;
                case 4 -> uid != null && uid == oid;
                case 1 -> roleIds.contains(oid);
                case 2 -> subjectResolver.matchDept(oid, deptIds, auth.getIncludeSub() == null ? 0 : auth.getIncludeSub());
                case 3 -> postIds.contains(oid);
                case 5 -> branchIds.contains(oid);
                default -> false;
            };
            if (!hit) continue;
            if (auth.getShowlevel() != null || auth.getShowlevel2() != null) {
                if (!subjectResolver.seclevelMatches(auth.getShowlevel(), auth.getShowlevel2(), subjectResolver.resolveSeclevel(uid))) continue;
            }
            int scopeType = auth.getScopeType() == null ? 1 : auth.getScopeType();
            if (scopeType == 1) {
                allowed.addAll(formIds);
            } else if (scopeType == 2) {
                if (uid != null) {
                    for (WorkflowBill bill : workflowBillMapper.selectList(new LambdaQueryWrapper<WorkflowBill>()
                        .select(WorkflowBill::getId).eq(WorkflowBill::getCreateUser, uid.intValue()))) {
                        if (bill.getId() != null && formIds.contains(bill.getId())) allowed.add(bill.getId());
                    }
                }
            } else if (scopeType == 5 && !StringUtil.isBlank(auth.getScopeValue())) {
                String column = StringUtil.isBlank(auth.getScopeColumn()) ? "id" : auth.getScopeColumn().trim();
                if (!java.util.Arrays.asList("id", "create_user", "status", "form_type", "detail_table_count").contains(column)) continue;
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(auth.getScopeValue());
                List<Long> values = new ArrayList<>();
                while (matcher.find()) {
                    Long value = Long.valueOf(matcher.group());
                    if (!values.contains(value)) values.add(value);
                }
                if (!values.isEmpty()) {
                    String marks = String.join(",", java.util.Collections.nCopies(values.size(), "?"));
                    for (Long id : jdbcTemplate.queryForList(
                        "SELECT id FROM workflow_bill WHERE is_deleted=0 AND `" + column + "` IN (" + marks + ")",
                        Long.class, values.toArray())) {
                        if (id != null && formIds.contains(id)) allowed.add(id);
                    }
                }
            } else if ((scopeType == 3 || scopeType == 4) && uid != null) {
                Set<Long> scopeDepts = new HashSet<>(deptIds);
                if (scopeType == 4) deptIds.forEach(d -> scopeDepts.addAll(subjectResolver.descendantDeptIds(d)));
                if (!scopeDepts.isEmpty()) {
                    for (WorkflowBill bill : workflowBillMapper.selectList(new LambdaQueryWrapper<WorkflowBill>()
                        .select(WorkflowBill::getId)
                        .inSql(WorkflowBill::getCreateUser,
                            "SELECT id FROM blade_user WHERE is_deleted=0 AND dept_id IN (" +
                                scopeDepts.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0") + ")"))) {
                        if (bill.getId() != null && formIds.contains(bill.getId())) allowed.add(bill.getId());
                    }
                }
            }
        }
        return new ArrayList<>(allowed);
    }

    private static Set<Long> parseIds(String csv) {
        Set<Long> result = new HashSet<>();
        if (StringUtil.isBlank(csv)) return result;
        for (String part : csv.split(",")) {
            if (StringUtil.isBlank(part)) continue;
            try { result.add(Long.valueOf(part.trim())); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveForms(List<Long> formIds, Long dirId) {
        if (formIds == null || formIds.isEmpty()) return false;
        if (dirId != null && !canView(dirId)) throw new IllegalArgumentException("无权使用目标目录");
        for (Long formId : formIds) {
            if (formId != null) moveForm(formId, dirId);
        }
        return true;
    }

    @Override
    public Long getFormDir(Long formId) {
        if (formId == null) return null;
        List<ModeInfo> rows = modeInfoMapper.selectList(new LambdaQueryWrapper<ModeInfo>()
            .eq(ModeInfo::getBillid, formId)
            .orderByDesc(ModeInfo::getId));
        return rows.isEmpty() ? null : rows.get(0).getModeTreeField();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveForm(Long formId, Long dirId) {
        if (formId == null) return false;
        List<ModeInfo> rows = modeInfoMapper.selectList(new LambdaQueryWrapper<ModeInfo>()
            .eq(ModeInfo::getBillid, formId));
        if (rows.isEmpty()) {
            WorkflowBill bill = workflowBillMapper.selectById(formId);
            if (bill == null) throw new IllegalArgumentException("表单不存在");
            ModeInfo row = new ModeInfo();
            row.setBillid(formId);
            row.setModename(bill.getFormName());
            row.setModedescription(bill.getDescription());
            row.setModetype(0);
            row.setStatus(bill.getStatus() == null ? 1 : bill.getStatus());
            row.setModeTreeField(dirId);
            row.setTenantId(bill.getTenantId());
            modeInfoMapper.insert(row);
            return true;
        }
        for (ModeInfo row : rows) {
            row.setModeTreeField(dirId);
            modeInfoMapper.updateById(row);
        }
        return true;
    }
}
