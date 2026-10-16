package org.springblade.portal.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.message.feign.IMessageClient;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息提醒元素 Provider
 *
 * <p>数据源：消息服务内部端点
 * {@code GET /feign/client/message/unread-count} 与 {@code GET /feign/client/message/recent}
 * （分别对应 {@code IMessageClient#unreadCount} / {@code IMessageClient#recent}）。</p>
 *
 * <p>返回结构：rows[0] = 汇总行（unread 未读数 + url 入口）；其余行 = 最近消息摘要
 * （id/sessionId/senderId/category/content/createTime）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageElementDataProvider implements IPortalElementDataProvider {

	private final IMessageClient messageClient;

	@Override
	public String type() {
		return PortalConstant.ELEMENT_MESSAGE;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());
		List<Map<String, Object>> rows = new ArrayList<>();
		vo.setData(rows);
		try {
			if (ctx.getUserId() == null) {
				vo.setMessage("未获取到登录用户");
				return vo;
			}

			// 1) 未读红点
			long unread = 0L;
			try {
				R<Long> countResult = messageClient.unreadCount(ctx.getUserId());
				if (countResult != null && countResult.isSuccess() && countResult.getData() != null) {
					unread = countResult.getData();
				}
			} catch (Exception e) {
				log.warn("[portal] 消息未读数获取失败，msg={}", e.getMessage());
			}

			Map<String, Object> summary = new LinkedHashMap<>();
			summary.put("unread", unread);
			summary.put("url", "/message");
			rows.add(summary);

			// 2) 最近消息（失败不影响汇总行）
			try {
				R<List<Map<String, Object>>> recentResult =
					messageClient.recent(ctx.getUserId(), ctx.getLimit());
				if (recentResult != null && recentResult.isSuccess() && recentResult.getData() != null) {
					rows.addAll(recentResult.getData());
				}
			} catch (Exception e) {
				log.warn("[portal] 最近消息获取失败，msg={}", e.getMessage());
			}
		} catch (Exception e) {
			log.warn("[portal] 消息元素取数失败，msg={}", e.getMessage());
			vo.setSuccess(Boolean.FALSE);
			vo.setMessage("消息加载失败：消息服务不可用");
		}
		return vo;
	}
}
