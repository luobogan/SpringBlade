/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.message.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.message.dto.MessageAttachmentDTO;
import org.springblade.message.dto.MessageSendDTO;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.constant.MessageConstant;
import org.springblade.message.entity.Message;
import org.springblade.message.entity.MessageAttachment;
import org.springblade.message.entity.MessageReadLog;
import org.springblade.message.entity.Session;
import org.springblade.message.entity.SessionMember;
import org.springblade.message.mapper.MessageAttachmentMapper;
import org.springblade.message.mapper.MessageReadLogMapper;
import org.springblade.message.mapper.SessionMapper;
import org.springblade.message.mapper.SessionMemberMapper;
import org.springblade.message.service.IMessageService;
import org.springblade.message.vo.MessageAttachmentVO;
import org.springblade.message.vo.MessageVO;
import org.springblade.message.vo.SessionUnreadVO;
import org.springblade.message.vo.UnreadCountVO;
import org.springblade.message.websocket.MessageRealtimePublisher;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息主体服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class MessageServiceImpl extends BaseServiceImpl<org.springblade.message.mapper.MessageMapper, Message> implements IMessageService {

	private final MessageAttachmentMapper messageAttachmentMapper;
	private final MessageReadLogMapper messageReadLogMapper;
	private final SessionMemberMapper sessionMemberMapper;
	private final SessionMapper sessionMapper;
	private final IUserClient userClient;
	private final MessageRealtimePublisher realtimePublisher;

	@Override
	public IPage<MessageVO> pageMessages(Long sessionId, Query query, BladeUser user) {
		return pageMessages(sessionId, query, user, false);
	}

	@Override
	public IPage<MessageVO> pageMessages(Long sessionId, Query query, BladeUser user, boolean desc) {
		var wrapper = Wrappers.<Message>lambdaQuery().eq(Message::getSessionId, sessionId);
		if (desc) {
			wrapper.orderByDesc(Message::getId);
		} else {
			wrapper.orderByAsc(Message::getId);
		}
		IPage<Message> page = page(Condition.getPage(query), wrapper);

		List<Long> messageIds = page.getRecords().stream().map(Message::getId).collect(Collectors.toList());
		List<Long> senderIds = page.getRecords().stream().map(Message::getSenderId).distinct().collect(Collectors.toList());

		// 发送人姓名/头像缓存（同一发送人只查询一次，避免重复 Feign 调用）
		Map<Long, User> userCache = new java.util.HashMap<>();
		for (Long senderId : senderIds) {
			userCache.put(senderId, resolveUser(senderId));
		}
		// 当前用户已读集合
		List<Long> readIds = messageIds.isEmpty() ? new ArrayList<>() :
			messageReadLogMapper.selectList(Wrappers.<MessageReadLog>lambdaQuery()
					.in(MessageReadLog::getMessageId, messageIds).eq(MessageReadLog::getUserId, user.getUserId()))
				.stream().map(MessageReadLog::getMessageId).collect(Collectors.toList());

		// 接收方（除当前用户外的会话成员）：用于「自己发出的消息」的已读/未读回执
		List<Long> receiverIds = sessionMemberMapper.selectList(
				Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, sessionId))
			.stream().map(SessionMember::getUserId)
			.filter(id -> !id.equals(user.getUserId()))
			.collect(Collectors.toList());
		final int receiverCount = receiverIds.size();
		final Map<Long, Long> receiptMap = (messageIds.isEmpty() || receiverIds.isEmpty()) ? new java.util.HashMap<>() :
			messageReadLogMapper.selectList(Wrappers.<MessageReadLog>lambdaQuery()
					.in(MessageReadLog::getMessageId, messageIds)
					.in(MessageReadLog::getUserId, receiverIds))
				.stream().collect(Collectors.groupingBy(MessageReadLog::getMessageId, Collectors.counting()));

		List<MessageVO> vos = page.getRecords().stream().map(message -> {
			MessageVO vo = BeanUtil.copyProperties(message, MessageVO.class);
			User sender = userCache.get(message.getSenderId());
			vo.setSenderName(sender == null ? null : sender.getName());
			vo.setSenderAvatar(sender == null ? null : sender.getAvatar());
			vo.setRead(readIds.contains(message.getId()));
			vo.setReceiverCount(receiverCount);
			vo.setReadCount(receiptMap.getOrDefault(message.getId(), 0L).intValue());
			List<MessageAttachment> attachments = messageAttachmentMapper.selectList(
				Wrappers.<MessageAttachment>lambdaQuery().eq(MessageAttachment::getMessageId, message.getId()));
			vo.setAttachments(attachments.stream().map(a -> BeanUtil.copyProperties(a, MessageAttachmentVO.class)).collect(Collectors.toList()));
			return vo;
		}).collect(Collectors.toList());

		IPage<MessageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		result.setRecords(vos);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean send(MessageSendDTO dto, BladeUser user) {
		Session session = sessionMapper.selectById(dto.getSessionId());
		if (session == null) {
			return false;
		}
		Message message = new Message();
		message.setSessionId(dto.getSessionId());
		message.setSenderId(user.getUserId());
		message.setContentType(dto.getContentType() == null ? 1 : dto.getContentType());
		message.setCategory(dto.getCategory() == null ? MessageConstant.CATEGORY_CHAT : dto.getCategory());
		message.setContent(dto.getContent());
		message.setQuoteMessageId(dto.getQuoteMessageId());
		message.setBizRefType(dto.getBizRefType());
		message.setBizRefId(dto.getBizRefId());
		message.setStatus(1);
		message.setTenantId(user.getTenantId());
		message.setCreateUser(user.getUserId());
		message.setCreateDept(Func.toLong(user.getDeptId()));
		save(message);

		if (Func.isNotEmpty(dto.getAttachments())) {
			for (MessageAttachmentDTO attachment : dto.getAttachments()) {
				MessageAttachment entity = new MessageAttachment();
				entity.setMessageId(message.getId());
				entity.setFileId(attachment.getFileId());
				entity.setFileName(attachment.getFileName());
				entity.setFileUrl(attachment.getFileUrl());
				entity.setFileSize(attachment.getFileSize());
				entity.setFileType(attachment.getFileType());
				entity.setTenantId(user.getTenantId());
				entity.setCreateUser(user.getUserId());
				entity.setCreateDept(Func.toLong(user.getDeptId()));
				entity.setStatus(1);
				entity.setIsDeleted(0);
				messageAttachmentMapper.insert(entity);
			}
		}

		// 更新会话摘要
		session.setLastMessage(buildSummary(dto));
		session.setLastTime(new Date());
		sessionMapper.updateById(session);

		// 接收人未读 +1（发送人不计未读）
		List<SessionMember> members = sessionMemberMapper.selectList(
			Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, session.getId()));
		List<Long> userIds = new ArrayList<>();
		for (SessionMember member : members) {
			userIds.add(member.getUserId());
			if (!member.getUserId().equals(user.getUserId())) {
				member.setUnreadCount(member.getUnreadCount() == null ? 1 : member.getUnreadCount() + 1);
				sessionMemberMapper.updateById(member);
			}
		}

		// 实时推送
		MessageVO pushVo = BeanUtil.copyProperties(message, MessageVO.class);
		User sender = resolveUser(user.getUserId());
		pushVo.setSenderName(sender == null ? null : sender.getName());
		pushVo.setSenderAvatar(sender == null ? null : sender.getAvatar());
		pushVo.setRead(false);
		// 刚发出：接收方均未读（接收方人数 = 成员数 - 发送人）
		pushVo.setReadCount(0);
		pushVo.setReceiverCount(Math.max(userIds.size() - 1, 0));
		pushVo.setAttachments(messageAttachmentMapper.selectList(
				Wrappers.<MessageAttachment>lambdaQuery().eq(MessageAttachment::getMessageId, message.getId()))
			.stream().map(a -> BeanUtil.copyProperties(a, MessageAttachmentVO.class)).collect(Collectors.toList()));
		realtimePublisher.publishNewMessage(session.getId(), user.getTenantId(), pushVo, userIds);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean sendNoticeToUsers(NoticeSendDTO dto) {
		if (dto == null || Func.isEmpty(dto.getUserIds()) || Func.isBlank(dto.getTenantId())) {
			return false;
		}
		for (Long userId : dto.getUserIds()) {
			if (userId == null) {
				continue;
			}
			// 系统 → 用户的「流程通知」走每人一条 type=3 通知会话，自动建/复用
			Session session = findOrCreateNoticeSession(userId, dto.getTenantId());
			Date now = new Date();
			Message message = new Message();
			message.setSessionId(session.getId());
			// 系统代发：无登录态，senderId=0（不存在对应 blade_user，前端展示为「系统」）
			message.setSenderId(MessageConstant.SENDER_SYSTEM);
			message.setContentType(dto.getContentType() == null ? 4 : dto.getContentType());
			message.setCategory(MessageConstant.CATEGORY_NOTICE);
			message.setContent(dto.getContent());
			message.setBizRefType(dto.getBizRefType());
			message.setBizRefId(dto.getBizRefId());
			message.setStatus(1);
			message.setTenantId(dto.getTenantId());
			message.setCreateUser(MessageConstant.SENDER_SYSTEM);
			save(message);

			// 会话摘要直接用通知文案（不经 buildSummary，保留完整标题）
			session.setLastMessage(dto.getContent());
			session.setLastTime(now);
			sessionMapper.updateById(session);

			// 通知会话成员=接收人本人（系统发送者不在成员内），全员未读 +1
			List<SessionMember> members = sessionMemberMapper.selectList(
				Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, session.getId()));
			List<Long> memberIds = new ArrayList<>();
			for (SessionMember member : members) {
				memberIds.add(member.getUserId());
				member.setUnreadCount(member.getUnreadCount() == null ? 1 : member.getUnreadCount() + 1);
				sessionMemberMapper.updateById(member);
			}

			// 实时推送：复用既有 NEW_MESSAGE/UNREAD Redis 链路，铃铛红点自动 +1
			MessageVO pushVo = BeanUtil.copyProperties(message, MessageVO.class);
			pushVo.setSenderName("系统");
			pushVo.setRead(false);
			pushVo.setReadCount(0);
			pushVo.setReceiverCount(memberIds.size());
			pushVo.setAttachments(new ArrayList<>());
			realtimePublisher.publishNewMessage(session.getId(), dto.getTenantId(), pushVo, memberIds);
		}
		return true;
	}

	/**
	 * 查找/创建用户的「系统通知会话」（type=3，每人每租户一条）
	 */
	private Session findOrCreateNoticeSession(Long userId, String tenantId) {
		List<SessionMember> myMembers = sessionMemberMapper.selectList(
			Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getUserId, userId));
		for (SessionMember member : myMembers) {
			Session session = sessionMapper.selectById(member.getSessionId());
			if (session != null
				&& MessageConstant.SESSION_TYPE_NOTICE.equals(session.getType())
				&& tenantId.equals(session.getTenantId())) {
				return session;
			}
		}
		Session session = new Session();
		session.setType(MessageConstant.SESSION_TYPE_NOTICE);
		session.setName("流程通知");
		session.setTenantId(tenantId);
		session.setCreateUser(MessageConstant.SENDER_SYSTEM);
		session.setStatus(1);
		session.setIsDeleted(0);
		sessionMapper.insert(session);

		SessionMember member = new SessionMember();
		member.setSessionId(session.getId());
		member.setUserId(userId);
		member.setUnreadCount(0);
		member.setPinned(0);
		member.setMute(0);
		member.setTenantId(tenantId);
		member.setCreateUser(MessageConstant.SENDER_SYSTEM);
		member.setStatus(1);
		member.setIsDeleted(0);
		sessionMemberMapper.insert(member);
		return session;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean markRead(Long sessionId, BladeUser user) {
		List<Message> messages = list(Wrappers.<Message>lambdaQuery().eq(Message::getSessionId, sessionId));
		if (Func.isNotEmpty(messages)) {
			List<Long> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());
			List<Long> readIds = messageReadLogMapper.selectList(Wrappers.<MessageReadLog>lambdaQuery()
					.in(MessageReadLog::getMessageId, messageIds).eq(MessageReadLog::getUserId, user.getUserId()))
				.stream().map(MessageReadLog::getMessageId).collect(Collectors.toList());
			for (Message message : messages) {
				if (!readIds.contains(message.getId())) {
					MessageReadLog log = new MessageReadLog();
					log.setMessageId(message.getId());
					log.setUserId(user.getUserId());
					log.setReadTime(new Date());
					log.setTenantId(user.getTenantId());
					log.setCreateUser(user.getUserId());
					log.setCreateDept(Func.toLong(user.getDeptId()));
					log.setStatus(1);
					log.setIsDeleted(0);
					// 幂等插入（INSERT IGNORE）：并发 markRead 的 check-then-insert 竞态下，
					// 唯一键 uk_blade_message_read_log_msg_user 冲突直接跳过，不再抛 Duplicate entry
					messageReadLogMapper.insertIgnore(log);
				}
			}
		}
		SessionMember member = sessionMemberMapper.selectOne(Wrappers.<SessionMember>lambdaQuery()
			.eq(SessionMember::getSessionId, sessionId).eq(SessionMember::getUserId, user.getUserId()));
		if (member != null) {
			member.setUnreadCount(0);
			sessionMemberMapper.updateById(member);
		}
		realtimePublisher.publishUnread(user.getUserId(), unreadCount(user.getUserId()));

		// 已读回执：通知会话内其他成员刷新自己发出消息的「已读/未读」标记
		List<Long> otherIds = sessionMemberMapper.selectList(
				Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, sessionId))
			.stream().map(SessionMember::getUserId)
			.filter(id -> !id.equals(user.getUserId()))
			.collect(Collectors.toList());
		realtimePublisher.publishRead(sessionId, user.getTenantId(), otherIds);
		return true;
	}

	@Override
	public UnreadCountVO unreadCount(BladeUser user) {
		return unreadCount(user.getUserId());
	}

	@Override
	public UnreadCountVO unreadCount(Long userId) {
		List<SessionMember> members = sessionMemberMapper.selectList(
			Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getUserId, userId));
		int total = members.stream().mapToInt(m -> m.getUnreadCount() == null ? 0 : m.getUnreadCount()).sum();
		List<SessionUnreadVO> sessionUnread = members.stream().map(m -> {
			SessionUnreadVO vo = new SessionUnreadVO();
			vo.setSessionId(m.getSessionId());
			vo.setUnreadCount(m.getUnreadCount() == null ? 0 : m.getUnreadCount());
			return vo;
		}).collect(Collectors.toList());
		UnreadCountVO vo = new UnreadCountVO();
		vo.setTotalUnread(total);
		vo.setSessionUnread(sessionUnread);
		return vo;
	}

	/**
	 * 解析用户信息（含头像），失败返回 null
	 */
	private User resolveUser(Long userId) {
		try {
			R<UserInfo> result = userClient.userInfo(userId);
			if (result != null && result.isSuccess() && result.getData() != null) {
				return result.getData().getUser();
			}
		} catch (Exception ignored) {
			// 用户信息解析失败不影响消息主流程
		}
		return null;
	}

	private String resolveUserName(Long userId) {
		User user = resolveUser(userId);
		return user == null ? null : user.getName();
	}

	private String buildSummary(MessageSendDTO dto) {
		Integer type = dto.getContentType() == null ? 1 : dto.getContentType();
		if (type == 4) {
			return "[流程]" + (dto.getBizRefType() == null ? "" : dto.getBizRefType());
		}
		if (type == 3) {
			if (Func.isNotEmpty(dto.getAttachments()) && dto.getAttachments().get(0) != null) {
				return "[附件]" + dto.getAttachments().get(0).getFileName();
			}
			return "[附件]";
		}
		String content = dto.getContent();
		if (content != null && content.length() > 50) {
			content = content.substring(0, 50);
		}
		return content;
	}

}
