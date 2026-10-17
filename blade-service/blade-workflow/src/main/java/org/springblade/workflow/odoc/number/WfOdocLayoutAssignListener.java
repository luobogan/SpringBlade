package org.springblade.workflow.odoc.number;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springframework.stereotype.Component;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;

/**
 * 版式「出口监听器」：节点提交后，在「转版式节点」把正文当前版本转为版式文件。
 *
 * <p>对应方案「定稿节点同步产正式 PDF/OFD」：引擎推进由 {@code WfTaskServiceImpl#doApprove}
 * 驱动（用户 BPMN 不挂自定义 listener 类），故在提交后置点调用，与
 * {@code WfOdocNumberAssignListener} 同范式。失败由调用方统一 catch 告警、不阻断审批主干。</p>
 *
 * <p>触发条件：当前节点 == 流程设置的「转版式节点」({@code layoutNodeKey})；
 * 若未配置 {@code layoutNodeKey} 则回落到「定稿节点」({@code finalizeNodeKey})；
 * 且 {@code layoutFormat} 为 12(PDF)/13(OFD)，0=不启用则跳过。</p>
 *
 * <p>幂等：该正文已存在同格式版式文件则跳过，避免重复提交重复转版式。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfOdocLayoutAssignListener {

	private final IWfOdocDocService docService;
	private final IWfOdocLayoutService layoutService;
	private final WfOdocProcessSetMapper processSetMapper;

	/**
	 * 节点提交后置处理：若为转版式/定稿节点则自动出版式文件。
	 */
	public void onNodeSubmit(WfInstance inst, WfProcessNode node) {
		if (inst == null || node == null) {
			return;
		}
		WfOdocProcessSet set = processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, inst.getDefId()));
		if (set == null) {
			return;
		}
		Integer fmt = set.getLayoutFormat();
		if (fmt == null || (fmt != 12 && fmt != 13)) {
			// 0=不启用（L1 纯 docx/pdf）或非法值：不自动转版式
			return;
		}
		if (!triggered(node.getNodeKey(), set)) {
			return;
		}
		WfOdocDoc doc = docService.getDocByInstanceId(inst.getId());
		if (doc == null) {
			log.warn("转版式跳过：实例无正文. instId={}", inst.getId());
			return;
		}
		WfOdocDocVersion cur = docService.getCurrentVersion(doc.getId());
		if (cur == null) {
			log.warn("转版式跳过：正文无版本. instId={}, docId={}", inst.getId(), doc.getId());
			return;
		}
		// 幂等：已存在同格式版式则跳过
		boolean exists = layoutService.list(doc.getId()).stream()
			.anyMatch(lf -> lf.getFormat() != null && lf.getFormat() == fmt);
		if (exists) {
			log.info("版式已存在，跳过自动转版式. instId={}, docId={}, format={}", inst.getId(), doc.getId(), fmt);
			return;
		}
		layoutService.toLayout(doc.getId(), cur.getId(), fmt);
		log.info("转版式节点自动出版式完成. instId={}, docId={}, srcVersionId={}, format={}",
			inst.getId(), doc.getId(), cur.getId(), fmt);
	}

	/** 命中判定：优先 layoutNodeKey；未配置时回落到 finalizeNodeKey */
	private boolean triggered(String nodeKey, WfOdocProcessSet set) {
		if (nodeKey == null) {
			return false;
		}
		String layoutNodeKey = set.getLayoutNodeKey();
		if (layoutNodeKey != null && !layoutNodeKey.isBlank()) {
			return layoutNodeKey.equals(nodeKey);
		}
		String finalizeNodeKey = set.getFinalizeNodeKey();
		return finalizeNodeKey != null && finalizeNodeKey.equals(nodeKey);
	}
}
