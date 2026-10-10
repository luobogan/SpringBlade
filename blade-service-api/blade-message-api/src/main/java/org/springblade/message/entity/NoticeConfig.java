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
package org.springblade.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程通知用户级接收配置实体（三期 T11.1，对齐 ecology ECOLOGY_MESSAGE_CONFIG）
 *
 * <p>{@code user_id + flow_key} 唯一；{@code flow_key='*'} 表示全部流程（通配）。
 * 语义为"只存偏离默认值的记录"：默认接收，屏蔽才写 {@code enabled=0}，
 * 恢复接收即删行；精确配置优先于通配。</p>
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_message_notice_config")
@Schema(description = "流程通知用户级接收配置")
public class NoticeConfig extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "用户ID（blade_user.id）")
	private Long userId;

	@Schema(description = "流程定义key（proc_key）；* = 全部流程")
	private String flowKey;

	@Schema(description = "是否接收 1=接收 0=屏蔽")
	private Integer enabled;

}
