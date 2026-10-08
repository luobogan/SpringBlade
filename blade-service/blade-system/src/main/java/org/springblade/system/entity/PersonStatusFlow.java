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

/**
 * 人员状态流转配置（对齐 ecology hrm_state_proc_set）
 * <p>按「源状态 → 目标状态」绑定 Flowable 流程；flow_key 为空表示直改不走审批。</p>
 *
 * @author Chill
 */
@Data
@TableName("blade_person_status_flow")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "人员状态流转配置")
public class PersonStatusFlow extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Schema(description = "源人员状态")
	private Integer fromStatus;

	@Schema(description = "目标人员状态")
	private Integer toStatus;

	@Schema(description = "流转名称")
	private String flowName;

	@Schema(description = "绑定的 Flowable 流程 procKey")
	private String flowKey;

	@Schema(description = "流程完成回调 Bean 名")
	private String callbackBean;

	@Schema(description = "备注")
	private String remark;

}
