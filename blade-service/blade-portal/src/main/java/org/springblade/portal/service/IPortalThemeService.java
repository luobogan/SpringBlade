package org.springblade.portal.service;

import org.springblade.portal.dto.PortalThemeSaveDTO;
import org.springblade.portal.entity.PortalTheme;
import org.springblade.portal.vo.ThemeVO;

import java.util.List;

/**
 * 门户主题服务
 * <p>对标泛微 themestyle（主题库）/ themecenter（主题个性化中心）</p>
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
}
