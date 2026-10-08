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
package org.springblade.system.service;

import org.springblade.core.mp.base.BaseService;
import org.springblade.system.entity.WorkCodeRule;

/**
 * 组织编码规则服务类（对齐 ecology CodeRuleManager）
 *
 * @author Chill
 */
public interface IWorkCodeRuleService extends BaseService<WorkCodeRule> {

	/**
	 * 按规则生成下一个编码：prefix + 日期段 + 左补零序列
	 * <p>序列通过 SQL 原子自增（current_seq = current_seq + 1）保证并发安全；
	 * 唯一性兜底（如撞库）由调用方循环重试。</p>
	 *
	 * @param tenantId 租户编号
	 * @param ruleCode 规则编码（USER=工号 / DEPT=部门编号）
	 * @return 生成的编码；该租户未配置规则时返回 null
	 */
	String generate(String tenantId, String ruleCode);

}
