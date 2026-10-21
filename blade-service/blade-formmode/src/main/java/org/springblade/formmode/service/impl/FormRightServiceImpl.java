package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.mapper.ModeFieldAuthorizeMapper;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.ModeRightInfoMapper;
import org.springblade.formmode.mapper.WorkflowBillFieldMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.vo.DataScope;
import org.springblade.formmode.vo.FieldPermVO;
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

    /** 约定：objid=0 表示「全部/所有人」 */
    private static final long OBJ_ID_ALL = 0L;

    @Override
    public boolean checkRight(Long modeId, Integer rightType, Long userId) {
        List<ModeRightInfo> rights = modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
                .eq(ModeRightInfo::getRighttype, rightType)
        );
        // 未配置任何权限 → 默认放行（保持既有语义）
        if (rights == null || rights.isEmpty()) {
            return true;
        }

        // 解析当前用户授权维度（角色/部门/岗位来自登录态 BladeUser；审批态下由 Feign 透传 token 保证）
        BladeUser user = AuthUtil.getUser();
        Long uid = (userId != null) ? userId : (user != null ? user.getUserId() : null);
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
        // 注：BladeUser 未承载 postId，岗位(objtype=3)暂不参与匹配（如需支持，另查 blade_user.post_id）

        for (ModeRightInfo r : rights) {
            Integer objType = r.getObjtype();
            Long objId = (r.getObjid() == null) ? null : r.getObjid().longValue();
            if (objType == null || objId == null) {
                continue;
            }
            // 0 约定为「全部」
            if (objId == OBJ_ID_ALL) {
                return true;
            }
            switch (objType) {
                case 4: // 人员
                    if (uid != null && uid.equals(objId)) {
                        return true;
                    }
                    break;
                case 1: // 角色
                    if (roleIds.contains(objId)) {
                        return true;
                    }
                    break;
                case 2: // 部门（includesub 子部门展开需部门祖先链，暂按精确匹配）
                    if (deptIds.contains(objId)) {
                        return true;
                    }
                    break;
                case 3: // 岗位（登录态未携带 postId，暂不匹配；如需支持另查用户岗位）
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
        return modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveModeRights(Long modeId, List<ModeRightInfo> rights) {
        // 删除旧的权限
        modeRightInfoMapper.delete(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId)
        );
        // 插入新的权限
        if (rights != null) {
            for (ModeRightInfo right : rights) {
                right.setModeid(modeId);
                modeRightInfoMapper.insert(right);
            }
        }
        return true;
    }

    @Override
    public List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId) {
        return modeFieldAuthorizeMapper.selectList(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes) {
        modeFieldAuthorizeMapper.delete(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId)
        );
        if (authorizes != null) {
            for (ModeFieldAuthorize auth : authorizes) {
                auth.setModeid(modeId);
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
    public List<FieldPermVO> listFieldPerm(Long modeId) {
        if (modeId == null) {
            return new ArrayList<>();
        }
        List<ModeFieldAuthorize> auths;
        try {
            auths = modeFieldAuthorizeMapper.selectList(new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId));
        } catch (Exception e) {
            log.warn("[formmode] 查询字段授权失败. modeId={}, err={}", modeId, e.getMessage());
            return new ArrayList<>();
        }
        // 空 = 未配置字段授权：调用方按字段定义默认渲染（与流程侧「无节点权限」一致）
        if (auths == null || auths.isEmpty()) {
            return new ArrayList<>();
        }

        // 当前用户授权维度（与 checkRight 同源；Feign 透传 token 时由登录态保证）
        BladeUser user = AuthUtil.getUser();
        Long uid = user != null ? user.getUserId() : null;
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);

        // 命中用户的授权 → fieldId → rightvalue（多条命中取最宽松：可编辑 > 可见）
        Map<Integer, Integer> fieldRight = new LinkedHashMap<>();
        for (ModeFieldAuthorize a : auths) {
            if (a == null || a.getFieldid() == null || a.getRightvalue() == null) {
                continue;
            }
            if (!matchAuthorizeObject(a, uid, roleIds, deptIds)) {
                continue;
            }
            fieldRight.merge(a.getFieldid(), a.getRightvalue(), Math::max);
        }
        if (fieldRight.isEmpty()) {
            return new ArrayList<>();
        }

        // fieldId → fieldName（modefieldauthorize 存的是 workflow_billfield.id，VO 需要字段名）
        Map<Integer, String> fieldNames = loadFieldNames(fieldRight.keySet());

        List<FieldPermVO> result = new ArrayList<>();
        fieldRight.forEach((fid, rv) -> {
            String fn = fieldNames.get(fid);
            if (fn == null || fn.isEmpty()) {
                return; // 字段已删/查不到 → 跳过（与「幽灵字段」处理一致）
            }
            boolean visible = rv >= 1;
            boolean editable = rv >= 2;
            result.add(new FieldPermVO("main", fn, visible, editable, null));
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
        Long uid = user != null ? user.getUserId() : null;
        Set<Long> roleIds = parseIds(user != null ? user.getRoleId() : null);
        Set<Long> deptIds = parseIds(user != null ? user.getDeptId() : null);
        // 岗位（objtype=4）暂不支持（BladeUser 不承载 postId）；如需另查 blade_user.post_id
        Long primaryDept = deptIds.isEmpty() ? null : deptIds.iterator().next();
        return dataScopeService.resolve(modeId, uid, primaryDept, new ArrayList<>(roleIds), null);
    }

    /**
     * 判断某行数据（创建人 creatorIdOfRow）是否在当前用户数据范围内（供详情/单条读取用）。
     */
    @Override
    public boolean inDataScope(Long modeId, Long creatorIdOfRow) {
        DataScope scope = resolveDataScope(modeId);
        if (scope.isCanViewAll()) {
            return true;
        }
        return Objects.equals(creatorIdOfRow, scope.getCreatorId());
    }

    /**
     * 判断一条授权是否命中当前用户（与 {@link #checkRight} 同一套对象匹配规则）。
     *
     * <p>已知残缺（与 checkRight 相同，不在本期扩大）：岗位(objtype=3)恒不匹配
     * （BladeUser 不承载 postId）；部门只做精确匹配（includesub 子部门展开未实现）。</p>
     */
    private boolean matchAuthorizeObject(ModeFieldAuthorize a, Long uid, Set<Long> roleIds, Set<Long> deptIds) {
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
            case 2: // 部门（精确匹配）
                return deptIds.contains(objId.longValue());
            default:
                return false; // 3 岗位暂不支持
        }
    }

    /** 批量查询字段ID → 字段名；异常时返回空 Map（调用方跳过该字段） */
    private Map<Integer, String> loadFieldNames(Set<Integer> fieldIds) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (fieldIds == null || fieldIds.isEmpty()) {
            return map;
        }
        try {
            List<WorkflowBillField> fields = workflowBillFieldMapper.selectBatchIds(fieldIds);
            if (fields != null) {
                for (WorkflowBillField f : fields) {
                    if (f != null && f.getId() != null && f.getFieldname() != null && !f.getFieldname().isEmpty()) {
                        map.put(f.getId(), f.getFieldname());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 批量查询字段名失败. fieldIds={}, err={}", fieldIds, e.getMessage());
        }
        return map;
    }

    @Override
    public List<Long> getUserVisibleModeIds(Long userId) {
        // 查询用户可以看见的模块ID列表
        // 实际项目中需要关联用户角色/部门维度查询
        return List.of();
    }

}
