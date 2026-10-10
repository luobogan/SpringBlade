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
package org.springblade.message.constant;

/**
 * 消息中心模块常量
 *
 * @author Chill
 */
public interface MessageConstant {

	/**
	 * 消息中心服务名称（Nacos 注册名）
	 */
	String APPLICATION_MESSAGE_NAME = "blade-message";

	/**
	 * 消息中心前端 API 前缀（Umi dev proxy 剥离 /api 后，网关路由 /blade-message/**）
	 */
	String API_PREFIX = "/api/blade-message";

	/**
	 * 会话类型：两人
	 */
	Integer SESSION_TYPE_PAIR = 1;

	/**
	 * 会话类型：群
	 */
	Integer SESSION_TYPE_GROUP = 2;

	/**
	 * 会话类型：系统通知（流程消息等系统代发的通知会话，每人一条，不进聊天气泡流）
	 */
	Integer SESSION_TYPE_NOTICE = 3;

	/**
	 * 消息分类：聊天（人员间消息）
	 */
	Integer CATEGORY_CHAT = 1;

	/**
	 * 消息分类：流程通知（workflow 事件经内部 Feign 产生的通知）
	 */
	Integer CATEGORY_NOTICE = 2;

	/**
	 * 系统发送者（blade_message.sender_id=0，表示系统代发，无对应 blade_user）
	 */
	Long SENDER_SYSTEM = 0L;

	/**
	 * 通知业务状态：已处理（待办被办理，审批同意时由 blade-workflow 回写）
	 */
	Integer BIZ_STATE_HANDLED = 1;

	/**
	 * 通知业务状态：已办结（流程结束通知自带终态）
	 */
	Integer BIZ_STATE_FINISHED = 2;

	/**
	 * 用户级提醒配置的通配流程 key（表示全部流程；精确配置优先于通配）
	 */
	String FLOW_KEY_WILDCARD = "*";

}
