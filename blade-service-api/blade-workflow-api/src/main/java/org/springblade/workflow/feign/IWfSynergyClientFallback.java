package org.springblade.workflow.feign;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 门户协同区 Feign 客户端降级实现（深度对接 blade-workflow）。
 * <p>服务不可用时返回空列表，保证门户协同区管理端不崩溃（绑定下拉置空）。</p>
 */
@Slf4j
@Component
public class IWfSynergyClientFallback implements IWfSynergyClient {

	@Override
	public R<List<BrowserOptionVO>> definitionOptions() {
		log.error("[blade-workflow] 查询流程定义选项失败，服务不可用");
		return R.data(new ArrayList<>());
	}

	@Override
	public R<List<BrowserOptionVO>> definitionNodes(Long defId) {
		log.error("[blade-workflow] 查询流程节点选项失败，服务不可用. defId={}", defId);
		return R.data(new ArrayList<>());
	}

	@Override
	public R<List<FormFieldVO>> definitionFields(Long defId) {
		log.error("[blade-workflow] 查询流程表单字段失败，服务不可用. defId={}", defId);
		return R.data(new ArrayList<>());
	}
}
