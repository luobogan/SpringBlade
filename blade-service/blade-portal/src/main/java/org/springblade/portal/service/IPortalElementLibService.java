package org.springblade.portal.service;

import org.springblade.portal.dto.PortalElementLibInstantiateDTO;
import org.springblade.portal.dto.PortalElementLibSaveDTO;
import org.springblade.portal.entity.PortalElementLib;
import org.springblade.portal.vo.PortalPageBrief;

import java.util.List;

/**
 * 门户独立元素库服务（对标 hpBaseElement / HPELEMENTTEMPLATE，§12.5 P3）
 */
public interface IPortalElementLibService {

	/**
	 * 当前租户全部未删除库元素（含封存）。
	 */
	List<PortalElementLib> list();

	/**
	 * 保存库元素（新增/编辑）。
	 *
	 * @return 库元素ID；名称为空返回 null
	 */
	Long save(PortalElementLibSaveDTO dto);

	/**
	 * 封存 / 解封库元素（封存后不可再被新引用，既有引用继续有效）。
	 */
	boolean setArchived(Long libId, boolean archived);

	/**
	 * 逻辑删除库元素；被页面引用时禁止删除。
	 */
	boolean remove(Long libId);

	/**
	 * 引用该库元素的页面列表（被哪些页面实例化引用）。
	 */
	List<PortalPageBrief> references(Long libId);

	/**
	 * 把库元素实例化到指定页面（创建带 lib_id 的页面元素），返回新元素ID。
	 * 库元素已封存返回 null。
	 */
	Long instantiate(PortalElementLibInstantiateDTO dto);
}
