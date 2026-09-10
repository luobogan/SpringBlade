package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.mapper.ModeFieldAuthorizeMapper;
import org.springblade.formmode.mapper.ModeRightInfoMapper;
import org.springblade.formmode.service.IFormRightService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
@Service
@RequiredArgsConstructor
public class FormRightServiceImpl implements IFormRightService {

    private final ModeRightInfoMapper modeRightInfoMapper;
    private final ModeFieldAuthorizeMapper modeFieldAuthorizeMapper;

    /** 约定：objid=0 表示「全部/所有人」 */
    private static final long OBJ_ID_ALL = 0L;

    @Override
    public boolean checkRight(Long modeId, Integer rightType, Long userId) {
        List<ModeRightInfo> rights = modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId.intValue())
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
                .eq(ModeRightInfo::getModeid, modeId.intValue())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveModeRights(Long modeId, List<ModeRightInfo> rights) {
        // 删除旧的权限
        modeRightInfoMapper.delete(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId.intValue())
        );
        // 插入新的权限
        if (rights != null) {
            for (ModeRightInfo right : rights) {
                right.setModeid(modeId.intValue());
                modeRightInfoMapper.insert(right);
            }
        }
        return true;
    }

    @Override
    public List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId) {
        return modeFieldAuthorizeMapper.selectList(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId.intValue())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes) {
        modeFieldAuthorizeMapper.delete(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId.intValue())
        );
        if (authorizes != null) {
            for (ModeFieldAuthorize auth : authorizes) {
                auth.setModeid(modeId.intValue());
                modeFieldAuthorizeMapper.insert(auth);
            }
        }
        return true;
    }

    @Override
    public List<Long> getUserVisibleModeIds(Long userId) {
        // 查询用户可以看见的模块ID列表
        // 实际项目中需要关联用户角色/部门维度查询
        return List.of();
    }

}
