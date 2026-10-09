package org.springblade.workflow.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程实例「内部 Feign 端点」实现（供 blade-system 等服务间发起流程）。
 *
 * <p>为什么要单独开一个端点，而不是复用 {@code POST /instance/start}：
 * {@code WfInstanceController} 带类级 {@code @PreAuth(HAS_AUTH)}，内部 Feign 经服务发现直连、
 * 不携带用户令牌，会被鉴权拦截（实测「认证异常」→ 调用方 fallback）。本项目既有约定是
 * <b>内部 Feign 端点不加 {@code @PreAuth}</b>（参照 blade-system 的 {@code UserStatusFlowClient}），
 * 故此处单独提供无鉴权的内部入口，业务逻辑完全复用 {@link IWfInstanceService#start}。</p>
 *
 * <p>{@code @Hidden}：仅在服务间调用，不对外暴露到接口文档。</p>
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class WfInstanceClient {

	private final IWfInstanceService instanceService;

	private final WfInstanceMapper instanceMapper;

	/**
	 * 内部发起流程（对应 Feign 契约 {@code IWorkflowClient#startProcess}）
	 *
	 * @param dto 发起参数
	 * @return 流程实例ID（字符串，避免 19 位雪花 ID 丢精度）
	 */
	@PostMapping("/feign/client/instance/start")
	public R<String> startProcess(@RequestBody StartProcessDTO dto) {
		return R.data(String.valueOf(instanceService.start(dto)), "发起成功");
	}

	/**
	 * 查询实例是否已结束（供发起侧竞态兜底，见 {@code IWorkflowClient#isEnded}）
	 *
	 * @param id 流程实例ID
	 * @return 是否已结束
	 */
	@GetMapping("/feign/client/instance/ended/{id}")
	public R<Boolean> isEnded(@PathVariable("id") Long id) {
		WfInstance instance = instanceMapper.selectById(id);
		return R.data(instance != null && instance.getEndTime() != null);
	}

}
