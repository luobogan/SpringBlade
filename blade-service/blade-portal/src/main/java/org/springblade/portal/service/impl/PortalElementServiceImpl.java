package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalElementSaveDTO;
import org.springblade.portal.dto.PortalElementSettingSaveDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalElementSetting;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalElementMapper;
import org.springblade.portal.mapper.PortalElementSettingMapper;
import org.springblade.portal.service.IPortalElementService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 门户元素服务实现
 * <p>写操作（save/saveBatch/remove/saveSetting）经 {@link IPortalLogService} 埋点审计（§13.4）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalElementServiceImpl implements IPortalElementService {

	private final PortalElementMapper elementMapper;
	private final PortalElementSettingMapper settingMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalElement> listByPage(Long pageId) {
		if (pageId == null) {
			return new ArrayList<>();
		}
		return elementMapper.selectList(new QueryWrapper<PortalElement>()
			.eq("page_id", pageId)
			.eq("is_deleted", 0)
			.orderByAsc("row_no")
			.orderByAsc("sort_order")
			.orderByAsc("id"));
	}

	@Override
	public boolean save(PortalElementSaveDTO dto) {
		if (!valid(dto)) {
			return false;
		}
		PortalElement existing = (dto.getElementId() == null) ? null : elementMapper.selectById(dto.getElementId());
		PortalElement old = null;
		if (existing != null) {
			old = new PortalElement();
			BeanUtils.copyProperties(existing, old);
		}
		PortalElement element = build(dto, existing);
		boolean ok = element.getId() == null ? elementMapper.insert(element) > 0 : elementMapper.updateById(element) > 0;
		if (ok) {
			String summary = (old == null)
				? ("新增元素 " + element.getElementName())
				: ("编辑元素 " + element.getElementName() + "；" + elementDiff(old, element));
			logService.record(PortalConstant.LOG_ELEMENT_SAVE, "element", String.valueOf(element.getId()),
				element.getElementName(), summary, old == null ? null : toJson(old), toJson(element));
		}
		return ok;
	}

	/**
	 * 整页保存（设计器）：<b>差量更新</b>
	 *
	 * <p>为什么不是「逻辑删除旧行 + 全量插入」：
	 * 那样每次保存都会为该页新增一批元素行（历史行持续膨胀），
	 * 且元素 id 每次都变 → 按 element_id 关联的用户个性化设置（blade_portal_element_setting）全部失效。</p>
	 *
	 * <p>差量语义：
	 * <ol>
	 *   <li>带 elementId 且属于本页 → updateById 原地更新（id 稳定）；</li>
	 *   <li>无 elementId → 新增；</li>
	 *   <li>本页原有、本次未提交的 → 逻辑删除并清理其个性化设置。</li>
	 * </ol>
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean saveBatch(Long pageId, List<PortalElementSaveDTO> elements) {
		if (pageId == null) {
			return false;
		}
		// 1) 读出该页现有元素，作为「差量」的基准
		List<PortalElement> olds = elementMapper.selectList(
			new QueryWrapper<PortalElement>().eq("page_id", pageId).eq("is_deleted", 0));
		java.util.Map<Long, PortalElement> oldById = new java.util.LinkedHashMap<>();
		for (PortalElement old : olds) {
			oldById.put(old.getId(), old);
		}

		// 2) 逐项更新 / 新增
		java.util.Set<Long> keptIds = new java.util.HashSet<>();
		int created = 0;
		int sort = 0;
		if (elements != null) {
			for (PortalElementSaveDTO dto : elements) {
				sort++;
				if (!valid(dto)) {
					continue;
				}
				dto.setPageId(pageId);
				if (dto.getSortOrder() == null || dto.getSortOrder() <= 0) {
					dto.setSortOrder(sort);
				}
				if (dto.getRowNo() == null || dto.getRowNo() <= 0) {
					dto.setRowNo(1);
				}
				PortalElement existing = null;
				if (dto.getElementId() != null) {
					existing = oldById.get(dto.getElementId());
				}
				PortalElement element = build(dto, existing);
				if (element.getId() == null) {
					elementMapper.insert(element);
					created++;
				} else {
					keptIds.add(element.getId());
					elementMapper.updateById(element);
				}
			}
		}

		// 3) 本次未提交的旧元素 → 逻辑删除并清理个性化设置
		java.util.List<Long> removedIds = new ArrayList<>();
		for (Long oldId : oldById.keySet()) {
			if (!keptIds.contains(oldId)) {
				removedIds.add(oldId);
			}
		}
		if (!removedIds.isEmpty()) {
			PortalElement gone = new PortalElement();
			gone.setIsDeleted(1);
			gone.setStatus(-1);
			gone.setUpdateTime(LocalDateTime.now());
			elementMapper.update(gone, new QueryWrapper<PortalElement>().in("id", removedIds).eq("is_deleted", 0));

			PortalElementSetting settingGone = new PortalElementSetting();
			settingGone.setIsDeleted(1);
			settingGone.setStatus(-1);
			settingMapper.update(settingGone,
				new QueryWrapper<PortalElementSetting>().in("element_id", removedIds).eq("is_deleted", 0));
			log.info("[portal] 整页保存清理掉未提交元素，pageId={}, removed={}", pageId, removedIds.size());
		}

		logService.record(PortalConstant.LOG_ELEMENT_BATCH, "element", String.valueOf(pageId),
			"整页保存元素", "整页保存：新增 " + created + "、更新 " + keptIds.size() + "、删除 " + removedIds.size(),
			null, null);
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean remove(Long elementId) {
		if (elementId == null) {
			return false;
		}
		PortalElement element = elementMapper.selectById(elementId);
		if (element == null) {
			return false;
		}
		String beforeJson = toJson(element);
		String elementName = element.getElementName();
		element.setIsDeleted(1);
		element.setStatus(-1);
		element.setUpdateTime(LocalDateTime.now());
		elementMapper.updateById(element);

		PortalElementSetting settingGone = new PortalElementSetting();
		settingGone.setIsDeleted(1);
		settingGone.setStatus(-1);
		settingMapper.update(settingGone,
			new QueryWrapper<PortalElementSetting>().eq("element_id", elementId).eq("is_deleted", 0));

		logService.record(PortalConstant.LOG_ELEMENT_DELETE, "element", String.valueOf(elementId),
			elementName, "删除元素 " + elementName, beforeJson, null);
		return true;
	}

	@Override
	public Long addUserElement(Long userId, PortalElementSaveDTO dto) {
		if (userId == null || dto == null || dto.getPageId() == null
			|| dto.getElementType() == null || dto.getElementType().isBlank()) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();
		PortalElement element = new PortalElement();
		element.setTenantId(tenantId);
		element.setPageId(dto.getPageId());
		element.setOwnerUserId(userId);
		element.setElementType(dto.getElementType().trim());
		element.setElementName(dto.getElementName());
		element.setShowTitle(dto.getShowTitle() == null ? 1 : dto.getShowTitle());
		element.setRowNo(dto.getRowNo() == null || dto.getRowNo() <= 0 ? 1 : dto.getRowNo());
		element.setColSpan(dto.getColSpan() == null || dto.getColSpan() <= 0 ? 12 : dto.getColSpan());
		element.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
		element.setConfigJson(dto.getConfigJson());
		element.setStyleJson(dto.getStyleJson());
		element.setIsDeleted(0);
		element.setStatus(1);
		element.setCreateUser(userId);
		element.setCreateTime(now);
		elementMapper.insert(element);
		logService.record(PortalConstant.LOG_ELEMENT_SAVE, "element", String.valueOf(element.getId()),
			element.getElementName(), "用户添加私有元素 " + element.getElementName(), null, null);
		return element.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeUserElement(Long userId, Long elementId) {
		if (userId == null || elementId == null) {
			return false;
		}
		PortalElement element = elementMapper.selectById(elementId);
		// 仅允许删除本人私有元素（页面级共享元素 owner=0 不可经此端点删）
		if (element == null || !userId.equals(element.getOwnerUserId())) {
			return false;
		}
		String name = element.getElementName();
		element.setIsDeleted(1);
		element.setStatus(-1);
		element.setUpdateTime(LocalDateTime.now());
		elementMapper.updateById(element);
		PortalElementSetting gone = new PortalElementSetting();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		settingMapper.update(gone,
			new QueryWrapper<PortalElementSetting>().eq("element_id", elementId).eq("is_deleted", 0));
		logService.record(PortalConstant.LOG_ELEMENT_DELETE, "element", String.valueOf(elementId),
			name, "用户删除私有元素 " + name, null, null);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateUserElement(Long userId, PortalElementSaveDTO dto) {
		if (userId == null || dto == null || dto.getElementId() == null) {
			return false;
		}
		PortalElement element = elementMapper.selectById(dto.getElementId());
		// 仅允许编辑本人私有元素
		if (element == null || !userId.equals(element.getOwnerUserId())) {
			return false;
		}
		if (dto.getElementName() != null) {
			element.setElementName(dto.getElementName());
		}
		if (dto.getColSpan() != null && dto.getColSpan() > 0) {
			element.setColSpan(dto.getColSpan());
		}
		if (dto.getRowNo() != null && dto.getRowNo() > 0) {
			element.setRowNo(dto.getRowNo());
		}
		if (dto.getConfigJson() != null) {
			element.setConfigJson(dto.getConfigJson());
		}
		element.setUpdateUser(userId);
		element.setUpdateTime(LocalDateTime.now());
		elementMapper.updateById(element);
		logService.record(PortalConstant.LOG_ELEMENT_SAVE, "element", String.valueOf(element.getId()),
			element.getElementName(), "用户编辑私有元素 " + element.getElementName(), null, null);
		return true;
	}

	@Override
	public boolean saveSetting(PortalElementSettingSaveDTO dto) {
		if (dto == null || dto.getElementId() == null) {
			log.warn("[portal] 个性化设置入参缺少 elementId，已忽略");
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		if (userId == null) {
			log.warn("[portal] 未获取到登录用户，个性化设置不保存");
			return false;
		}

		// 查该用户是否已有此元素的设置（含逻辑删除行一并复活，避免主键/唯一冲突）
		PortalElementSetting exist = settingMapper.selectOne(
			new QueryWrapper<PortalElementSetting>()
				.eq("element_id", dto.getElementId())
				.eq("user_id", userId)
				.last("limit 1"));

		LocalDateTime now = LocalDateTime.now();
		boolean ok;
		if (exist == null) {
			PortalElementSetting setting = new PortalElementSetting();
			setting.setElementId(dto.getElementId());
			setting.setUserId(userId);
			setting.setPerPage(dto.getPerPage());
			setting.setShowTitle(dto.getShowTitle());
			setting.setStyleJson(dto.getStyleJson());
			setting.setIsDeleted(0);
			setting.setStatus(1);
			setting.setCreateTime(now);
			setting.setCreateUser(userId);
			ok = settingMapper.insert(setting) > 0;
		} else {
			// ⚠️ 必须用 UpdateWrapper 显式 set：MyBatis-Plus 的 updateById 默认忽略 null 字段，
			// 用户「清空条数回退到默认」时 per_page 需要被写成 NULL，否则会一直保留旧值。
			com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PortalElementSetting> uw =
				new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
			uw.set("per_page", dto.getPerPage())
				.set("show_title", dto.getShowTitle())
				// style_json：传 null 不改变（不在 sql 里 set 会保留旧值，故按 null 判断）；
				// 传空串表示清除个人样式覆盖（对标 hpElementSettingDetail 用户级样式可写）
				.set(dto.getStyleJson() != null, "style_json",
					dto.getStyleJson().isBlank() ? null : dto.getStyleJson())
				.set("is_deleted", 0)
				.set("status", 1)
				.set("update_time", now)
				.set("update_user", userId)
				.eq("id", exist.getId());
			ok = settingMapper.update(null, uw) >= 0;
		}
		if (ok) {
			logService.record(PortalConstant.LOG_ELEMENT_SETTING, "setting", String.valueOf(dto.getElementId()),
				"元素个性化设置", "perPage=" + dto.getPerPage() + ", showTitle=" + dto.getShowTitle()
					+ ", styleJson=" + (dto.getStyleJson() == null ? "(不变)" : (dto.getStyleJson().isBlank() ? "(清除)" : "(更新)")),
				null, null);
		}
		return ok;
	}

	/* ==================== 私有方法 ==================== */

	/**
	 * 实体转 JSON 快照（失败降级为 null）
	 */
	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 审计快照序列化失败：{}", e.getMessage());
			return null;
		}
	}

	/**
	 * 元素变更字段摘要（旧 -> 新）
	 */
	private String elementDiff(PortalElement old, PortalElement now) {
		if (old == null) {
			return "新增";
		}
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(old.getElementName(), now.getElementName())) {
			changed.add("elementName");
		}
		if (!Objects.equals(old.getElementType(), now.getElementType())) {
			changed.add("elementType");
		}
		if (!Objects.equals(old.getShowTitle(), now.getShowTitle())) {
			changed.add("showTitle");
		}
		if (!Objects.equals(old.getRowNo(), now.getRowNo())) {
			changed.add("rowNo");
		}
		if (!Objects.equals(old.getColSpan(), now.getColSpan())) {
			changed.add("colSpan");
		}
		if (!Objects.equals(old.getSortOrder(), now.getSortOrder())) {
			changed.add("sortOrder");
		}
		if (!Objects.equals(old.getConfigJson(), now.getConfigJson())) {
			changed.add("config");
		}
		if (!Objects.equals(old.getStyleJson(), now.getStyleJson())) {
			changed.add("style");
		}
		return changed.isEmpty() ? "无字段变化" : ("变更: " + String.join(", ", changed));
	}

	/**
	 * 入参校验：类型与页面必填，栅格宽度兜底
	 */
	private boolean valid(PortalElementSaveDTO dto) {
		if (dto == null || dto.getElementType() == null || dto.getElementType().isBlank()) {
			log.warn("[portal] 元素入参缺少 elementType，已跳过");
			return false;
		}
		if (dto.getColSpan() == null || dto.getColSpan() <= 0) {
			dto.setColSpan(12);
		}
		if (dto.getShowTitle() == null) {
			dto.setShowTitle(1);
		}
		return true;
	}

	/**
	 * DTO → 实体（existing 为空表示新增）
	 */
	private PortalElement build(PortalElementSaveDTO dto, PortalElement existing) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();

		PortalElement element = (existing == null) ? new PortalElement() : existing;
		boolean creating = element.getId() == null;
		if (creating) {
			element.setIsDeleted(0);
			element.setStatus(1);
			element.setCreateTime(LocalDateTime.now());
			element.setCreateUser(userId);
		} else {
			element.setUpdateTime(LocalDateTime.now());
			element.setUpdateUser(userId);
		}
		element.setPageId(dto.getPageId());
		element.setElementType(dto.getElementType().trim());
		element.setElementName(dto.getElementName());
		element.setShowTitle(dto.getShowTitle());
		element.setRowNo(dto.getRowNo() == null || dto.getRowNo() <= 0 ? 1 : dto.getRowNo());
		element.setColSpan(dto.getColSpan());
		element.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
		element.setConfigJson(dto.getConfigJson());
		element.setStyleJson(dto.getStyleJson());
		return element;
	}
}
