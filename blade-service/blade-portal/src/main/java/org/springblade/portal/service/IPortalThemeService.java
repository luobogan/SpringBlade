package org.springblade.portal.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.portal.dto.PortalThemeSaveDTO;
import org.springblade.portal.entity.PortalTheme;
import org.springblade.portal.vo.ThemeUserVO;
import org.springblade.portal.vo.ThemeVO;
import org.springblade.portal.vo.UserThemeInfoVO;

import java.util.List;

/**
 * 门户主题服务
 * <p>对标泛微 themestyle（主题库）/ themecenter（主题个性化中心）/ themeappoint（批量指定）
 * / themesearch（使用查询）——四件套（§15.5 A1-A4）。</p>
 */
public interface IPortalThemeService {

	/**
	 * 主题列表（当前租户全部未删除）
	 */
	List<PortalTheme> list();

	/**
	 * 保存主题（新增/编辑）
	 *
	 * @return 新主题ID；编码冲突或入参非法返回 null
	 */
	Long save(PortalThemeSaveDTO dto);

	/**
	 * 设为租户默认主题（同一租户仅一个）
	 */
	boolean setDefault(Long themeId);

	/**
	 * 逻辑删除主题（默认主题不可删）；同步清理用户偏好指向
	 */
	boolean remove(Long themeId);

	/**
	 * 获取用户主题偏好（主题ID），无则返回 null
	 */
	Long getUserThemeId(Long userId);

	/**
	 * 设置用户主题偏好（覆盖）
	 */
	boolean setUserTheme(Long userId, Long themeId);

	/**
	 * 解析当前生效主题
	 * <pre>优先级：用户偏好 &gt; 页面 theme_key &gt; 定时生效主题(openTimer 且在当前时段内)
	 *       &gt; 租户默认主题 &gt; null</pre>
	 * <p>定时切换为「拉模式」：请求时按当前时间比对 openTimer/timerStart/timerEnd，
	 * 无调度器（对标泛微 GetMyThemeCmd.getUserTimerThemeInfo §18.2②）。</p>
	 */
	ThemeVO resolveTheme(String pageThemeKey, Long userId, String tenantId);

	/* ==================== 批量指定（themeappoint §15.5 A3） ==================== */

	/**
	 * 按人员范围批量指定主题（对标 setAppointThemeOfPersonnelScope）。
	 * <p>把主题写为范围内所有用户的显式偏好（覆盖用户此前的自选/旧指定）。</p>
	 *
	 * @param scopeType 1全员 2部门 3个人 4角色（分部暂不支持）
	 * @param scopeIds  范围对象ID（部门/个人/角色）；scopeType=1 时可空
	 * @return 实际生效用户数；主题不存在或范围非法返回 -1
	 */
	int appointByScope(Long themeId, int scopeType, List<Long> scopeIds);

	/**
	 * 按主题整体迁移（对标 setAppointThemeOfThemeId）：
	 * 把「源主题」的全部显式偏好用户迁移到「目标主题」。
	 *
	 * @return 迁移用户数；主题不存在返回 -1
	 */
	int migrateTheme(Long fromThemeId, Long toThemeId);

	/* ==================== 使用查询（themesearch §15.5 A4） ==================== */

	/**
	 * 反查主题使用人员（对标 getAllHrmOfTheme）：分页返回显式偏好该主题的用户。
	 */
	IPage<ThemeUserVO> listThemeUsers(Long themeId, long current, long size);

	/**
	 * 查人员当前主题（对标 getAllThemesOfUserId）：返回显式偏好 + 生效主题名称。
	 */
	UserThemeInfoVO getUserThemeInfo(Long userId);
}
