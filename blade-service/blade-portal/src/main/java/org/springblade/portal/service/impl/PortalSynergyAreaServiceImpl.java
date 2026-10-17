package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalSynergyAreaSaveDTO;
import org.springblade.portal.entity.PortalSynergyArea;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalSynergyAreaMapper;
import org.springblade.portal.service.IPortalSynergyAreaService;
import org.springblade.workflow.feign.IWfSynergyClient;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 门户协同区服务实现（对标 synergy §15.7 C2；知识型延后，本期实现 workflow/form/node）。
 * <p>深度对接 blade-workflow：{@code IWfSynergyClient} 提供流程定义/节点/表单字段的真实选项。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSynergyAreaServiceImpl implements IPortalSynergyAreaService {

	private static final List<String> AREA_TYPES = Arrays.asList("workflow", "form", "node");

	private final PortalSynergyAreaMapper synergyMapper;
	private final IPortalLogService logService;
	private final IWfSynergyClient wfSynergyClient;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PortalSynergyArea save(PortalSynergyAreaSaveDTO dto) {
		if (!AREA_TYPES.contains(dto.getAreaType())) {
			throw new IllegalArgumentException("不支持的协同区类型：" + dto.getAreaType());
		}
		if (!StringUtils.hasText(dto.getBindId())) {
			throw new IllegalArgumentException("绑定流程定义ID(bindId)不能为空");
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = currentTenant();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		LocalDateTime now = LocalDateTime.now();

		String nodeId = StringUtils.hasText(dto.getNodeId()) ? dto.getNodeId() : "";
		String formId = StringUtils.hasText(dto.getFormId()) ? dto.getFormId() : "";

		// 自然键定位：同租户同类型同流程同节点（仅 node 用 nodeId 区分）
		PortalSynergyArea row;
		if (dto.getId() != null) {
			row = synergyMapper.selectById(dto.getId());
			if (row == null || !tenantId.equals(row.getTenantId())) {
				throw new IllegalArgumentException("协同区不存在或无权限");
			}
		} else {
			row = synergyMapper.selectOne(new QueryWrapper<PortalSynergyArea>()
				.eq("tenant_id", tenantId).eq("area_type", dto.getAreaType())
				.eq("bind_id", dto.getBindId()).eq("node_id", nodeId).last("limit 1"));
		}
		boolean creating = (row == null);
		if (creating) {
			row = new PortalSynergyArea();
			row.setTenantId(tenantId);
			row.setCreateUser(userId);
			row.setCreateDept(deptId);
			row.setCreateTime(now);
		}
		row.setAreaType(dto.getAreaType());
		row.setTitle(dto.getTitle());
		row.setBindId(dto.getBindId());
		row.setBindName(dto.getBindName());
		row.setNodeId(nodeId);
		row.setNodeName(dto.getNodeName());
		row.setFormId(formId);
		row.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
		row.setWidth(dto.getWidth() == null ? 12 : dto.getWidth());
		row.setParamsJson(dto.getParamsJson());
		row.setIsDeleted(0);
		row.setStatus(1);
		row.setUpdateUser(userId);
		row.setUpdateTime(now);

		if (creating) {
			synergyMapper.insert(row);
		} else {
			synergyMapper.updateById(row);
		}
		logService.record(PortalConstant.LOG_SYNERGY_SAVE, "synergyArea", String.valueOf(row.getId()),
			dto.getTitle() == null ? row.getBindName() : dto.getTitle(),
			(creating ? "新增" : "更新") + "协同区 " + dto.getAreaType() + "（流程=" + dto.getBindName() + "）", null, null);
		return row;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalSynergyArea exist = synergyMapper.selectById(id);
		if (exist == null || Integer.valueOf(1).equals(exist.getIsDeleted())) {
			return false;
		}
		PortalSynergyArea gone = new PortalSynergyArea();
		gone.setId(id);
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		synergyMapper.updateById(gone);
		logService.record(PortalConstant.LOG_SYNERGY_DELETE, "synergyArea", String.valueOf(id),
			exist.getTitle(), "注销协同区 " + exist.getAreaType() + "（流程=" + exist.getBindName() + "）", null, null);
		return true;
	}

	@Override
	public Map<String, List<PortalSynergyArea>> tree() {
		List<PortalSynergyArea> all = synergyMapper.selectList(new QueryWrapper<PortalSynergyArea>()
			.eq("tenant_id", currentTenant()).eq("is_deleted", 0).orderByAsc("area_type", "id"));
		Map<String, List<PortalSynergyArea>> grouped = new LinkedHashMap<>();
		for (String type : AREA_TYPES) {
			grouped.put(type, new ArrayList<>());
		}
		for (PortalSynergyArea a : all) {
			grouped.computeIfAbsent(a.getAreaType(), k -> new ArrayList<>()).add(a);
		}
		return grouped;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setEnabled(Long id, Integer enabled) {
		return updateField(id, "enabled", (enabled == null || enabled == 0) ? 0 : 1, "启用/禁用");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setWidth(Long id, Integer width) {
		int w = (width == null || width < 1) ? 1 : Math.min(width, 24);
		return updateField(id, "width", w, "设置宽度");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveParams(Long id, String paramsJson) {
		return updateField(id, "params_json", paramsJson, "保存参数");
	}

	@Override
	public List<BrowserOptionVO> workflowOptions() {
		try {
			var r = wfSynergyClient.definitionOptions();
			return (r != null && r.isSuccess() && r.getData() != null) ? r.getData() : new ArrayList<>();
		} catch (Exception e) {
			log.warn("[portal] 拉取流程定义选项失败，msg={}", e.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public List<BrowserOptionVO> nodeOptions(Long defId) {
		if (defId == null) {
			return new ArrayList<>();
		}
		try {
			var r = wfSynergyClient.definitionNodes(defId);
			return (r != null && r.isSuccess() && r.getData() != null) ? r.getData() : new ArrayList<>();
		} catch (Exception e) {
			log.warn("[portal] 拉取流程节点选项失败，defId={}, msg={}", defId, e.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public List<FormFieldVO> wfFields(Long defId) {
		if (defId == null) {
			return new ArrayList<>();
		}
		try {
			var r = wfSynergyClient.definitionFields(defId);
			return (r != null && r.isSuccess() && r.getData() != null) ? r.getData() : new ArrayList<>();
		} catch (Exception e) {
			log.warn("[portal] 拉取流程表单字段失败，defId={}, msg={}", defId, e.getMessage());
			return new ArrayList<>();
		}
	}

	private boolean updateField(Long id, String col, Object val, String action) {
		if (id == null) {
			return false;
		}
		PortalSynergyArea exist = synergyMapper.selectById(id);
		if (exist == null || Integer.valueOf(1).equals(exist.getIsDeleted())) {
			return false;
		}
		PortalSynergyArea patch = new PortalSynergyArea();
		patch.setId(id);
		if ("enabled".equals(col)) {
			patch.setEnabled((Integer) val);
		} else if ("width".equals(col)) {
			patch.setWidth((Integer) val);
		} else if ("params_json".equals(col)) {
			patch.setParamsJson((String) val);
		}
		patch.setUpdateTime(LocalDateTime.now());
		synergyMapper.updateById(patch);
		logService.record(PortalConstant.LOG_SYNERGY_SAVE, "synergyArea", String.valueOf(id),
			exist.getTitle(), action + "协同区", null, null);
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
}
