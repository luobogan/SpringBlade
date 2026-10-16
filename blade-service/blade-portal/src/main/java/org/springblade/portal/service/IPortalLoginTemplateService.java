package org.springblade.portal.service;

import org.springblade.portal.dto.PortalLoginTemplateSaveDTO;
import org.springblade.portal.entity.PortalLoginTemplate;
import org.springblade.portal.vo.PortalLoginTemplateVO;

import java.util.List;

/**
 * 登录前门户皮肤服务（对标泛微 SystemLoginTemplate §16.1）
 */
public interface IPortalLoginTemplateService {

	/**
	 * 当前租户全部未删除皮肤模板。
	 */
	List<PortalLoginTemplate> list();

	/**
	 * 保存皮肤模板（新增/编辑）。
	 *
	 * @return 模板ID；名称重复返回 null
	 */
	Long save(PortalLoginTemplateSaveDTO dto);

	/**
	 * 设为当前启用（租户内唯一）。
	 */
	boolean setCurrent(Long templateId);

	/**
	 * 逻辑删除（当前启用中的不可删）。
	 */
	boolean remove(Long templateId);

	/**
	 * 解析当前应生效的皮肤（供登录页，免鉴权）：
	 * 定时生效(open_timer=1 且当前在 [start,end]) 优先，否则当前启用(is_current=1)，再否则第一个。
	 */
	PortalLoginTemplateVO resolveCurrent();
}
