package org.springblade.portal.service;

import org.springblade.portal.dto.PortalLayoutInstantiateDTO;
import org.springblade.portal.dto.PortalLayoutTemplateSaveDTO;
import org.springblade.portal.entity.PortalLayout;

import java.util.List;

/**
 * 门户布局模板服务（对标泛微 portallayout §12.5 P2）
 */
public interface IPortalLayoutService {

	/**
	 * 当前租户全部未删除布局模板。
	 */
	List<PortalLayout> list();

	/**
	 * 把某页面的元素排列（页面级 owner=0）存为模板。
	 *
	 * @return 模板ID；页面无元素或名称为空返回 null
	 */
	Long saveFromPage(PortalLayoutTemplateSaveDTO dto);

	/**
	 * 逻辑删除模板。
	 */
	boolean remove(Long layoutId);

	/**
	 * 从模板实例化一个新门户页（创建页面 + 复制元素），返回新页面ID。
	 */
	Long instantiate(PortalLayoutInstantiateDTO dto);
}
