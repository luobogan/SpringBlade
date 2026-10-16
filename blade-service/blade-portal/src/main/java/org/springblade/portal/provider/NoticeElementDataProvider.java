package org.springblade.portal.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.mapper.PortalDataMapper;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知公告元素 Provider
 * <p>
 * 数据源：直查 {@code blade_notice}（dev 多服务共用数据源）。
 * 不用 {@code INoticeClient} Feign 的原因：通知公告所在的 blade-desk 服务
 * 并未在 start-all.bat 启动，走 Feign 在开发环境必然降级；
 * 待 blade-desk 上线后可平滑切换为 Feign 调用。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeElementDataProvider implements IPortalElementDataProvider {

	private final PortalDataMapper dataMapper;

	@Override
	public String type() {
		return PortalConstant.ELEMENT_NOTICE;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());
		try {
			int limit = ctx.getLimit() > 0 ? ctx.getLimit() : PortalConstant.DEFAULT_PER_PAGE;
			// 组织过滤：租户始终过滤；部门/分部按 ctx 中的 filterDeptId / branchDeptIds 生效（§22 数据型元素按组织过滤）
			List<Map<String, Object>> list = dataMapper.selectNotices(
				limit, ctx.getTenantId(), ctx.getFilterDeptId(), ctx.getBranchDeptIds());
			vo.setData(list == null ? new ArrayList<>() : list);
		} catch (Exception e) {
			log.warn("[portal] 通知公告元素取数失败，msg={}", e.getMessage());
			vo.setData(new ArrayList<>());
			vo.setSuccess(Boolean.FALSE);
			vo.setMessage("通知公告加载失败");
		}
		return vo;
	}
}