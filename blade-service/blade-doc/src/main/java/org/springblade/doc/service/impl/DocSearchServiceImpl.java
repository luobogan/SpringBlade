package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.service.IDocSearchService;
import org.springblade.system.feign.IDeptClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检索服务实现：解析用户可见维度（人员/部门/角色/分部）+ 管理员 bypass，走 {@link DocDetailMapper#searchVisible}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocSearchServiceImpl implements IDocSearchService {

    private final DocDetailMapper detailMapper;
    private final IDeptClient deptClient;

    @Override
    public IPage<DocDetail> page(long current, long size, String keyword, Long secCategoryId) {
        BladeUser user = SecureUtil.getUser();
        Long userId = SecureUtil.getUserId();
        if (user == null || userId == null) {
            return new Page<>(current, size);
        }
        boolean allVisible = hasRole(user, RoleConstant.ADMINISTRATOR, DocConstant.ROLE_DOC);
        List<Long> roleIds = Func.toLongList(user.getRoleId());
        Long deptId = parseLong(user.getDeptId());
        Long divisionId = resolveDivision(deptId);
        Page<DocDetail> page = new Page<>(current, size);
        return detailMapper.searchVisible(page, keyword, secCategoryId, allVisible,
            userId, deptId, divisionId, roleIds);
    }

    private Long resolveDivision(Long deptId) {
        if (deptId == null) {
            return null;
        }
        try {
            R<Long> r = deptClient.divisionIdByDept(deptId);
            return (r != null && r.isSuccess()) ? r.getData() : null;
        } catch (Exception e) {
            log.warn("解析用户分部失败 deptId={}", deptId, e);
            return null;
        }
    }

    private Long parseLong(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Long.valueOf(s.trim());
        } catch (Exception e) {
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
