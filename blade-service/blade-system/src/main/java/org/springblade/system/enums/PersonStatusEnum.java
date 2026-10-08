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
package org.springblade.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springblade.core.tool.utils.Func;

/**
 * 人员状态枚举（对齐 ecology hrmresource.status 经典枚举）
 * <p>
 * <b>在职判定口径（P3-5，已确认）</b>：在职集合 = {@code 0,1,2,3,5}，仅 {@code 4 解聘} 拒绝登录，
 * 对齐 Ecology {@code ResourceComInfo} 的在职判定；退休(5) 仍属在职，可正常登录。
 * 存量数据 {@code person_status} 为 NULL 时按在职放行，保证向后兼容。
 *
 * @author Chill
 */
@Getter
@AllArgsConstructor
public enum PersonStatusEnum {

	PROBATION(0, "试用"),
	REGULAR(1, "正式"),
	TEMPORARY(2, "临时"),
	EXTENDED(3, "延期"),
	DISMISS(4, "解聘"),
	RETIRE(5, "退休");

	private final int code;
	private final String desc;

	/**
	 * 是否允许登录：仅解聘(4) 拒绝；null（存量数据）按在职放行
	 *
	 * @param personStatus 人员状态
	 * @return true 允许登录
	 */
	public static boolean loginAllowed(Integer personStatus) {
		if (personStatus == null) {
			return true;
		}
		return !Integer.valueOf(DISMISS.code).equals(personStatus);
	}

	/**
	 * 按编码取枚举
	 *
	 * @param code 状态编码
	 * @return 枚举；未命中返回 null
	 */
	public static PersonStatusEnum of(Integer code) {
		if (code == null) {
			return null;
		}
		for (PersonStatusEnum item : values()) {
			if (item.code == code) {
				return item;
			}
		}
		return null;
	}

	/**
	 * 状态描述（未命中返回原值）
	 *
	 * @param code 状态编码
	 * @return 描述文本
	 */
	public static String descOf(Integer code) {
		PersonStatusEnum item = of(code);
		return item == null ? Func.toStr(code, "-") : item.desc;
	}

}
