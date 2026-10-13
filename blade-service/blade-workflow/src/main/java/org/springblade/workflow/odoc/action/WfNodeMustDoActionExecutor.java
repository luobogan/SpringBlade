package org.springblade.workflow.odoc.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 公文「节点必做动作」统一拦截入口。
 *
 * <p>在审批同意（提交）推进引擎前执行：当前节点若为流程设置里的某个「必做节点」
 * 且对应动作未完成，则拒绝提交。目前已落地「套红」；后续电子签章 / 清稿 / 打印 /
 * 转版式等在同一框架内按节点判定扩展，互不影响。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfNodeMustDoActionExecutor {

	private final IWfOdocDocService docService;
	private final WfOdocProcessSetMapper processSetMapper;

	/**
	 * 提交前校验节点必做动作。
	 *
	 * @param inst 流程实例
	 * @param node 当前节点
	 */
	public void checkBeforeSubmit(WfInstance inst, WfProcessNode node) {
		if (inst == null || node == null) {
			return;
		}
		WfOdocProcessSet set = processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, inst.getDefId()));
		if (set == null) {
			return;
		}
		// 套红：套红节点 + 强制套红开关 → 文档必须已套红
		checkTaohong(inst, node, set);
		// 未来：checkSeal / checkClear / checkPrint / checkLayout 在此按节点扩展
	}

	private void checkTaohong(WfInstance inst, WfProcessNode node, WfOdocProcessSet set) {
		Integer must = set.getMustTaohong();
		String taohongNodeKey = set.getTaohongNodeKey();
		if (must == null || must != 1 || taohongNodeKey == null || taohongNodeKey.isBlank()) {
			return;
		}
		if (!Objects.equals(node.getNodeKey(), taohongNodeKey)) {
			return;
		}
		WfOdocDoc doc = docService.getDocByInstanceId(inst.getId());
		boolean done = doc != null && doc.getHasUsedTemplet() != null && doc.getHasUsedTemplet() == 1;
		if (!done) {
			throw new ServiceException("该节点提交前必须完成公文套红");
		}
	}
}
