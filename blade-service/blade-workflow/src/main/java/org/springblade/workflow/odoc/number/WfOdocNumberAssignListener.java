package org.springblade.workflow.odoc.number;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocNumberService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 文号「出口监听器」：节点提交后（token 离开节点）在定稿节点取号并回写表单。
 *
 * <p>对应方案「文号并发取号 + Flowable 出口监听器」：本工作流的引擎推进由
 * {@code WfTaskServiceImpl#doApprove} 驱动（用户 BPMN 不挂自定义 listener 类），
 * 故在此提交后置点调用，与 {@code NodeActionExecutor(PHASE_POST)} 同范式。</p>
 *
 * <p>取号点 = 流程设置的「定稿节点」(finalizeNodeKey)；取到的文号回写到业务表
 * {@code numberFieldKey} 字段。幂等：目标字段已有值则跳过，避免重复提交重复取号。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfOdocNumberAssignListener {

	private final IWfOdocNumberService numberService;
	private final WfOdocProcessSetMapper processSetMapper;
	private final IFormmodeClient formmodeClient;

	/**
	 * 节点提交后置处理：若为定稿节点则取号并回写。
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
		String finalizeNodeKey = set.getFinalizeNodeKey();
		String numberFieldKey = set.getNumberFieldKey();
		if (finalizeNodeKey == null || finalizeNodeKey.isBlank()
			|| numberFieldKey == null || numberFieldKey.isBlank()) {
			return;
		}
		if (!finalizeNodeKey.equals(node.getNodeKey())) {
			return;
		}
		if (inst.getFormId() == null || inst.getDataId() == null) {
			log.warn("定稿取号跳过：实例未关联表单/业务数据 instId={}", inst.getId());
			return;
		}

		// 幂等：业务表文号字段已有值则不再取号
		R<FormDataVO> cur = formmodeClient.getBusinessData(inst.getFormId(), inst.getDataId());
		if (cur != null && cur.isSuccess() && cur.getData() != null && cur.getData().getFieldValues() != null) {
			Object exist = cur.getData().getFieldValues().get(numberFieldKey);
			if (exist != null && !String.valueOf(exist).isBlank()) {
				log.info("定稿节点文号已存在，跳过取号 instId={} value={}", inst.getId(), exist);
				return;
			}
		}

		String number = numberService.assign(inst.getDefId());
		FormDataSaveDTO dto = new FormDataSaveDTO();
		dto.setFormId(inst.getFormId());
		dto.setDataId(inst.getDataId());
		Map<String, Object> fieldValues = new HashMap<>();
		fieldValues.put(numberFieldKey, number);
		dto.setFieldValues(fieldValues);
		R<Long> r = formmodeClient.saveBusinessData(dto);
		if (r == null || !r.isSuccess()) {
			throw new RuntimeException("文号回写表单失败: " + (r == null ? "null" : r.getMsg()));
		}
		log.info("定稿节点取号并回写完成 instId={} numberFieldKey={} number={}", inst.getId(), numberFieldKey, number);
	}
}
