package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalLayoutItem;
import org.springblade.portal.dto.PortalLayoutSaveDTO;
import org.springblade.portal.dto.PortalPageSaveDTO;
import org.springblade.portal.entity.PortalDefault;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.entity.PortalPageShare;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalDefaultService;
import org.springblade.portal.service.IPortalPageService;
import org.springblade.portal.service.IPortalUserDefaultService;
import org.springblade.portal.service.IPortalUserLayoutService;
import org.springblade.portal.vo.MyPortalVO;
import org.springblade.portal.vo.PortalPageExportVO;
import org.springblade.portal.vo.PortalPageStatVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户页面控制器
 * <p>
 * 前端经网关以 {@code /api/blade-portal/**} 访问。
 * 写接口采用「HAS_AUTH（已登录）+ PortalRightUtil.check(权限码)」双层门禁，无权限抛
 * {@link PortalAccessDeniedException} → HTTP 403（§13.4）。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "门户页面", description = "我的门户聚合与页面配置维护")
public class PortalPageController {

	private final IPortalPageService pageService;
	private final IPortalDefaultService defaultService;
	private final IPortalUserDefaultService userDefaultService;
	private final IPortalUserLayoutService userLayoutService;
	private final PortalRightUtil portalRightUtil;

	@GetMapping("/my-portal")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "我的门户", description = "返回当前用户可见的门户页面与元素配置（已合并用户个性化）")
	public R<MyPortalVO> myPortal(
		@Parameter(description = "页面编码，不传则取默认页") @RequestParam(required = false) String portalCode) {
		return R.data(pageService.getMyPortal(portalCode));
	}

	@GetMapping("/page/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "门户页面列表", description = "管理端：全部未删除页面")
	public R<List<PortalPage>> list() {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.list());
	}

	@PostMapping("/page/submit")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存门户页面", description = "新增/编辑；pageId 为空视为新增")
	public R<Boolean> submit(@RequestBody PortalPageSaveDTO dto) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.status(pageService.save(dto));
	}

	@PostMapping("/page/default")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为默认门户", description = "全局默认页，同一时刻仅允许一个")
	public R<Boolean> setDefault(@RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_PAGE_DEFAULT);
		return R.status(pageService.setDefault(pageId));
	}

	@PostMapping("/page/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "删除门户页面", description = "逻辑删除页面并级联逻辑删除其元素、授权与个性化")
	public R<Boolean> remove(@RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.status(pageService.remove(pageId));
	}

	@GetMapping("/page/export")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "导出门户页面", description = "导出页面+元素+授权的完整配置（对标泛微 portalimport）")
	public R<PortalPageExportVO> exportPage(@Parameter(description = "页面ID") @RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.exportPage(pageId));
	}

	@PostMapping("/page/import")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "导入门户页面", description = "导入页面+元素+授权为新页面（自动唯一编码、不抢默认页），返回新页面ID")
	public R<Long> importPage(@RequestBody PortalPageExportVO vo) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.importPage(vo));
	}

	@GetMapping("/page/default/org/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "机构默认首页列表", description = "全部机构默认首页配置（对标泛微 defaultportal）")
	public R<List<PortalDefault>> orgDefaultList() {
		requireRight(PortalConstant.PERM_PAGE_DEFAULT);
		return R.data(defaultService.list());
	}

	@PostMapping("/page/default/org/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设置机构默认首页", description = "按机构(deptId)指定默认门户页面")
	public R<Boolean> orgDefaultSet(
		@Parameter(description = "机构/部门ID") @RequestParam Long deptId,
		@Parameter(description = "门户页面ID") @RequestParam Long pageId,
		@Parameter(description = "备注") @RequestParam(required = false) String remark) {
		requireRight(PortalConstant.PERM_PAGE_DEFAULT);
		return R.status(defaultService.set(deptId, pageId, remark));
	}

	@PostMapping("/page/default/org/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "清除机构默认首页", description = "按机构(deptId)清除默认首页")
	public R<Boolean> orgDefaultRemove(@RequestParam Long deptId) {
		requireRight(PortalConstant.PERM_PAGE_DEFAULT);
		return R.status(defaultService.remove(deptId));
	}

	@PostMapping("/page/duplicate")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "复制页面", description = "复制页面+元素+授权为新页面（编码自动去重、不抢占默认页），返回新页面ID")
	public R<Long> duplicate(@Parameter(description = "源页面ID") @RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.duplicate(pageId));
	}

	/* ==================== 页面可见范围（对标 elementshareinfo，§22） ==================== */

	@GetMapping("/page/share/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "页面可见范围", description = "查询页面授权（1全员/2部门/3个人）")
	public R<List<PortalPageShare>> shareList(@Parameter(description = "页面ID") @RequestParam Long pageId) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.listShare(pageId));
	}

	@PostMapping("/page/share/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存页面可见范围", description = "差量增删授权；传空数组=清空授权")
	public R<Boolean> shareSet(
		@Parameter(description = "页面ID") @RequestParam Long pageId,
		@RequestBody(required = false) List<PortalPageShare> shares) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.status(pageService.saveShare(pageId, shares));
	}

	/* ==================== 个人默认门户（对标 hpcurrentuse，§18.2/§22） ==================== */

	@GetMapping("/page/default/user")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "我的默认门户", description = "当前用户个人默认首页ID（无则 null）")
	public R<Long> userDefault() {
		BladeUser user = SecureUtil.getUser();
		return R.data(userDefaultService.getUserDefaultPageId(user == null ? null : user.getUserId()));
	}

	@PostMapping("/page/default/user/set")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设为我的默认门户", description = "覆盖当前用户个人默认首页（landing 时优先于机构/全局默认）")
	public R<Boolean> userDefaultSet(@Parameter(description = "门户页面ID") @RequestParam Long pageId) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(userDefaultService.set(userId, pageId));
	}

	@PostMapping("/page/default/user/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "清除我的默认门户", description = "清除当前用户个人默认首页")
	public R<Boolean> userDefaultRemove() {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(userDefaultService.remove(userId));
	}

	/* ==================== 个人门户布局（对标 hpcurrentuse，§18.2） ==================== */

	@GetMapping("/my-portal/layout")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "我的门户布局", description = "当前用户对指定门户页的元素排列覆盖（无则空列表）")
	public R<List<PortalLayoutItem>> myLayout(@Parameter(description = "页面ID") @RequestParam Long pageId) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.data(userLayoutService.getLayout(userId, pageId));
	}

	@PostMapping("/my-portal/layout/save")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存我的门户布局", description = "按用户保存元素排列（排序/行/栅格/显隐）")
	public R<Boolean> saveMyLayout(@RequestBody PortalLayoutSaveDTO dto) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(userLayoutService.save(userId, dto.getPageId(), dto.getItems()));
	}

	@PostMapping("/my-portal/layout/reset")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "重置我的门户布局", description = "清除个性化，恢复创建者默认布局")
	public R<Boolean> resetMyLayout(@Parameter(description = "页面ID") @RequestParam Long pageId) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		return R.status(userLayoutService.reset(userId, pageId));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行门户维护操作");
		}
	}

	/* ==================== 访问分析（热度/趋势，§22 门户使用统计） ==================== */

	@GetMapping("/page/stats/daily")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "门户访问趋势", description = "按日聚合的访问量（PV），最近 days 天（缺失日期补0）")
	public R<List<PortalPageStatVO>> dailyStats(
		@Parameter(description = "天数，1-180") @RequestParam(defaultValue = "30") int days) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.dailyStats(days));
	}

	@GetMapping("/page/stats/top")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "热门门户页", description = "本租户访问量最高的页面 Top N")
	public R<List<PortalPage>> topPages(
		@Parameter(description = "条数，1-50") @RequestParam(defaultValue = "10") int limit) {
		requireRight(PortalConstant.PERM_PAGE_MAINT);
		return R.data(pageService.topPages(limit));
	}
}
