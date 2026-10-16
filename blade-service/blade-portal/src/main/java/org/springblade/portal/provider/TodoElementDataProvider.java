package org.springblade.portal.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.vo.ElementDataVO;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办事项元素 Provider
 * <p>
 * 数据源：流程服务内部端点 {@code GET /feign/client/task/todo}
 * （由 {@code IWorkflowClient#todoList} 调用；网关 InnerFilter 隔离，不对外开放）。
 * 服务不可用时降级为空列表，仅该卡片显示错误态。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoElementDataProvider implements IPortalElementDataProvider {

	private final IWorkflowClient workflowClient;

	@Override
	public String type() {
		return PortalConstant.ELEMENT_TODO;
	}

	@Override
	public ElementDataVO load(ElementDataContext ctx) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type());
		try {
			if (ctx.getUserId() == null) {
				vo.setData(new ArrayList<>());
				vo.setMessage("未获取到登录用户");
				return vo;
			}
			int limit = ctx.getLimit() > 0 ? ctx.getLimit() : PortalConstant.DEFAULT_PER_PAGE;
			R<List<WfTaskVO>> result = workflowClient.todoList(ctx.getUserId(), limit);
			List<WfTaskVO> tasks = (result != null && result.isSuccess()) ? result.getData() : null;
			List<Map<String, Object>> rows = new ArrayList<>();
			if (tasks != null) {
				for (WfTaskVO task : tasks) {
					if (task == null) {
						continue;
					}
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("taskId", task.getId());
					row.put("instId", task.getInstId());
					row.put("title", task.getTitle());
					row.put("defName", task.getDefName());
					row.put("nodeName", task.getNodeName());
					row.put("starter", task.getStarter());
					row.put("urgency", task.getUrgency());
					row.put("receiveTime", task.getReceiveTime());
					row.put("dueTime", task.getDueTime());
					// 前端据此跳转到待办列表处理（内部端点不对外，页面入口仍是 /workflow/todo）
					row.put("url", "/workflow/todo");
					rows.add(row);
					if (rows.size() >= limit) {
						break;
					}
				}
			}
			vo.setData(rows);
		} catch (Exception e) {
			log.warn("[portal] 待办元素取数失败，msg={}", e.getMessage());
			vo.setData(new ArrayList<>());
			vo.setSuccess(Boolean.FALSE);
			vo.setMessage("待办加载失败：审批流程服务不可用");
		}
		return vo;
	}
}