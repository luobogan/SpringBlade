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
package org.springblade.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单组件配置视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "MenuComponentVO对象")
public class MenuComponentVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	private Long id;

	/**
	 * 组件名称
	 */
	private String componentName;

	/**
	 * 组件路径
	 */
	private String componentPath;

	/**
	 * 组件类型: bundled-内置, remote-远程
	 */
	private String componentType;

	/**
	 * 远程组件URL
	 */
	private String remoteUrl;

	/**
	 * 版本号
	 */
	private String version;

	/**
	 * MD5校验值
	 */
	private String md5;

	/**
	 * 状态: 0-禁用, 1-启用
	 */
	private Integer status;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 创建者
	 */
	private String createUser;

	/**
	 * 创建时间
	 */
	private String createTime;

	/**
	 * 更新者
	 */
	private String updateUser;

	/**
	 * 更新时间
	 */
	private String updateTime;

}
