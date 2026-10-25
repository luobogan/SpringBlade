package org.springblade.workflow.odoc.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealStrategyHolder;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;
import org.springblade.workflow.odoc.service.IWfOdocSealService;
import org.springblade.workflow.odoc.service.IWfOdocSecretRuleService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公文「节点必做动作」统一拦截入口。
 *
 * <p>在审批同意（提交）推进引擎前执行：当前节点若为流程设置里的某个「必做节点」
 * 且对应动作未完成，则拒绝提交。已落地「套红」「电子签章」「密级收发限制」；清稿 / 打印暂无
 * 「完成态」可校验，仅记录到达日志、不阻断（详见对应方法 TODO）。后续新增必做动作
 * 在同一框架内按节点判定扩展，互不影响。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfNodeMustDoActionExecutor {

	private final IWfOdocDocService docService;
	private final WfOdocProcessSetMapper processSetMapper;
	private final IWfOdocSealService sealService;
	private final IFormmodeClient formmodeClient;
	private final IWfOdocSecretRuleService secretRuleService;
	private final IWfOdocLayoutService layoutService;
	private final OdocSealStrategyHolder strategyHolder;

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
		checkTaohong(inst, node, set);
		checkSeal(inst, node, set);
		checkSecretLevel(inst, node, set);
		// 清稿 / 打印暂无「完成态」可校验：WfOdocPrintSet 只是「哪个节点用哪个打印模板」的
		// 配置表，非「已打印」记录；清稿无任何实体/版本标记。若此时做硬拦截会永久卡流程，
		// 故仅记录到达日志、不阻断，待完成态落地后再改为硬拦截。
		checkClearLogOnly(inst, node, set);
		checkPrintLogOnly(inst, node, set);
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

	/**
	 * 电子签章硬拦截：当前节点命中签章节点（signNodeKey 单选 或 signatureNodeKeys 多选）
	 * 且 {@code sealRequired=1} 时，要求该正文已有成功签章记录，否则拒绝提交。
	 *
	 * <p>优雅降级：未装配可用 CA 签章服务（无从签章）时只告警并跳过，绝不卡死未配置流程。</p>
	 *
	 * <p><b>第三方电子签章（esign）例外</b>：签署是异步的（提交后由出口监听器发起签署，
	 * 回调/补偿完成后才归档签章件），若在此要求"已签章"会形成死锁（永远无法提交）。
	 * 故 esign 方案下把校验口径降为「可签章前提」——必须已有 PDF/OFD 版式文件。</p>
	 */
	private void checkSeal(WfInstance inst, WfProcessNode node, WfOdocProcessSet set) {
		if (set.getSealRequired() == null || set.getSealRequired() != 1) {
			return;
		}
		if (!hitNode(node.getNodeKey(), set.getSignNodeKey(), set.getSignatureNodeKeys())) {
			return;
		}
		WfOdocDoc doc = docService.getDocByInstanceId(inst.getId());
		if (IOdocSealStrategy.MODE_ESIGN.equals(strategyHolder.mode())) {
			// 第三方签章：异步，只校验「已有版式文件」这一可签章前提
			boolean ready = doc != null && hasLayout(doc.getId());
			if (!ready) {
				throw new ServiceException("该节点提交前必须先生成版式文件（第三方电子签章需要 PDF/OFD 版式）");
			}
			return;
		}
		if (!sealService.sealAvailable()) {
			log.warn("[blade-workflow] 签章节点到达但未装配可用 CA 签章服务，降级跳过. instId={}, nodeKey={}",
				inst.getId(), node.getNodeKey());
			return;
		}
		boolean sealed = doc != null && hasSeal(doc.getId());
		if (!sealed) {
			throw new ServiceException("该节点提交前必须完成电子签章");
		}
	}

	/** 是否已有可用于签章的版式文件（PDF/OFD） */
	private boolean hasLayout(Long docId) {
		List<WfOdocLayoutFile> list = layoutService.list(docId);
		return list != null && list.stream()
			.anyMatch(lf -> lf.getFormat() != null && (lf.getFormat() == 12 || lf.getFormat() == 13));
	}

	/**
	 * 密级收发限制：读取表单的「秘密等级」与「收文单位」，若收文单位被限制规则约束且公文密级
	 * 超出其允许范围，则拒绝提交。
	 *
	 * <p>防御式，绝不误伤正常提交：① 未配置密级/收文单位字段 → 跳过；② 读表单异常 → 记日志跳过；
	 * ③ 密级无法判定或无收文单位 → 跳过；④ 无对应限制规则 → 不限制；仅显式规则越限才拒。</p>
	 */
	private void checkSecretLevel(WfInstance inst, WfProcessNode node, WfOdocProcessSet set) {
		String secKey = set.getSecretLevelFieldKey();
		String unitKey = set.getReceiveUnitFieldKey();
		if (secKey == null || secKey.isBlank() || unitKey == null || unitKey.isBlank()) {
			return;
		}
		String docSecret;
		String receiveUnits;
		try {
			Map<String, Object> fv = readFormValues(inst);
			docSecret = str(fv, secKey);
			receiveUnits = str(fv, unitKey);
		} catch (Exception e) {
			log.warn("[blade-workflow] 密级收发限制读取表单失败（忽略，不阻断）. instId={}, {}",
				inst.getId(), e.getMessage());
			return;
		}
		String msg = secretRuleService.violationMessage(docSecret, receiveUnits);
		if (msg != null) {
			throw new ServiceException(msg);
		}
	}

	private boolean hasSeal(Long docId) {
		List<WfOdocSeal> list = sealService.list(docId);
		return list != null && list.stream()
			.anyMatch(s -> s.getStatus() != null && s.getStatus() >= 1);
	}

	private void checkClearLogOnly(WfInstance inst, WfProcessNode node, WfOdocProcessSet set) {
		if (containsCsv(node.getNodeKey(), set.getCleanCopyNodeKeys())) {
			// TODO 清稿完成态（清稿版本标记）落地后改为硬拦截
			log.info("[blade-workflow] 到达清稿节点(暂无完成态校验，仅记录). instId={}, nodeKey={}",
				inst.getId(), node.getNodeKey());
		}
	}

	private void checkPrintLogOnly(WfInstance inst, WfProcessNode node, WfOdocProcessSet set) {
		if (containsCsv(node.getNodeKey(), set.getPrintNodeKeys())) {
			// TODO 打印留痕（已打印记录）落地后改为硬拦截
			log.info("[blade-workflow] 到达打印节点(暂无完成态校验，仅记录). instId={}, nodeKey={}",
				inst.getId(), node.getNodeKey());
		}
	}

	/** 命中判定：nodeKey 等于单选 key，或包含在 CSV 多值里 */
	private boolean hitNode(String nodeKey, String single, String csv) {
		if (nodeKey == null) {
			return false;
		}
		if (single != null && !single.isBlank() && single.equals(nodeKey)) {
			return true;
		}
		return containsCsv(nodeKey, csv);
	}

	private boolean containsCsv(String nodeKey, String csv) {
		if (nodeKey == null || csv == null || csv.isBlank()) {
			return false;
		}
		Set<String> keys = Arrays.stream(csv.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
		return keys.contains(nodeKey);
	}

	/** 读流程表单业务数据；异常直接抛出由调用方决定（密级校验选择跳过）。 */
	private Map<String, Object> readFormValues(WfInstance inst) {
		if (inst.getFormId() == null || inst.getDataId() == null) {
			return Collections.emptyMap();
		}
		R<FormDataVO> cur = formmodeClient.getBusinessData(inst.getFormId(), inst.getDataId());
		if (cur != null && cur.isSuccess() && cur.getData() != null
			&& cur.getData().getFieldValues() != null) {
			return cur.getData().getFieldValues();
		}
		return Collections.emptyMap();
	}

	private static String str(Map<String, Object> fv, String key) {
		if (fv == null || key == null || key.isBlank()) {
			return null;
		}
		Object v = fv.get(key);
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v);
		return s.isBlank() ? null : s;
	}
}
