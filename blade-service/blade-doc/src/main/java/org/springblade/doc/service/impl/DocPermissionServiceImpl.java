package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocSecCategory;
import org.springblade.doc.entity.DocShare;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocSecCategoryMapper;
import org.springblade.doc.mapper.DocShareMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.system.feign.IDeptClient;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档级权限判定实现（对齐《落地规范》§3 的 DocViewPermission 等价物）。
 *
 * <p>判定路径：① 创建人 → ② 超管/内容管理员 → ③ 目录管理角色(manageRoleId) → ④ 显式共享。
 * 共享对象匹配经用户中心 Feign（{@link IUserClient}/{@link IDeptClient}）解析「角色/部门/岗位/分部」；
 * Feign 失败（fallback 返回 success=false）一律 fail-closed（宁缺勿滥，绝不因联调不通而放行）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocPermissionServiceImpl implements DocPermissionService {

    private final DocDetailMapper detailMapper;
    private final DocSecCategoryMapper categoryMapper;
    private final DocShareMapper shareMapper;
    private final IUserClient userClient;
    private final IDeptClient deptClient;

    @Override
    public boolean canRead(Long docId) {
        return canAccess(docId, 1);
    }

    @Override
    public boolean canManage(Long docId) {
        return canAccess(docId, 2);
    }

    @Override
    public boolean canDownload(Long docId) {
        return canAccess(docId, 1);
    }

    private boolean canAccess(Long docId, int requiredLevel) {
        BladeUser user = SecureUtil.getUser();
        Long userId = SecureUtil.getUserId();
        if (docId == null || user == null || userId == null) {
            return false;
        }
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            return false;
        }
        // 1) 创建人
        if (userId.equals(doc.getCreateUser())) {
            return true;
        }
        // 2) 超管 / 内容管理员
        if (hasRole(user, RoleConstant.ADMINISTRATOR, DocConstant.ROLE_DOC)) {
            return true;
        }
        // 3) 目录管理角色（manageRoleId）：用户是否隶属该角色（用户中心解析）
        if (isCategoryManager(doc, userId)) {
            return true;
        }
        // 4) 显式共享
        List<DocShare> shares = shareMapper.selectList(
            Wrappers.<DocShare>lambdaQuery().eq(DocShare::getDocId, docId));
        for (DocShare s : shares) {
            if (s.getShareLevel() != null && s.getShareLevel() >= requiredLevel && matchTarget(s, doc, user, userId)) {
                return true;
            }
        }
        // 5) fail-closed
        log.debug("doc 权限不足(拒绝) docId={} userId={} requiredLevel={}", docId, userId, requiredLevel);
        return false;
    }

    private boolean isCategoryManager(DocDetail doc, Long userId) {
        if (doc.getSecCategoryId() == null) {
            return false;
        }
        DocSecCategory cat = categoryMapper.selectById(doc.getSecCategoryId());
        return cat != null && userInRole(cat.getManageRoleId(), userId);
    }

    /**
     * 共享对象匹配（shareType 对齐 E9：1人员 2分部 3部门 4角色 5所有人 10岗位 80创建人）。
     * 未覆盖类型（6群组/7客户/8客户类型/81直接上级/82所有上级/84本分部/85本部门）暂 fail-closed，
     * 待接 CRM 客户端 / 上级链解析后补全。
     */
    private boolean matchTarget(DocShare s, DocDetail doc, BladeUser user, Long userId) {
        Integer t = s.getShareType();
        if (t == null) {
            return false;
        }
        Long target = s.getTargetId();
        switch (t) {
            case 1:
                return target != null && target.equals(userId);               // 人员
            case 2:
                return target != null && target.equals(userDivisionId(user)); // 分部
            case 3:
                return userInDept(target, userId);                            // 部门
            case 4:
                return userInRole(target, userId);                            // 角色
            case 5:
                return true;                                                  // 所有人
            case 10:
                return userInPost(target, userId);                            // 岗位
            case 80:
                return userId.equals(doc.getCreateUser());                    // 创建人
            default:
                return false;                                                 // 其余类型暂不匹配（fail-closed）
        }
    }

    private boolean userInRole(Long roleId, Long userId) {
        if (roleId == null || userId == null) {
            return false;
        }
        R<List<Long>> r = userClient.userIdsByRole(roleId);
        return r != null && r.isSuccess() && r.getData() != null && r.getData().contains(userId);
    }

    private boolean userInDept(Long deptId, Long userId) {
        if (deptId == null || userId == null) {
            return false;
        }
        R<List<Long>> r = userClient.userIdsByDept(deptId, false);
        return r != null && r.isSuccess() && r.getData() != null && r.getData().contains(userId);
    }

    private boolean userInPost(Long postId, Long userId) {
        if (postId == null || userId == null) {
            return false;
        }
        R<List<Long>> r = userClient.userIdsByPost(postId);
        return r != null && r.isSuccess() && r.getData() != null && r.getData().contains(userId);
    }

    private Long userDivisionId(BladeUser user) {
        if (user.getDeptId() == null || user.getDeptId().isBlank()) {
            return null;
        }
        try {
            R<Long> r = deptClient.divisionIdByDept(Long.valueOf(user.getDeptId()));
            return (r != null && r.isSuccess()) ? r.getData() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasRole(BladeUser user, String... roles) {
        if (user.getRoleName() == null || user.getRoleName().trim().isEmpty()) {
            return false;
        }
        for (String r : user.getRoleName().split(",")) {
            for (String want : roles) {
                if (want.equals(r.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
