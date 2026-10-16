package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMaterialSaveDTO;
import org.springblade.portal.entity.PortalMaterial;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalMaterialMapper;
import org.springblade.portal.service.IPortalMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 素材库服务实现（对标泛微 materiallib §15.4 C2；文件走资源服务，本表记元数据）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalMaterialServiceImpl implements IPortalMaterialService {

	private final PortalMaterialMapper materialMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalMaterial> list(String dirName) {
		QueryWrapper<PortalMaterial> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (dirName != null && !dirName.isBlank()) {
			qw.eq("dir_name", dirName.trim());
		}
		qw.orderByAsc("dir_name").orderByAsc("id");
		return materialMapper.selectList(qw);
	}

	@Override
	public List<String> dirs() {
		// 目录维度去重（对标 getDirs）
		List<PortalMaterial> all = materialMapper.selectList(new QueryWrapper<PortalMaterial>()
			.eq("is_deleted", 0).select("dir_name"));
		Set<String> dirs = new LinkedHashSet<>();
		for (PortalMaterial m : all) {
			if (m.getDirName() != null && !m.getDirName().isBlank()) {
				dirs.add(m.getDirName());
			}
		}
		return List.copyOf(dirs);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalMaterialSaveDTO dto) {
		if (dto == null || isBlank(dto.getMaterialName()) || isBlank(dto.getFileUrl())) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalMaterial exist = (dto.getId() == null) ? null : materialMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			exist = new PortalMaterial();
			exist.setTenantId(tenantId);
			exist.setIsDeleted(0);
			exist.setStatus(1);
			exist.setCreateUser(userId);
			exist.setCreateDept(deptId);
			exist.setCreateTime(now);
		} else {
			exist.setUpdateUser(userId);
			exist.setUpdateTime(now);
		}
		exist.setDirName(isBlank(dto.getDirName()) ? "默认目录" : dto.getDirName().trim());
		exist.setMaterialName(dto.getMaterialName().trim());
		exist.setMaterialType(isBlank(dto.getMaterialType())
			? PortalConstant.MATERIAL_IMAGE : dto.getMaterialType().trim());
		exist.setFileUrl(dto.getFileUrl().trim());
		exist.setFileSize(dto.getFileSize());
		exist.setMimeType(dto.getMimeType());
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? materialMapper.insert(exist) > 0 : materialMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_MATERIAL_SAVE, "material", String.valueOf(exist.getId()),
				exist.getMaterialName(), (creating ? "上传素材 " : "编辑素材 ") + exist.getMaterialName(),
				null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalMaterial exist = materialMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalMaterial gone = new PortalMaterial();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		materialMapper.update(gone, new QueryWrapper<PortalMaterial>().eq("id", id));
		logService.record(PortalConstant.LOG_MATERIAL_DELETE, "material", String.valueOf(id),
			exist.getMaterialName(), "删除素材 " + exist.getMaterialName(), null, null);
		return true;
	}

	private String currentTenant() {
		BladeUser user = SecureUtil.getUser();
		if (user != null && user.getTenantId() != null && !user.getTenantId().isBlank()) {
			return user.getTenantId();
		}
		return "000000";
	}

	private Long toLongOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
