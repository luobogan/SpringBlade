package org.springblade.workflow.odoc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.service.IWfOdocTodoService;
import org.springblade.workflow.odoc.vo.OdocTodoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公文待办 / 已办 / 我的请求 控制器（对应 E9 {@code OdocTodoListAction} + {@code OdocReqListCmd}）。
 *
 * <p>路径与 {@link WfOdocController} 同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。
 * 办理 / 退回 / 转办等动作复用通用 {@code /task/**} 接口，本控制器只负责「公文列表」。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文待办/查询", description = "待办/已办/我的请求（公文维度分页）")
public class WfOdocTodoController {

	private final IWfOdocTodoService todoService;

	@GetMapping("/todo")
	@Operation(summary = "公文待办", description = "当前人公文待办，按接收时间倒序；officalType 区分发文/收文/签报")
	public R<IPage<OdocTodoVO>> todo(OdocListQuery query) {
		return R.data(todoService.pageTodo(query));
	}

	@GetMapping("/done")
	@Operation(summary = "公文已办", description = "当前人已办公文，按处理时间倒序")
	public R<IPage<OdocTodoVO>> done(OdocListQuery query) {
		return R.data(todoService.pageDone(query));
	}

	@GetMapping("/my-request")
	@Operation(summary = "我的请求/公文查询", description = "我发起的公文实例，按发起时间倒序")
	public R<IPage<OdocTodoVO>> myRequest(OdocListQuery query) {
		return R.data(todoService.pageMyRequest(query));
	}

}
