package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.mapper.ModeDataShareRuleMapper;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.vo.DataScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 行级数据范围解析实现（对标 ecology modedatasharerule，精简子集）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeDataShareRuleServiceImpl implements IDataScopeService {

    private final ModeDataShareRuleMapper mapper;

    /** 查看权限类型（modedatasharerule.type=1） */
    private static final int TYPE_VIEW = 1;
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

    @Override
    public DataScope resolve(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds) {
        DataScope scope = new DataScope();
        scope.setCanViewAll(false);
        scope.setCreatorId(userId);
        if (modeId == null || userId == null) {
            // 防御：无模块/用户 → 仅 creator 自阅（creatorId 可能为 null，则实际全不可见）
            return scope;
        }
        List<ModeDataShareRule> rules;
        try {
            rules = mapper.selectList(new LambdaQueryWrapper<ModeDataShareRule>()
                .eq(ModeDataShareRule::getModeid, modeId)
                .eq(ModeDataShareRule::getType, TYPE_VIEW));
        } catch (Exception e) {
            log.warn("[formmode] 数据范围规则加载失败，按 fail-open 仅 creator 自阅. modeId={}, err={}",
                modeId, e.getMessage());
            return scope;
        }
        for (ModeDataShareRule r : rules) {
            if (matchRule(r, userId, deptId, roleIds, jobIds)) {
                scope.setCanViewAll(true);
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

    /**
     * 单条规则是否命中（纯函数，便于单测）。
     *
     * <p>部门/角色的「含子级」展开需部门树/角色层级，本项目暂按精确匹配（include_sub 留作后续增强）。</p>
     */
    public static boolean matchRule(ModeDataShareRule r, Long userId, Long deptId,
                                    List<Long> roleIds, List<Long> jobIds) {
        if (r == null || r.getObjtype() == null || r.getObjid() == null) {
            return false;
        }
        Long objId = r.getObjid();
        switch (r.getObjtype()) {
            case OBJ_PERSON:
                return Objects.equals(objId, userId);
            case OBJ_DEPT:
                return Objects.equals(objId, deptId);
            case OBJ_ROLE:
                return roleIds != null && roleIds.contains(objId);
            case OBJ_JOB:
                return jobIds != null && jobIds.contains(objId);
            default:
                return false;
            }
            }

            @Override
            public List<ModeDataShareRule> listRules(Long modeId) {
            if (modeId == null) {
                return new ArrayList<>();
            }
            try {
                return mapper.selectList(new LambdaQueryWrapper<ModeDataShareRule>()
                    .eq(ModeDataShareRule::getModeid, modeId)
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
                mapper.delete(new LambdaQueryWrapper<ModeDataShareRule>().eq(ModeDataShareRule::getModeid, modeId));
                if (rules != null) {
                    for (ModeDataShareRule r : rules) {
                        if (r == null) {
                            continue;
                        }
                        r.setModeid(modeId);
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
