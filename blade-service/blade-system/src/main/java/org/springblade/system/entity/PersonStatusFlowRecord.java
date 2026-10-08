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
package org.springblade.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 人员状态流转记录
 * <p>审批回调按 instance_id 反查本表落库 person_status，天然幂等；直改模式 instance_id 为空。</p>
 *
 * @author Chill
 */
@Data
@TableName("blade_person_status_flow_record")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "人员状态流转记录")
public class PersonStatusFlowRecord extends TenantEntity {

	/** 办理方式：流程审批 */
	public static final int MODE_FLOW = 1;
	/** 办理方式：直接变更（未配置流程时降级） */
	public static final int MODE_DIRECT = 2;

	/** 流转状态：审批中 */
	public static final int FLOW_STATUS_RUNNING = 0;
	/** 流转状态：通过 */
	public static final int FLOW_STATUS_APPROVED = 1;
	/** 流转状态：驳回 */
	public static final int FLOW_STATUS_REJECTED = 2;

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "用户id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	@Schema(description = "变更前人员状态")
	private Integer fromStatus;

	@Schema(description = "变更后人员状态")
	private Integer toStatus;

	@Schema(description = "发起时使用的流程 procKey")
	private String flowKey;

	@Schema(description = "流程实例id(wf_instance.id)")
	private String instanceId;

	@Schema(description = "办理方式:1流程审批 2直接变更")
	private Integer mode;

	@Schema(description = "流转状态:0审批中 1通过 2驳回")
	private Integer flowStatus;

	@Schema(description = "办理/审批意见")
	private String opinion;

	@Schema(description = "发起人")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long starter;

	@Schema(description = "办结时间")
	private LocalDateTime finishTime;

}
