package org.springblade.message.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.message.mapper.MessageMapper;
import org.springblade.message.service.IMessageService;
import org.springblade.message.vo.UnreadCountVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息中心「内部 Feign 端点」实现（供 blade-portal 门户消息元素取数）。
 *
 * <p>不复用 {@code GET /message/message/unread-count}：那个入口在
 * {@code MessageController}（依赖登录态参数 {@code BladeUser user}），
 * 内部 Feign 经服务发现直连、不带用户令牌。本项目既有约定是
 * <b>内部 Feign 端点不加 {@code @PreAuth}</b>（参照 blade-workflow 的 {@code WfInstanceClient}），
 * 路径用 {@code /feign/client} 前缀隔离，网关 {@code InnerFilter} 拒绝经网关的外部访问。</p>
 *
 * <p>{@code @Hidden}：仅服务间调用，不暴露到接口文档。</p>
 */
@Hidden
@RestController
@RequestMapping("/feign/client/message")
@RequiredArgsConstructor
public class MessageClient {

	private final IMessageService messageService;
	private final MessageMapper messageMapper;

	/**
	 * 查询未读红点总数（对应 Feign 契约 {@code IMessageClient#unreadCount}）
	 *
	 * @param userId 用户ID（调用方显式传入，不依赖请求头令牌）
	 * @return 未读总数
	 */
	@GetMapping("/unread-count")
	public R<Long> unreadCount(@RequestParam("userId") Long userId) {
		if (userId == null) {
			return R.fail("用户ID不能为空");
		}
		UnreadCountVO vo = messageService.unreadCount(userId);
		Integer total = (vo == null) ? null : vo.getTotalUnread();
		return R.data(total == null ? 0L : total.longValue());
	}

	/**
	 * 查询某个用户的最近消息（对应 Feign 契约 {@code IMessageClient#recent}）
	 *
	 * @param userId 用户ID
	 * @param limit  条数（默认 8，上限 50）
	 * @return 消息摘要列表
	 */
	@GetMapping("/recent")
	public R<List<Map<String, Object>>> recent(
		@RequestParam("userId") Long userId,
		@RequestParam(value = "limit", required = false) Integer limit) {
		if (userId == null) {
			return R.fail("用户ID不能为空");
		}
		int size = (limit == null || limit <= 0) ? 8 : Math.min(limit, 50);
		try {
			return R.data(messageMapper.selectRecentByUser(userId, size));
		} catch (Exception e) {
			// 门户元素数据失败不应影响整页，返回空列表而非抛错
			return R.data(new ArrayList<>());
		}
	}
}