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
import org.springblade.core.tool.api.R;
import org.springblade.system.dto.StatusFlowStartDTO;
import org.springblade.system.entity.PersonStatusFlow;
import org.springblade.system.entity.PersonStatusFlowRecord;

import java.util.List;

/**
 * 人员状态流转服务类（P3，对齐 ecology hrm_state_proc_set + HrmResourceTryAction/FireAction）
 *
 * @author Chill
 */
public interface IPersonStatusFlowService extends BaseService<PersonStatusFlow> {

	/**
	 * 发起状态变更：命中配置且已绑定流程则发起 Flowable 审批，未配置流程则直接变更
	 *
	 * @param dto 发起参数（用户 + 目标状态 + 意见）
	 * @return 办理结果提示（"已发起审批"/"已直接变更为X"）
	 */
	R<String> start(StatusFlowStartDTO dto);

	/**
	 * 审批完成回调：按 instanceId 反查流转记录，幂等更新 person_status
	 *
	 * @param instanceId 流程实例id(wf_instance.id)
	 * @param approved   是否通过
	 * @param opinion    审批意见
	 * @return 是否处理成功
	 */
	R<Boolean> callback(String instanceId, Boolean approved, String opinion);

	/**
	 * 某用户当前可用的流转配置（供前端渲染「办理状态变更」按钮）
	 *
	 * @param userId 用户主键
	 * @return 可用流转列表
	 */
	List<PersonStatusFlow> availableFlows(Long userId);

	/**
	 * 某用户的历史流转记录（供前端展示流转进度）
	 *
	 * @param userId 用户主键
	 * @return 流转记录列表
	 */
	List<PersonStatusFlowRecord> records(Long userId);

}
