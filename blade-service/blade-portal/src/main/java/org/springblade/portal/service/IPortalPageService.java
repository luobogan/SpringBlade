package org.springblade.portal.service;

import org.springblade.portal.dto.PortalPageSaveDTO;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.entity.PortalPageShare;
import org.springblade.portal.vo.MyPortalVO;
import org.springblade.portal.vo.PortalPageExportVO;
import org.springblade.portal.vo.PortalPageStatVO;

import java.util.List;

/**
 * 门户页面服务
 */
public interface IPortalPageService {

	/**
	 * 页面列表（管理端）
	 *
	 * @return 全部未删除页面
	 */
	List<PortalPage> list();

	/**
	 * 我的门户聚合
	 * <p>
	 * 可见性规则（对标泛微元素共享 sharelevel）：
	 * 默认页（is_default=1）对所有人可见；其余页面需命中授权
	 * （1=全员 / 2=本部门 / 3=本人）。全部不命中时回退默认页，无默认页则返回空结果。
	 * </p>
	 *
	 * @param portalCode 页面编码（为空取默认页）
	 * @return 页面 + 元素配置（已合并当前用户个性化）
	 */
	MyPortalVO getMyPortal(String portalCode);

	/**
	 * 访问趋势：最近 days 天每日聚合 PV（缺失日期补 0）
	 */
	List<PortalPageStatVO> dailyStats(int days);

	/**
	 * 热门门户页：本租户访问量 Top N
	 */
	List<PortalPage> topPages(int limit);

	/**
	 * 保存页面（新增/编辑）
	 *
	 * @param dto 页面入参
	 * @return 是否成功
	 */
	boolean save(PortalPageSaveDTO dto);

	/**
	 * 设为默认门户（同一时刻仅一个默认页）
	 *
	 * @param pageId 页面ID
	 * @return 是否成功
	 */
	boolean setDefault(Long pageId);

	/**
	 * 逻辑删除页面（级联逻辑删除其元素、授权与个性化）
	 *
	 * @param pageId 页面ID
	 * @return 是否成功
	 */
	boolean remove(Long pageId);

	/**
	 * 导出页面完整配置（对标泛微 portalimport）
	 *
	 * @param pageId 页面ID
	 * @return 页面 + 元素 + 授权；页面不存在或已删除返回 null
	 */
	PortalPageExportVO exportPage(Long pageId);

	/**
	 * 导入页面完整配置为新页面（自动生成唯一编码、不抢占默认页）
	 *
	 * @param vo 导出 VO（page/elements/shares）
	 * @return 新页面ID
	 */
	Long importPage(PortalPageExportVO vo);

	/**
	 * 复制页面（页面 + 元素 + 授权）为新页面（编码自动加后缀去重、不抢占默认页）。
	 *
	 * @param pageId 源页面ID
	 * @return 新页面ID；源页面不存在返回 null
	 */
	Long duplicate(Long pageId);

	/**
	 * 登录前门户（免登录）：返回 page_type=0 的门户页 + 页面级共享元素（无个性化、无用户私有元素）。
	 *
	 * @param portalCode 页面编码（为空取第一个登录前页）
	 * @return 门户 VO；无登录前页返回空 VO
	 */
	MyPortalVO getPreLoginPortal(String portalCode);

	/**
	 * 查询页面可见范围（授权）列表（对标泛微元素共享 elementshareinfo）。
	 *
	 * @param pageId 页面ID
	 * @return 该页全部未删除授权
	 */
	List<PortalPageShare> listShare(Long pageId);

	/**
	 * 保存页面可见范围（差量：按 share_type+share_id 增删）。
	 * <p>全员(1)唯一；部门(2)/个人(3)可多条。传空列表=清空授权。</p>
	 *
	 * @param pageId  页面ID
	 * @param shares  目标授权集合
	 * @return 是否成功
	 */
	boolean saveShare(Long pageId, List<PortalPageShare> shares);
}