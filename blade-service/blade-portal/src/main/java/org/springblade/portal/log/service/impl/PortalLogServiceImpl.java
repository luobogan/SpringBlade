package org.springblade.portal.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.portal.entity.PortalLog;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 门户业务审计服务实现
 * <p>对标泛微 PortalLogInfo + BizLogContext：落库操作人/前后值/IP/时间；
 * 写入全程 try-catch 隔离，审计失败仅打 error 日志，绝不影响业务返回。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortalLogServiceImpl implements IPortalLogService {

	/** JSON 快照最大入库长度（超长截断，避免撑爆行宽） */
	private static final int MAX_JSON = 2000;
	private static final String DEFAULT_TENANT = "000000";

	private final PortalLogMapper logMapper;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void record(String logType, String bizType, String bizKey, String title,
				String summary, String beforeJson, String afterJson) {
		record(logType, bizType, bizKey, title, summary, beforeJson, afterJson, null);
	}

	@Override
	public void record(String logType, String bizType, String bizKey, String title,
					   String summary, String beforeJson, String afterJson, String paramsJson) {
		try {
			PortalLog log = new PortalLog();
			fillActor(log);
			log.setLogType(logType);
			log.setBizType(bizType);
			log.setBizKey(bizKey);
			log.setTitle(truncate(title, 255));
			log.setSummary(truncate(summary, 500));
			log.setBeforeJson(truncate(beforeJson, MAX_JSON));
			log.setAfterJson(truncate(afterJson, MAX_JSON));
			log.setParamsJson(truncate(paramsJson, MAX_JSON));
			log.setStatus(1);
			log.setCreateTime(LocalDateTime.now());
			logMapper.insert(log);
		} catch (Exception e) {
			log.error("[portal-log] 记录审计失败，logType={}, bizKey={}, err={}", logType, bizKey, e.getMessage());
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void recordFail(String logType, String bizType, String bizKey, String title, String errorMsg) {
		try {
			PortalLog log = new PortalLog();
			fillActor(log);
			log.setLogType(logType);
			log.setBizType(bizType);
			log.setBizKey(bizKey);
			log.setTitle(truncate(title, 255));
			log.setStatus(0);
			log.setErrorMsg(truncate(errorMsg, 500));
			log.setCreateTime(LocalDateTime.now());
			logMapper.insert(log);
		} catch (Exception e) {
			log.error("[portal-log] 记录失败审计异常，logType={}, err={}", logType, e.getMessage());
		}
	}

	@Override
	public IPage<PortalLog> page(long current, long size, String logType, String bizKey) {
		Page<PortalLog> page = new Page<>(Math.max(current, 1), Math.min(Math.max(size, 1), 200));
		QueryWrapper<PortalLog> qw = new QueryWrapper<>();
		qw.eq(logType != null && !logType.isBlank(), "log_type", logType);
		qw.eq(bizKey != null && !bizKey.isBlank(), "biz_key", bizKey);
		qw.orderByDesc("create_time").orderByDesc("id");
		return logMapper.selectPage(page, qw);
	}

	/** 填充操作人/租户/部门/IP */
	private void fillActor(PortalLog log) {
		BladeUser user = SecureUtil.getUser();
		if (user != null) {
			log.setCreateUser(user.getUserId());
			log.setCreateDept(toLongSafe(user.getDeptId()));
			log.setTenantId(user.getTenantId() == null || user.getTenantId().isBlank()
				? DEFAULT_TENANT : user.getTenantId());
		} else {
			log.setTenantId(DEFAULT_TENANT);
		}
		log.setIp(clientIp());
	}

	/** 取客户端真实 IP：网关已写 X-Real-IP；兼容 X-Forwarded-For 首个；最后回退连接地址 */
	private String clientIp() {
		HttpServletRequest request = WebUtil.getRequest();
		if (request == null) {
			return null;
		}
		String ip = request.getHeader("X-Real-IP");
		if (isBlank(ip)) {
			String xff = request.getHeader("X-Forwarded-For");
			if (!isBlank(xff)) {
				ip = xff.split(",")[0].trim();
			}
		}
		if (isBlank(ip)) {
			ip = request.getRemoteAddr();
		}
		return truncate(ip, 64);
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static Long toLongSafe(String s) {
		if (s == null || s.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.length() <= max ? s : s.substring(0, max) + "…[截断]";
	}
}
