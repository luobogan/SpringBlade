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
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.message.dto.SessionCreateDTO;
import org.springblade.message.entity.Session;
import org.springblade.message.entity.SessionMember;
import org.springblade.message.mapper.SessionMapper;
import org.springblade.message.mapper.SessionMemberMapper;
import org.springblade.message.service.ISessionService;
import org.springblade.message.vo.SessionVO;
import org.springblade.message.wrapper.SessionWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息会话服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class SessionServiceImpl extends BaseServiceImpl<SessionMapper, Session> implements ISessionService {

	private final SessionMemberMapper sessionMemberMapper;

	@Override
	public IPage<SessionVO> pageSessions(Query query, BladeUser user) {
		List<SessionMember> myMembers = sessionMemberMapper.selectList(
			Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getUserId, user.getUserId()));
		if (Func.isEmpty(myMembers)) {
			// 不能直接取 query.getSize()/getCurrent()：前端未传 size 时为 null，自动拆箱会 NPE。
			// 走 Condition 的分页装配，与下方正常分支保持一致并可获得默认分页值。
			IPage<Session> emptyPage = Condition.getPage(query);
			return new Page<>(emptyPage.getCurrent(), emptyPage.getSize(), 0L);
		}
		List<Long> sessionIds = myMembers.stream().map(SessionMember::getSessionId).distinct().collect(Collectors.toList());
		Map<Long, Integer> unreadMap = myMembers.stream()
			.collect(Collectors.toMap(SessionMember::getSessionId, SessionMember::getUnreadCount, (a, b) -> a));

		IPage<Session> page = page(Condition.getPage(query),
			Wrappers.<Session>lambdaQuery().in(Session::getId, sessionIds).orderByDesc(Session::getLastTime));

		List<SessionVO> vos = page.getRecords().stream().map(session -> {
			SessionVO vo = SessionWrapper.build().entityVO(session);
			vo.setUnreadCount(unreadMap.getOrDefault(session.getId(), 0));
			List<SessionMember> members = sessionMemberMapper.selectList(
				Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, session.getId()));
			vo.setMemberCount(members.size());
			vo.setMemberIds(members.stream().map(SessionMember::getUserId).collect(Collectors.toList()));
			return vo;
		}).collect(Collectors.toList());

		IPage<SessionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
		result.setRecords(vos);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SessionVO createSession(SessionCreateDTO dto, BladeUser user) {
		List<Long> memberIds = new ArrayList<>(dto.getMemberIds());
		if (Func.isEmpty(memberIds)) {
			throw new IllegalArgumentException("会话成员不能为空");
		}
		// 当前用户必须包含在会话内
		if (!memberIds.contains(user.getUserId())) {
			memberIds.add(user.getUserId());
		}
		// 两人会话幂等复用
		if (memberIds.size() == 2) {
			SessionVO exist = findTwoPersonSession(memberIds);
			if (exist != null) {
				return exist;
			}
		}
		Session session = new Session();
		session.setType(memberIds.size() == 2 ? 1 : 2);
		session.setName(dto.getName());
		session.setTenantId(user.getTenantId());
		session.setCreateUser(user.getUserId());
		session.setCreateDept(Func.toLong(user.getDeptId()));
		save(session);

		for (Long memberId : memberIds) {
			SessionMember member = new SessionMember();
			member.setSessionId(session.getId());
			member.setUserId(memberId);
			member.setUnreadCount(0);
			member.setPinned(0);
			member.setMute(0);
			member.setTenantId(user.getTenantId());
			member.setCreateUser(user.getUserId());
			member.setCreateDept(Func.toLong(user.getDeptId()));
			// 直接走 mapper.insert 不经过 BaseServiceImpl.resolveSave，需显式填充逻辑删除/状态，
			// 否则 is_deleted 为 NULL，@TableLogic 查询会过滤掉该成员导致未读红点失效。
			member.setStatus(1);
			member.setIsDeleted(0);
			sessionMemberMapper.insert(member);
		}

		SessionVO vo = SessionWrapper.build().entityVO(session);
		vo.setUnreadCount(0);
		vo.setMemberCount(memberIds.size());
		vo.setMemberIds(memberIds);
		return vo;
	}

	private SessionVO findTwoPersonSession(List<Long> memberIds) {
		Long a = memberIds.get(0);
		Long b = memberIds.get(1);
		List<SessionMember> aMembers = sessionMemberMapper.selectList(
			Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getUserId, a));
		for (SessionMember aMember : aMembers) {
			List<SessionMember> members = sessionMemberMapper.selectList(
				Wrappers.<SessionMember>lambdaQuery().eq(SessionMember::getSessionId, aMember.getSessionId()));
			if (members.size() != 2) {
				continue;
			}
			boolean hasA = false;
			boolean hasB = false;
			for (SessionMember m : members) {
				if (m.getUserId().equals(a)) {
					hasA = true;
				} else if (m.getUserId().equals(b)) {
					hasB = true;
				}
			}
			if (hasA && hasB) {
				Session session = getById(aMember.getSessionId());
				if (session != null && Integer.valueOf(1).equals(session.getType())) {
					SessionVO vo = SessionWrapper.build().entityVO(session);
					vo.setUnreadCount(0);
					vo.setMemberCount(members.size());
					vo.setMemberIds(members.stream().map(SessionMember::getUserId).collect(Collectors.toList()));
					return vo;
				}
			}
		}
		return null;
	}

}
