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
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_dept")
@Schema(description = "Dept对象")
public class Dept implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 租户ID
	 */
	@Schema(description = "租户ID")
	private String tenantId;

	/**
	 * 父主键
	 */
	@Schema(description = "父主键")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;

	/**
	 * 祖级机构主键
	 */
	@Schema(description = "祖级机构主键")
	private String ancestors;

	/**
	 * 部门名
	 */
	@Schema(description = "部门名")
	private String deptName;

	/**
	 * 部门全称
	 */
	@Schema(description = "部门全称")
	private String fullName;

	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 节点类型（对齐 ecology hrmsubcompany/hrmdepartment 双表模型：1 分部 / 2 部门）
	 */
	@Schema(description = "节点类型:1分部 2部门")
	private Integer deptType;
	/**
	 * 编号（对齐 ecology subcompanycode/departmentcode；租户内唯一，可由编码规则生成）
	 */
	@Schema(description = "组织编号")
	private String deptCode;
	/**
	 * 所属分部节点 id（对齐 ecology hrmdepartment.subcompanyid1 冗余外键，仅部门节点填写）
	 */
	@Schema(description = "所属分部节点id")
	private Long subcompanyId;
	/**
	 * 封存标志（对齐 ecology canceled：封存可解封，语义 ≠ is_deleted 逻辑删除）
	 */
	@Schema(description = "封存标志:0正常 1封存")
	private Integer canceled;
	/**
	 * 部门主管/协管人用户 id（对齐 ecology coadjutant）
	 */
	@Schema(description = "部门主管用户id")
	private Long managerUserId;
	/**
	 * 外部系统同步外键（对齐 ecology outkey）
	 */
	@Schema(description = "外部系统同步外键")
	private String outKey;
	/**
	 * 同步 UUID（对齐 ecology uuid）
	 */
	@Schema(description = "同步UUID")
	private String syncUuid;
	/**
	 * 拼音搜索列（由 dept_name + full_name 生成，对齐 ecology_pinyin_search）
	 */
	@Schema(description = "拼音搜索列")
	private String pinyin;
	/**
	 * 分部人员数上限（对齐 ecology limitUsers，仅超管可设）
	 */
	@Schema(description = "分部人员数上限")
	private Integer limitUsers;
	/**
	 * 层级号（由 ancestors 推导，10 级上限）
	 */
	@Schema(description = "层级号")
	private Integer deptLevel;

	/**
	 * 是否已删除
	 */
	@TableLogic
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
