package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.springblade.workflow.dto.WfBackfillResult;
import org.springblade.workflow.dto.WfReconcileResult;
import org.springblade.workflow.entity.FlowDefBridge;
import org.springblade.workflow.entity.WfCustomOperation;
import org.springblade.workflow.entity.WfCustomOperationRight;
import org.springblade.workflow.entity.WfDefinitionGray;
import org.springblade.workflow.entity.WfNodeDetailFilter;
import org.springblade.workflow.entity.WfNodeDetailPerm;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.FlowDefBridgeMapper;
import org.springblade.workflow.mapper.WfCustomOperationMapper;
import org.springblade.workflow.mapper.WfCustomOperationRightMapper;
import org.springblade.workflow.mapper.WfDefinitionGrayMapper;
import org.springblade.workflow.mapper.WfNodeDetailFilterMapper;
import org.springblade.workflow.mapper.WfNodeDetailPermMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfNodeTimeoutMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.util.BpmnExtensionUtil.WfDetailFilterExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfDetailPermExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfDetailTablePermExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfFieldPermExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfLinkExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfNodeExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfOperationExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfOperatorExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfProcessMetaExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfRightExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfTimeoutExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定义期回填与双轨对账作业（T-5 / P1）。
 *
 * <p><b>回填</b>：把现有 {@code wf_*} 定义期数据（节点/出口/操作者/字段权限/明细过滤/超时/自定义操作）
 * 转换并写回 BPMN {@code extensionElements}（{@code wf:} 命名空间），随后重新部署（生成<b>新引擎版本</b>，
 * 旧版本保留供历史实例还原），并在 {@code flow_def_bridge} 记录业务 defId ↔ 引擎定义映射。</p>
 *
 * <p><b>双轨对账</b>：回填后保留 {@code wf_*} 定义期表<b>只读</b>，周期比对「引擎 BPMN 扩展」与
 * 「wf_* 源表」的差异，不一致仅记录告警，不修复、不改写。</p>
 *
 * <p><b>幂等</b>：BPMN 已含 {@code wf:} 命名空间或桥接表已存在则跳过；重复执行安全。</p>
 *
 * <p><b>租户</b>：受 MyBatis-Plus 租户插件约束，本作业按当前租户上下文读取/写入；
 * 跨租户全量迁移请在对应租户上下文下执行，或后续扩展为遍历租户（见 {@code doc/md/定义期回填与双轨对账方案.md}）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfDefinitionBackfillJob {

	private static final ObjectMapper OM = new ObjectMapper();
	private static final BpmnXMLConverter CONVERTER = new BpmnXMLConverter();
	private static final Pattern PROCDEF_VERSION = Pattern.compile(":(\\d+):");

	private final WfProcessDefinitionMapper defMapper;
	private final WfProcessNodeMapper nodeMapper;
	private final WfNodeOperatorMapper operatorMapper;
	private final WfNodeLinkMapper linkMapper;
	private final WfNodeFieldPermMapper fieldPermMapper;
	private final WfNodeDetailPermMapper detailPermMapper;
	private final WfNodeDetailFilterMapper detailFilterMapper;
	private final WfNodeTimeoutMapper timeoutMapper;
	private final WfCustomOperationMapper customOperationMapper;
	private final WfCustomOperationRightMapper customOperationRightMapper;
	private final WfDefinitionGrayMapper definitionGrayMapper;
	private final FlowDefBridgeMapper bridgeMapper;
	private final IWfDefinitionService definitionService;

	@Value("${blade.workflow.backfill.enabled:false}")
	private boolean scheduledEnabled;

	/** 手动触发：回填全部流程定义。 */
	public WfBackfillResult backfillAll() {
		List<WfProcessDefinition> defs = defMapper.selectList(new QueryWrapper<>());
		int total = 0, ok = 0, skip = 0, fail = 0;
		List<String> errors = new ArrayList<>();
		for (WfProcessDefinition def : defs) {
			total++;
			try {
				if (isBackfilled(def)) {
					skip++;
					continue;
				}
				backfillOne(def);
				ok++;
			} catch (Exception e) {
				fail++;
				String msg = "defId=" + def.getId() + " tenant=" + def.getTenantId() + " : " + e.getMessage();
				errors.add(msg);
				log.warn("[blade-workflow] 定义期回填失败. {}", msg, e);
			}
		}
		log.info("[blade-workflow] 定义期回填完成. total={}, ok={}, skip={}, fail={}", total, ok, skip, fail);
		return new WfBackfillResult(total, ok, skip, fail, errors);
	}

	/** 定时触发（默认关闭，由 blade.workflow.backfill.enabled 开关；仅做一次性迁移，跑完即关）。 */
	@Scheduled(cron = "0 0 3 * * ?")
	public void scheduledBackfill() {
		if (!scheduledEnabled) {
			return;
		}
		backfillAll();
	}

	/** 手动触发：双轨对账全部已回填定义。 */
	public WfReconcileResult reconcileAll() {
		List<WfProcessDefinition> defs = defMapper.selectList(new QueryWrapper<>());
		int total = 0, matched = 0, mismatched = 0;
		List<String> details = new ArrayList<>();
		for (WfProcessDefinition def : defs) {
			Long cnt = bridgeMapper.selectCount(new QueryWrapper<FlowDefBridge>().eq("def_id", def.getId()));
			if (cnt == null || cnt == 0) {
				continue;
			}
			total++;
			List<String> diff = reconcileOne(def);
			if (diff.isEmpty()) {
				matched++;
			} else {
				mismatched++;
				details.add("defId=" + def.getId() + " -> " + String.join("; ", diff));
			}
		}
		log.info("[blade-workflow] 双轨对账完成. total={}, matched={}, mismatched={}", total, matched, mismatched);
		return new WfReconcileResult(total, matched, mismatched, details);
	}

	// ---------------------------------------------------------------- 回填单条

	@Transactional(rollbackFor = Exception.class)
	public void backfillOne(WfProcessDefinition def) {
		String xml = def.getBpmnXml();
		if (xml == null || xml.isBlank()) {
			return;
		}
		BpmnModel model = parse(xml, def.getId());
		Process process = model.getMainProcess();
		if (process == null) {
			return;
		}

		// 流程级元数据
		WfProcessMetaExt meta = new WfProcessMetaExt();
		meta.defKey = def.getProcKey();
		meta.formId = def.getFormId() == null ? null : String.valueOf(def.getFormId());
		WfDefinitionGray gray = definitionGrayMapper.selectOne(
			new QueryWrapper<WfDefinitionGray>().eq("def_id", def.getId()));
		if (gray != null) {
			meta.grayEnabled = gray.getStatus() == null ? "0" : String.valueOf(gray.getStatus());
			try {
				ObjectNode g = OM.createObjectNode();
				g.put("strategy", gray.getStrategy());
				g.put("ratio", gray.getRatio());
				g.put("scopeValue", gray.getScopeValue());
				g.put("baseProcDefId", gray.getBaseProcDefId());
				g.put("grayProcDefId", gray.getGrayProcDefId());
				meta.grayRule = OM.writeValueAsString(g);
			} catch (Exception ignored) {
				// 灰度规则非回填关键路径
			}
		}
		BpmnExtensionUtil.writeProcessMeta(process, meta);

		// 节点（含操作者/字段权限/明细/超时/自定义操作）
		List<WfProcessNode> nodes = nodeMapper.selectList(new QueryWrapper<WfProcessNode>().eq("def_id", def.getId()));
		for (WfProcessNode node : nodes) {
			UserTask task = (UserTask) process.getFlowElement(node.getNodeKey());
			if (task == null) {
				log.warn("[blade-workflow] 节点 {} 在 BPMN 中无对应 userTask，跳过", node.getNodeKey());
				continue;
			}
			BpmnExtensionUtil.writeNode(task, buildNodeExt(node));
		}

		// 出口（连线）：
		//  ① 真实连线 → 写 sequenceFlow 上的 wf:link（含 viaGatewayKey）；
		//  ② 折叠连线（A→网关→B 折叠为 A→B，BPMN 中【没有】A→B sequenceFlow）
		//     → 写流程级 wf:foldedLink。此前这类连线因查不到 sequenceFlow 被直接跳过而丢失，
		//       导致「可退回节点 / 下一节点」计算与设计器不一致，故必须落盘。
		List<WfNodeLink> links = linkMapper.selectList(new QueryWrapper<WfNodeLink>().eq("def_id", def.getId()));
		java.util.List<BpmnExtensionUtil.WfFoldedLinkExt> folded = new java.util.ArrayList<>();
		for (WfNodeLink link : links) {
			SequenceFlow flow = findSequenceFlow(process, link.getFromNodeKey(), link.getToNodeKey());
			if (flow == null) {
				BpmnExtensionUtil.WfFoldedLinkExt f = new BpmnExtensionUtil.WfFoldedLinkExt();
				f.from = link.getFromNodeKey();
				f.to = link.getToNodeKey();
				f.viaGatewayKey = link.getViaGatewayKey();
				f.isReject = str(link.getIsReject());
				f.isMustPass = str(link.getIsMustPass());
				f.conditionCn = link.getConditionCn();
				f.sortOrder = str(link.getSortOrder());
				f.extraOperations = link.getExtraOperations();
				folded.add(f);
				continue;
			}
			WfLinkExt lext = new WfLinkExt();
			lext.isReject = str(link.getIsReject());
			lext.isMustPass = str(link.getIsMustPass());
			lext.conditionCn = link.getConditionCn();
			lext.sortOrder = str(link.getSortOrder());
			lext.viaGateway = str(link.getViaGateway());
			lext.viaGatewayKey = link.getViaGatewayKey();
			lext.extraOperations = link.getExtraOperations();
			BpmnExtensionUtil.writeLink(flow, lext);
		}
		BpmnExtensionUtil.writeFoldedLinks(process, folded);
		if (!folded.isEmpty()) {
			log.info("[blade-workflow] 回填折叠连线 {} 条（BPMN 无对应 sequenceFlow，落到流程级 wf:foldedLink）. defId={}",
				folded.size(), def.getId());
		}

		// 序列化并落库（BPMN 现为载体，单一事实源）
		String merged = new String(CONVERTER.convertToXML(model), StandardCharsets.UTF_8);
		def.setBpmnXml(merged);
		defMapper.updateById(def);

		// 重新部署（生成新引擎版本并激活）；deploy 内部含校验/注入出口条件/归一化
		definitionService.deploy(def.getId());

		// 记录桥接（D6）
		WfProcessDefinition after = defMapper.selectById(def.getId());
		FlowDefBridge bridge = new FlowDefBridge();
		bridge.setDefKey(def.getProcKey());
		bridge.setDefId(def.getId());
		bridge.setEngineDefKey(def.getProcKey());
		bridge.setEngineDefId(after.getProcDefId());
		bridge.setEngineVersion(extractVersion(after.getProcDefId()));
		bridge.setTenantId(def.getTenantId());
		bridge.setStatus(1);
		bridgeMapper.insert(bridge);

		log.info("[blade-workflow] 定义期回填完成并部署. defId={}, procKey={}, engineDefId={}",
			def.getId(), def.getProcKey(), after.getProcDefId());
	}

	private WfNodeExt buildNodeExt(WfProcessNode node) {
		WfNodeExt ext = new WfNodeExt();
		ext.nodeType = str(node.getNodeType());
		ext.signOrder = str(node.getSignOrder());
		ext.mergeType = str(node.getMergeType());
		ext.passNum = str(node.getPassNum());
		ext.allowReject = str(node.getAllowReject());
		ext.allowForward = str(node.getAllowForward());
		ext.autoApprove = str(node.getAutoApprove());
		ext.sortOrder = str(node.getSortOrder());
		ext.testStatus = str(node.getTestStatus());

		// 操作者（按 nodeId 关联；signOrder/batchNo/conditionJson 等超出冻结 schema 的字段暂未落扩展，
		// 必要时可在后续版本扩展 wf:operator 属性或并入节点 extJson）
		List<WfNodeOperator> ops = operatorMapper.selectList(
			new QueryWrapper<WfNodeOperator>().eq("node_id", node.getId()));
		for (WfNodeOperator op : ops) {
			WfOperatorExt o = new WfOperatorExt();
			o.groupNo = str(op.getGroupNo());
			o.opType = str(op.getOpType());
			o.objId = op.getObjId();
			o.bhxj = str(op.getBhxj());
			o.levelMin = str(op.getLevelMin());
			o.levelMax = str(op.getLevelMax());
			ext.operators.add(o);
		}

		// 字段权限（main 作用域 → fieldPerm；dt 作用域 → detailPerm）
		List<WfNodeFieldPerm> fps = fieldPermMapper.selectList(new QueryWrapper<WfNodeFieldPerm>()
			.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey()));
		for (WfNodeFieldPerm fp : fps) {
			String scope = fp.getScope();
			if (scope != null && scope.startsWith("dt")) {
				WfDetailPermExt d = new WfDetailPermExt();
				d.dtKey = scope;
				d.field = fp.getFieldName();
				d.perm = str(fp.getPerm());
				d.visible = str(fp.getIsVisible());
				d.editable = str(fp.getIsEditable());
				d.required = str(fp.getIsRequired());
				ext.detailPerms.add(d);
			} else {
				WfFieldPermExt f = new WfFieldPermExt();
				f.scope = (scope == null || scope.isBlank()) ? "main" : scope;
				f.field = fp.getFieldName();
				f.perm = str(fp.getPerm());
				f.visible = str(fp.getIsVisible());
				f.editable = str(fp.getIsEditable());
				f.required = str(fp.getIsRequired());
				ext.fieldPerms.add(f);
			}
		}

		// 明细表整表权限（canAdd/canEdit/canDelete 等）→ detailTablePerm 元素
		List<WfNodeDetailPerm> dtps = detailPermMapper.selectList(new QueryWrapper<WfNodeDetailPerm>()
			.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey()));
		for (WfNodeDetailPerm dp : dtps) {
			WfDetailTablePermExt d = new WfDetailTablePermExt();
			d.dtIndex = str(dp.getDtIndex());
			d.canAdd = str(dp.getCanAdd());
			d.canEdit = str(dp.getCanEdit());
			d.canDelete = str(dp.getCanDelete());
			d.hideEmpty = str(dp.getHideEmpty());
			d.defaultRows = str(dp.getDefaultRows());
			d.required = str(dp.getRequired());
			d.printSerial = str(dp.getPrintSerial());
			d.allowScroll = str(dp.getAllowScroll());
			d.openPaging = str(dp.getOpenPaging());
			ext.detailTablePerms.add(d);
		}

		// 明细表字段过滤（逐字段比较规则）→ detailFilter 元素
		List<WfNodeDetailFilter> dtfs = detailFilterMapper.selectList(new QueryWrapper<WfNodeDetailFilter>()
			.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey()));
		for (WfNodeDetailFilter df : dtfs) {
			WfDetailFilterExt d = new WfDetailFilterExt();
			d.dtIndex = str(df.getDtIndex());
			d.modeType = str(df.getModeType());
			d.fieldName = df.getFieldName();
			d.compareType = str(df.getCompareType());
			d.compareValue = df.getCompareValue();
			d.isRequired = str(df.getIsRequired());
			ext.detailFilters.add(d);
		}

		// 超时规则
		List<WfNodeTimeout> tos = timeoutMapper.selectList(new QueryWrapper<WfNodeTimeout>()
			.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey()).orderByAsc("seq"));
		for (WfNodeTimeout t : tos) {
			WfTimeoutExt e = new WfTimeoutExt();
			e.seq = str(t.getSeq());
			e.enabled = str(t.getEnabled());
			e.startType = str(t.getStartType());
			e.startField = t.getStartField();
			e.endType = str(t.getEndType());
			e.endFixedTime = t.getEndFixedTime();
			e.endField = t.getEndField();
			e.durationMin = str(t.getDurationMin());
			e.actionWay = t.getActionWay();
			e.opinion = t.getOpinion();
			e.operatorIds = t.getOperatorIds();
			e.remindBeforeOperator = str(t.getRemindBeforeOperator());
			e.remindTypes = t.getRemindTypes();
			e.remindPersons = t.getRemindPersons();
			ext.timeouts.add(e);
		}

		// 自定义操作（按钮 + 权限矩阵）
		List<WfCustomOperation> cos = customOperationMapper.selectList(new QueryWrapper<WfCustomOperation>()
			.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey()).orderByAsc("btn_order"));
		for (WfCustomOperation co : cos) {
			WfOperationExt o = new WfOperationExt();
			o.btnName = co.getBtnName();
			o.btnOrder = str(co.getBtnOrder());
			o.actionType = str(co.getActionType());
			o.enabled = str(co.getEnabled());
			List<WfCustomOperationRight> rights = customOperationRightMapper.selectList(
				new QueryWrapper<WfCustomOperationRight>().eq("op_id", co.getId()));
			for (WfCustomOperationRight r : rights) {
				WfRightExt re = new WfRightExt();
				re.rightType = r.getRightType();
				re.rightValue = r.getRightValue();
				o.rights.add(re);
			}
			ext.operations.add(o);
		}

		// 节点 extJson：保留原 WfProcessNode.extJson 并保全明细表级权限，确保迁移无损
		ext.extJson = buildNodeExtJson(node.getExtJson(), dtps);
		return ext;
	}

	// ---------------------------------------------------------------- 双轨对账

	private List<String> reconcileOne(WfProcessDefinition def) {
		List<String> diff = new ArrayList<>();
		String xml = def.getBpmnXml();
		if (xml == null || xml.isBlank() || !xml.contains(BpmnExtensionUtil.WF_NS)) {
			diff.add("BPMN 未含 wf: 扩展（回填缺失）");
			return diff;
		}
		BpmnModel model = parse(xml, def.getId());
		Process process = model.getMainProcess();
		if (process == null) {
			diff.add("BPMN 无主流程");
			return diff;
		}

		List<WfProcessNode> nodes = nodeMapper.selectList(new QueryWrapper<WfProcessNode>().eq("def_id", def.getId()));
		int srcNodeCount = nodes.size();
		int tgtNodeCount = countUserTasksWithWfNode(process);
		if (srcNodeCount != tgtNodeCount) {
			diff.add("节点数不一致 src=" + srcNodeCount + " tgt=" + tgtNodeCount);
		}
		for (WfProcessNode node : nodes) {
			UserTask task = (UserTask) process.getFlowElement(node.getNodeKey());
			if (task == null) {
				diff.add("节点 " + node.getNodeKey() + " 在 BPMN 无 userTask");
				continue;
			}
			WfNodeExt tgt = BpmnExtensionUtil.readNode(task);
			if (tgt == null) {
				diff.add("节点 " + node.getNodeKey() + " 在 BPMN 无 wf:node");
				continue;
			}
			int srcOps = operatorMapper.selectCount(
				new QueryWrapper<WfNodeOperator>().eq("node_id", node.getId())).intValue();
			if (srcOps != tgt.operators.size()) {
				diff.add("节点 " + node.getNodeKey() + " 操作者数不一致 src=" + srcOps + " tgt=" + tgt.operators.size());
			}
			int srcTos = timeoutMapper.selectCount(new QueryWrapper<WfNodeTimeout>()
				.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey())).intValue();
			if (srcTos != tgt.timeouts.size()) {
				diff.add("节点 " + node.getNodeKey() + " 超时数不一致 src=" + srcTos + " tgt=" + tgt.timeouts.size());
			}
			int srcCos = customOperationMapper.selectCount(new QueryWrapper<WfCustomOperation>()
				.eq("def_id", node.getDefId()).eq("node_key", node.getNodeKey())).intValue();
			if (srcCos != tgt.operations.size()) {
				diff.add("节点 " + node.getNodeKey() + " 自定义操作数不一致 src=" + srcCos + " tgt=" + tgt.operations.size());
			}
		}
		return diff;
	}

	private int countUserTasksWithWfNode(Process process) {
		int c = 0;
		for (FlowElement e : process.getFlowElements()) {
			if (e instanceof UserTask && BpmnExtensionUtil.firstExt(e, "node") != null) {
				c++;
			}
		}
		return c;
	}

	// ---------------------------------------------------------------- 工具

	private boolean isBackfilled(WfProcessDefinition def) {
		String xml = def.getBpmnXml();
		if (xml != null && xml.contains(BpmnExtensionUtil.WF_NS)) {
			return true;
		}
		Long cnt = bridgeMapper.selectCount(new QueryWrapper<FlowDefBridge>().eq("def_id", def.getId()));
		return cnt != null && cnt > 0;
	}

	private BpmnModel parse(String xml, Long defId) {
		try {
			return CONVERTER.convertToBpmnModel(
				() -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
		} catch (Exception e) {
			throw new RuntimeException("BPMN 解析失败 defId=" + defId + " : " + e.getMessage(), e);
		}
	}

	private SequenceFlow findSequenceFlow(Process process, String from, String to) {
		if (from == null || to == null) {
			return null;
		}
		SequenceFlow matched = null;
		int hits = 0;
		for (FlowElement e : process.getFlowElements()) {
			if (e instanceof SequenceFlow) {
				SequenceFlow f = (SequenceFlow) e;
				if (from.equals(f.getSourceRef()) && to.equals(f.getTargetRef())) {
					matched = f;
					hits++;
				}
			}
		}
		if (hits > 1) {
			log.warn("[blade-workflow] 存在多条 {}-{} 连线，取首条", from, to);
		}
		return matched;
	}

	private String buildNodeExtJson(String original, List<WfNodeDetailPerm> dtps) {
		try {
			ObjectNode root;
			if (original != null && !original.isBlank()) {
				try {
					root = (ObjectNode) OM.readTree(original);
				} catch (Exception e) {
					root = OM.createObjectNode();
					root.put("_original", original);
				}
			} else {
				root = OM.createObjectNode();
			}
			if (dtps != null && !dtps.isEmpty()) {
				root.set("_detailTablePerms", OM.valueToTree(dtps));
			}
			if (root.isEmpty()) {
				return null;
			}
			return OM.writeValueAsString(root);
		} catch (Exception e) {
			return original;
		}
	}

	private static String str(Object v) {
		return v == null ? null : v.toString();
	}

	private static Integer extractVersion(String procDefId) {
		if (procDefId == null) {
			return null;
		}
		Matcher m = PROCDEF_VERSION.matcher(procDefId);
		return m.find() ? Integer.valueOf(m.group(1)) : null;
	}
}
