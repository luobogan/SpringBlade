package org.springblade.portal.service;

import org.springblade.portal.dto.PortalElementSaveDTO;
import org.springblade.portal.dto.PortalElementSettingSaveDTO;
import org.springblade.portal.entity.PortalElement;

import java.util.List;

/**
 * 门户元素服务
 */
public interface IPortalElementService {

	/**
	 * 按页面查询元素（管理端）
	 *
	 * @param pageId 页面ID
	 * @return 元素列表（按行号、排序号）
	 */
	List<PortalElement> listByPage(Long pageId);

	/**
	 * 保存单个元素（新增/编辑）
	 *
	 * @param dto 元素入参
	 * @return 是否成功
	 */
	boolean save(PortalElementSaveDTO dto);

	/**
	 * 整页保存元素（设计器保存）
	 * <p>
	 * 语义：先把该页现有元素与其个性化设置**逻辑删除**，再按提交顺序全量插入新元素。
	 * 不做「按 id 差量更新」是为了规避主键复用/局部删除失序的问题（历史上 mode 表曾因
	 * 「逻辑删除旧行 + 带旧 id 插入」导致 Duplicate entry 主键冲突）。
	 * </p>
	 *
	 * @param pageId   页面ID
	 * @param elements 元素入参列表
	 * @return 是否成功
	 */
	boolean saveBatch(Long pageId, List<PortalElementSaveDTO> elements);

	/**
	 * 逻辑删除元素（同时清理其个性化设置）
	 *
	 * @param elementId 元素ID
	 * @return 是否成功
	 */
	boolean remove(Long elementId);

	/**
	 * 保存当前用户对某个元素的个性化设置（upsert）
	 *
	 * <p>userId 取当前登录人（不接受入参传入，防越权）；已有记录则更新，否则新增。
	 * 对标泛微 hpElementSettingDetail 的「元素定义 + 用户明细覆盖」两层模型。</p>
	 *
	 * @param dto 个性化设置入参
	 * @return 是否成功
	 */
	boolean saveSetting(PortalElementSettingSaveDTO dto);

	/**
	 * 终端用户添加私有元素到「我的门户」（对标 UserAddElementInfo，仅本人可见）。
	 *
	 * @param userId 登录用户ID（内部取，防越权）
	 * @param dto    元素入参（elementType/pageId/configJson 等）
	 * @return 新元素ID；入参非法返回 null
	 */
	Long addUserElement(Long userId, PortalElementSaveDTO dto);

	/**
	 * 终端用户删除自己添加的私有元素（仅允许删 ownerUserId=本人 的元素）。
	 *
	 * @param userId    登录用户ID
	 * @param elementId 元素ID
	 * @return 是否成功（非本人私有元素返回 false）
	 */
	boolean removeUserElement(Long userId, Long elementId);

	/**
	 * 终端用户编辑自己添加的私有元素（标题/宽度/行号/配置；仅允许改 ownerUserId=本人 的元素）。
	 *
	 * @param userId 登录用户ID
	 * @param dto    元素入参（elementId 必填）
	 * @return 是否成功（非本人私有元素返回 false）
	 */
	boolean updateUserElement(Long userId, PortalElementSaveDTO dto);
}