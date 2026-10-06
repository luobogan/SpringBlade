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
package org.springblade.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.message.entity.Message;

import java.util.List;

/**
 * 消息视图类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageVO extends Message {

	/**
	 * 发送人姓名
	 */
	@Schema(description = "发送人姓名")
	private String senderName;

	/**
	 * 发送人头像（供消息列表 Avatar 展示）
	 */
	@Schema(description = "发送人头像")
	private String senderAvatar;

	/**
	 * 附件列表
	 */
	@Schema(description = "附件列表")
	private List<MessageAttachmentVO> attachments;

	/**
	 * 当前用户是否已读
	 */
	@Schema(description = "当前用户是否已读")
	private Boolean read;

	/**
	 * 接收方已读人数（不含发送人），用于「自己发出的消息」展示已读/未读回执
	 */
	@Schema(description = "接收方已读人数")
	private Integer readCount;

	/**
	 * 接收方总人数（不含发送人）
	 */
	@Schema(description = "接收方总人数")
	private Integer receiverCount;

}
