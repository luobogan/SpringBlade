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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.WorkCodeRule;
import org.springblade.system.mapper.WorkCodeRuleMapper;
import org.springblade.system.service.IWorkCodeRuleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 组织编码规则服务实现类
 *
 * @author Chill
 */
@Service
public class WorkCodeRuleServiceImpl extends BaseServiceImpl<WorkCodeRuleMapper, WorkCodeRule> implements IWorkCodeRuleService {

	/** 序列左补零默认位数 */
	private static final int DEFAULT_SEQ_LEN = 4;
	/** 编码规则默认租户（规则表按租户隔离，缺省走 000000） */
	private static final String DEFAULT_TENANT = "000000";

	@Override
	public String generate(String tenantId, String ruleCode) {
		String resolvedTenant = Func.toStr(tenantId, DEFAULT_TENANT);
		WorkCodeRule rule = this.getOne(Wrappers.<WorkCodeRule>lambdaQuery()
			.eq(WorkCodeRule::getRuleCode, ruleCode)
			.eq(WorkCodeRule::getTenantId, resolvedTenant));
		if (rule == null) {
			// 未配置编码规则：交由调用方决定兜底策略
			return null;
		}
		// 原子自增序列（InnoDB 行锁保证并发安全）
		this.update(Wrappers.<WorkCodeRule>lambdaUpdate()
			.setSql("current_seq = current_seq + 1")
			.eq(WorkCodeRule::getId, rule.getId()));
		long seq = this.getById(rule.getId()).getCurrentSeq();
		int seqLen = Func.toInt(rule.getSeqLen(), DEFAULT_SEQ_LEN);
		StringBuilder code = new StringBuilder(Func.toStr(rule.getPrefix(), ""));
		if (Func.isNotBlank(rule.getDateFormat())) {
			code.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat())));
		}
		code.append(String.format("%0" + seqLen + "d", seq));
		return code.toString();
	}

}
