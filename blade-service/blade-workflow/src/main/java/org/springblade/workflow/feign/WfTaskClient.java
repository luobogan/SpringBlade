package org.springblade.workflow.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.dto.WfTaskListFilter;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程任务「内部 Feign 端点」实现（供 blade-portal 门户待办元素取数）。
 *
 * <p>为什么不复用 {@code GET /task/todo}：{@code WfTaskController} 带类级
 * {@code @PreAuth(HAS_AUTH)}，内部 Feign 经服务发现直连、不携带用户令牌会被鉴权拦截。
 * 本项目既有约定是<b>内部 Feign 端点不加 {@code @PreAuth}</b>（参照 {@link WfInstanceClient}），
 * 路径用 {@code /feign/client} 前缀隔离，网关 {@code InnerFilter} 拒绝经网关的外部访问。</p>
 *
 * <p>办理人由调用方显式传入（门户传当前登录用户ID）；本端点只读且已被前缀隔离，不做
 * 「非管理员收敛为登录人」处理。</p>
 *
 * <p>{@code @Hidden}：仅服务间调用，不暴露到接口文档。</p>
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class WfTaskClient {

	private final IWfTaskService taskService;

	/**
	 * 查询待办（对应 Feign 契约 {@code IWorkflowClient#todoList}）
	 *
	 * @param assignee 办理人用户ID
	 * @param limit    最多返回条数
	 * @return 待办列表
	 */
	@GetMapping("/feign/client/task/todo")
	public R<List<WfTaskVO>> todoList(@RequestParam("assignee") Long assignee,
									  @RequestParam(value = "limit", required = false) Integer limit) {
		if (assignee == null) {
			return R.fail("办理人不能为空");
		}
		List<WfTaskVO> tasks = taskService.todo(assignee, new WfTaskListFilter());
		List<WfTaskVO> result = new ArrayList<>();
		if (tasks != null) {
			int max = (limit == null || limit <= 0) ? tasks.size() : limit;
			for (WfTaskVO task : tasks) {
				if (result.size() >= max) {
					break;
				}
				result.add(task);
			}
		}
		return R.data(result);
	}
}