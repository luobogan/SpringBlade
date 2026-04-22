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
 * 菜单组件实体类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_menu_component")
@Schema(description = "MenuComponent对象")
public class MenuComponent extends TenantEntity {

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
	 * 组件名称
	 */
	@Schema(description = "组件名称")
	private String componentName;

	/**
	 * 组件类型
	 */
	@Schema(description = "组件类型")
	private Integer componentType;

	/**
	 * 组件路径
	 */
	@Schema(description = "组件路径")
	private String componentPath;

	/**
	 * 组件配置
	 */
	@Schema(description = "组件配置")
	private String componentConfig;

	/**
	 * 组件状态
	 */
	@Schema(description = "组件状态")
	private Integer status;

	/**
	 * 组件描述
	 */
	@Schema(description = "组件描述")
	private String componentDesc;

	/**
	 * 组件版本
	 */
	@Schema(description = "组件版本")
	private String componentVersion;

	/**
	 * 组件作者
	 */
	@Schema(description = "组件作者")
	private String componentAuthor;

	/**
	 * 组件图标
	 */
	@Schema(description = "组件图标")
	private String componentIcon;

	/**
	 * 组件颜色
	 */
	@Schema(description = "组件颜色")
	private String componentColor;

	/**
	 * 组件标签
	 */
	@Schema(description = "组件标签")
	private String componentTags;

	/**
	 * 组件分类
	 */
	@Schema(description = "组件分类")
	private String componentCategory;

	/**
	 * 组件排序
	 */
	@Schema(description = "组件排序")
	private Integer componentSort;

	/**
	 * 组件是否系统内置
	 */
	@Schema(description = "组件是否系统内置")
	private Integer isSystem;

	/**
	 * 组件是否可用
	 */
	@Schema(description = "组件是否可用")
	private Integer isEnable;

	/**
	 * 组件是否删除
	 */
	@Schema(description = "组件是否删除")
	private Integer isDeleted;

	/**
	 * 组件扩展字段
	 */
	@Schema(description = "组件扩展字段")
	private String extra;

}
