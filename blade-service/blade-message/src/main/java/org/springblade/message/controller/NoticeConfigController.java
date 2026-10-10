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
package org.springblade.message.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.message.entity.NoticeConfig;
import org.springblade.message.mapper.NoticeConfigMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程通知用户级接收配置（三期 T11.1，对齐 ecology ECOLOGY_MESSAGE_CONFIG）
 *
 * <p>语义："只存偏离默认值的记录"——默认全部接收；屏蔽=写 enabled=0；
 * 恢复接收=删行。flow_key 支持 '*'（全部流程），精确配置优先于通配。</p>
 *
 * @author Chill
 */
@RestController
@RequestMapping("notice/config")
@AllArgsConstructor
@Tag(name = "流程通知配置", description = "用户级流程通知接收开关")
public class NoticeConfigController extends BladeController {

	private final NoticeConfigMapper noticeConfigMapper;

	/**
	 * 我的配置列表（前端与流程列表合并渲染开关）
	 */
	@GetMapping("/my")
	@Operation(summary = "我的通知配置", description = "返回当前用户全部配置行（含通配 '*'）")
	public R<List<NoticeConfig>> my(BladeUser user) {
		return R.data(noticeConfigMapper.selectList(Wrappers.<NoticeConfig>lambdaQuery()
			.eq(NoticeConfig::getUserId, user.getUserId())));
	}

	/**
	 * 屏蔽一个流程（写 enabled=0 的行）
	 */
	@PostMapping("/mute")
	@Operation(summary = "屏蔽流程通知", description = "upsert 当前用户对该 flowKey 的配置为屏蔽")
	public R<Boolean> mute(@RequestParam String flowKey, BladeUser user) {
		return R.status(upsert(user, flowKey, 0));
	}

	/**
	 * 恢复接收（物理删行 = 回到默认接收）
	 */
	@PostMapping("/reset")
	@Operation(summary = "恢复接收", description = "物理删除当前用户对该 flowKey 的配置行（回到默认接收）")
	public R<Boolean> reset(@RequestParam String flowKey, BladeUser user) {
		noticeConfigMapper.physicalDelete(user.getUserId(), flowKey);
		return R.success("已恢复接收");
	}

	/**
	 * upsert：把 (user_id, flow_key) 收敛到同一行，避免逻辑删除残留行与唯一索引冲突。
	 * <ul>
	 *   <li>存在任意行（含 is_deleted=1 的幽灵行）→ 重新激活（置 enabled、清 is_deleted）；</li>
	 *   <li>不存在 → 插入新行（enabled=0 表示屏蔽）。</li>
	 * </ul>
	 */
	private boolean upsert(BladeUser user, String flowKey, int enabled) {
		if (flowKey == null || flowKey.isBlank()) {
			return false;
		}
		NoticeConfig exist = noticeConfigMapper.selectByUserAndFlow(user.getUserId(), flowKey);
		if (exist != null) {
			return noticeConfigMapper.reactivate(exist.getId(), enabled, user.getUserId()) > 0;
		}
		NoticeConfig config = new NoticeConfig();
		config.setUserId(user.getUserId());
		config.setFlowKey(flowKey);
		config.setEnabled(enabled);
		config.setTenantId(user.getTenantId());
		config.setCreateUser(user.getUserId());
		config.setCreateDept(user.getDeptId() == null ? 0L : Long.valueOf(user.getDeptId()));
		config.setStatus(1);
		config.setIsDeleted(0);
		noticeConfigMapper.insert(config);
		return true;
	}

}
