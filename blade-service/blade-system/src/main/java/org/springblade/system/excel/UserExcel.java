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
package org.springblade.system.excel;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * UserExcel
 *
 * @author Chill
 */
@Data
public class UserExcel implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private String tenantId;

	private String account;

	private String name;

	private String realName;

	private String email;

	private String phone;

	private String roleId;

	private String deptId;

	private String postId;

	private String roleName;

	private String deptName;

	private String postName;

	private String birthday;

}
