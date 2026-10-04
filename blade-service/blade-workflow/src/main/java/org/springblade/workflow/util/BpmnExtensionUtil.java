package org.springblade.workflow.util;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BPMN {@code wf:} 扩展读写工具（T-3 定稿 schema 的承载实现）。
 *
 * <p>规范见 {@code doc/md/Flowable8承接台账模块-去wf_表改造分析.md} §二十（BPMN 扩展 schema 定稿）。定义期语义（节点/出口/操作者/字段权限/
 * 明细权限/明细过滤/超时/自定义动作/自定义操作/流程级元数据）全部以 {@code wf:} 命名空间写入
 * BPMN {@code extensionElements}，随 {@code ACT_GE_BYTEARRAY} 持久化/版本化，不再落 {@code wf_*} 表。</p>
 *
 * <p><b>命名约束</b>：元素 localName 不得与 Flowable 已注册名冲突（见 schema 文档 §3）。</p>
 */
public final class BpmnExtensionUtil {

	public static final String WF_NS = "http://www.springblade.org/workflow";
	public static final String WF_PREFIX = "wf";
	/** 流程级「折叠连线」元素名（A→网关→B 折叠为 A→B，BPMN 无对应 sequenceFlow） */
	public static final String FOLDED_LINK = "foldedLink";

	private BpmnExtensionUtil() {
	}

	// ---------------------------------------------------------- 通用读写助手

	/** 取首个指定 localName 的扩展元素（FlowElement / Process 的直接 extensionElements） */
	public static ExtensionElement firstExt(BaseElement element, String localName) {
		List<ExtensionElement> list = element.getExtensionElements().get(localName);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	/** 取 BaseElement 下全部指定 localName 的扩展元素 */
	public static List<ExtensionElement> allExt(BaseElement element, String localName) {
		List<ExtensionElement> list = element.getExtensionElements().get(localName);
		return list == null ? Collections.emptyList() : list;
	}

	/** 取 ExtensionElement 的子元素（其专属 childElements） */
	public static ExtensionElement firstChild(ExtensionElement parent, String localName) {
		List<ExtensionElement> list = parent.getChildElements().get(localName);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	/** 取 ExtensionElement 下全部指定 localName 的子元素 */
	public static List<ExtensionElement> allChild(ExtensionElement parent, String localName) {
		List<ExtensionElement> list = parent.getChildElements().get(localName);
		return list == null ? Collections.emptyList() : list;
	}

	public static String attr(ExtensionElement element, String name) {
		List<ExtensionAttribute> list = element.getAttributes().get(name);
		return (list == null || list.isEmpty()) ? null : list.get(0).getValue();
	}

	private static void setAttr(ExtensionElement element, String name, Object value) {
		if (value == null) {
			return;
		}
		String str = value.toString();
		if (str.isEmpty()) {
			return;
		}
		ExtensionAttribute a = new ExtensionAttribute(name, str);
		a.setNamespace(WF_NS);
		a.setNamespacePrefix(WF_PREFIX);
		element.getAttributes().put(name, Collections.singletonList(a));
	}

	private static ExtensionElement newElement(String localName) {
		ExtensionElement e = new ExtensionElement();
		e.setName(localName);
		e.setNamespace(WF_NS);
		e.setNamespacePrefix(WF_PREFIX);
		return e;
	}

	/**
	 * 在 BaseElement 的 extensionElements 下新增/复用首个指定 localName 的扩展元素（用于写回）。
	 * 复用时先清空其既有子元素与属性，保证对同一元素重复写回时幂等、不重复叠加。
	 */
	private static ExtensionElement ensureFirstExt(BaseElement element, String localName) {
		ExtensionElement existing = firstExt(element, localName);
		if (existing != null) {
			existing.getChildElements().clear();
			existing.getAttributes().clear();
			return existing;
		}
		ExtensionElement e = newElement(localName);
		element.addExtensionElement(e);
		return e;
	}

	// ---------------------------------------------------------- wf:node 读

	public static WfNodeExt readNode(UserTask task) {
		ExtensionElement node = firstExt(task, "node");
		if (node == null) {
			return null;
		}
		WfNodeExt ext = new WfNodeExt();
		ext.nodeType = attr(node, "nodeType");
		ext.signOrder = attr(node, "signOrder");
		ext.mergeType = attr(node, "mergeType");
		ext.passNum = attr(node, "passNum");
		ext.allowReject = attr(node, "allowReject");
		ext.allowForward = attr(node, "allowForward");
		ext.autoApprove = attr(node, "autoApprove");
		ext.sortOrder = attr(node, "sortOrder");
		ext.testStatus = attr(node, "testStatus");
		ext.multiInstance = attr(node, "multiInstance");
		ext.formKey = attr(node, "formKey");

		for (ExtensionElement op : allChild(node, "operator")) {
			WfOperatorExt o = new WfOperatorExt();
			o.groupNo = attr(op, "groupNo");
			o.opType = attr(op, "opType");
			o.objId = attr(op, "objId");
			o.bhxj = attr(op, "bhxj");
			o.levelMin = attr(op, "levelMin");
			o.levelMax = attr(op, "levelMax");
			o.signOrder = attr(op, "signOrder");
			o.batchNo = attr(op, "batchNo");
			o.groupName = attr(op, "groupName");
			o.canView = attr(op, "canView");
			o.conditionJson = attr(op, "conditionJson");
			o.isCoadjutant = attr(op, "isCoadjutant");
			o.coadjutants = attr(op, "coadjutants");
			o.isPending = attr(op, "isPending");
			o.isModify = attr(op, "isModify");
			o.signType = attr(op, "signType");
			ext.operators.add(o);
		}
		for (ExtensionElement fp : allChild(node, "fieldPerm")) {
			WfFieldPermExt f = new WfFieldPermExt();
			f.scope = attr(fp, "scope");
			f.field = attr(fp, "field");
			f.perm = attr(fp, "perm");
			f.visible = attr(fp, "visible");
			f.editable = attr(fp, "editable");
			f.required = attr(fp, "required");
			ext.fieldPerms.add(f);
		}
		for (ExtensionElement dp : allChild(node, "detailPerm")) {
			WfDetailPermExt d = new WfDetailPermExt();
			d.dtKey = attr(dp, "dtKey");
			d.field = attr(dp, "field");
			d.perm = attr(dp, "perm");
			d.visible = attr(dp, "visible");
			d.editable = attr(dp, "editable");
			d.required = attr(dp, "required");
			ext.detailPerms.add(d);
		}
		for (ExtensionElement df : allChild(node, "detailFilter")) {
			WfDetailFilterExt d = new WfDetailFilterExt();
			d.dtIndex = attr(df, "dtIndex");
			d.modeType = attr(df, "modeType");
			d.fieldName = attr(df, "fieldName");
			d.compareType = attr(df, "compareType");
			d.compareValue = attr(df, "compareValue");
			d.isRequired = attr(df, "isRequired");
			ext.detailFilters.add(d);
		}
		for (ExtensionElement dp : allChild(node, "detailTablePerm")) {
			WfDetailTablePermExt d = new WfDetailTablePermExt();
			d.dtIndex = attr(dp, "dtIndex");
			d.canAdd = attr(dp, "canAdd");
			d.canEdit = attr(dp, "canEdit");
			d.canDelete = attr(dp, "canDelete");
			d.hideEmpty = attr(dp, "hideEmpty");
			d.defaultRows = attr(dp, "defaultRows");
			d.required = attr(dp, "required");
			d.printSerial = attr(dp, "printSerial");
			d.allowScroll = attr(dp, "allowScroll");
			d.openPaging = attr(dp, "openPaging");
			ext.detailTablePerms.add(d);
		}
		for (ExtensionElement to : allChild(node, "timeout")) {
			WfTimeoutExt t = new WfTimeoutExt();
			t.seq = attr(to, "seq");
			t.enabled = attr(to, "enabled");
			t.startType = attr(to, "startType");
			t.startField = attr(to, "startField");
			t.endType = attr(to, "endType");
			t.endFixedTime = attr(to, "endFixedTime");
			t.endField = attr(to, "endField");
			t.durationMin = attr(to, "durationMin");
			t.actionWay = attr(to, "actionWay");
			t.opinion = attr(to, "opinion");
			t.operatorIds = attr(to, "operatorIds");
			t.remindBeforeOperator = attr(to, "remindBeforeOperator");
			t.remindTypes = attr(to, "remindTypes");
			t.remindPersons = attr(to, "remindPersons");
			ext.timeouts.add(t);
		}
		for (ExtensionElement ca : allChild(node, "customAction")) {
			WfCustomActionExt c = new WfCustomActionExt();
			c.actionKey = attr(ca, "actionKey");
			c.name = attr(ca, "name");
			c.type = attr(ca, "type");
			c.url = attr(ca, "url");
			c.expression = attr(ca, "expression");
			ext.customActions.add(c);
		}
		for (ExtensionElement op : allChild(node, "operation")) {
			WfOperationExt o = new WfOperationExt();
			o.btnName = attr(op, "btnName");
			o.btnOrder = attr(op, "btnOrder");
			o.actionType = attr(op, "actionType");
			o.enabled = attr(op, "enabled");
			// 动作明细（前端 CustomOperationModal：URL/流程操作/接口 三种动作类型的参数）
			o.url = attr(op, "url");
			o.httpMethod = attr(op, "httpMethod");
			o.paramExpr = attr(op, "paramExpr");
			o.flowOperation = attr(op, "flowOperation");
			o.interfaceName = attr(op, "interfaceName");
			o.opinion = attr(op, "opinion");
			for (ExtensionElement r : allChild(op, "right")) {
				WfRightExt right = new WfRightExt();
				right.rightType = attr(r, "rightType");
				right.rightValue = attr(r, "rightValue");
				o.rights.add(right);
			}
			ext.operations.add(o);
		}
		ExtensionElement extJson = firstChild(node, "extJson");
		if (extJson != null) {
			ext.extJson = extJson.getElementText();
		}
		return ext;
	}

	// ---------------------------------------------------------- wf:node 写

	public static void writeNode(UserTask task, WfNodeExt ext) {
		ExtensionElement node = ensureFirstExt(task, "node");
		setAttr(node, "nodeType", ext.nodeType);
		setAttr(node, "signOrder", ext.signOrder);
		setAttr(node, "mergeType", ext.mergeType);
		setAttr(node, "passNum", ext.passNum);
		setAttr(node, "allowReject", ext.allowReject);
		setAttr(node, "allowForward", ext.allowForward);
		setAttr(node, "autoApprove", ext.autoApprove);
		setAttr(node, "sortOrder", ext.sortOrder);
		setAttr(node, "testStatus", ext.testStatus);
		setAttr(node, "multiInstance", ext.multiInstance);
		setAttr(node, "formKey", ext.formKey);

		for (WfOperatorExt o : ext.operators) {
			ExtensionElement e = newElement("operator");
			setAttr(e, "groupNo", o.groupNo);
			setAttr(e, "opType", o.opType);
			setAttr(e, "objId", o.objId);
			setAttr(e, "bhxj", o.bhxj);
			setAttr(e, "levelMin", o.levelMin);
			setAttr(e, "levelMax", o.levelMax);
			setAttr(e, "signOrder", o.signOrder);
			setAttr(e, "batchNo", o.batchNo);
			setAttr(e, "groupName", o.groupName);
			setAttr(e, "canView", o.canView);
			setAttr(e, "conditionJson", o.conditionJson);
			setAttr(e, "isCoadjutant", o.isCoadjutant);
			setAttr(e, "coadjutants", o.coadjutants);
			setAttr(e, "isPending", o.isPending);
			setAttr(e, "isModify", o.isModify);
			setAttr(e, "signType", o.signType);
			node.addChildElement(e);
		}
		for (WfFieldPermExt f : ext.fieldPerms) {
			ExtensionElement e = newElement("fieldPerm");
			setAttr(e, "scope", f.scope);
			setAttr(e, "field", f.field);
			setAttr(e, "perm", f.perm);
			setAttr(e, "visible", f.visible);
			setAttr(e, "editable", f.editable);
			setAttr(e, "required", f.required);
			node.addChildElement(e);
		}
		for (WfDetailPermExt d : ext.detailPerms) {
			ExtensionElement e = newElement("detailPerm");
			setAttr(e, "dtKey", d.dtKey);
			setAttr(e, "field", d.field);
			setAttr(e, "perm", d.perm);
			setAttr(e, "visible", d.visible);
			setAttr(e, "editable", d.editable);
			setAttr(e, "required", d.required);
			node.addChildElement(e);
		}
		for (WfDetailFilterExt d : ext.detailFilters) {
			ExtensionElement e = newElement("detailFilter");
			setAttr(e, "dtIndex", d.dtIndex);
			setAttr(e, "modeType", d.modeType);
			setAttr(e, "fieldName", d.fieldName);
			setAttr(e, "compareType", d.compareType);
			setAttr(e, "compareValue", d.compareValue);
			setAttr(e, "isRequired", d.isRequired);
			node.addChildElement(e);
		}
		for (WfDetailTablePermExt d : ext.detailTablePerms) {
			ExtensionElement e = newElement("detailTablePerm");
			setAttr(e, "dtIndex", d.dtIndex);
			setAttr(e, "canAdd", d.canAdd);
			setAttr(e, "canEdit", d.canEdit);
			setAttr(e, "canDelete", d.canDelete);
			setAttr(e, "hideEmpty", d.hideEmpty);
			setAttr(e, "defaultRows", d.defaultRows);
			setAttr(e, "required", d.required);
			setAttr(e, "printSerial", d.printSerial);
			setAttr(e, "allowScroll", d.allowScroll);
			setAttr(e, "openPaging", d.openPaging);
			node.addChildElement(e);
		}
		for (WfTimeoutExt t : ext.timeouts) {
			ExtensionElement e = newElement("timeout");
			setAttr(e, "seq", t.seq);
			setAttr(e, "enabled", t.enabled);
			setAttr(e, "startType", t.startType);
			setAttr(e, "startField", t.startField);
			setAttr(e, "endType", t.endType);
			setAttr(e, "endFixedTime", t.endFixedTime);
			setAttr(e, "endField", t.endField);
			setAttr(e, "durationMin", t.durationMin);
			setAttr(e, "actionWay", t.actionWay);
			setAttr(e, "opinion", t.opinion);
			setAttr(e, "operatorIds", t.operatorIds);
			setAttr(e, "remindBeforeOperator", t.remindBeforeOperator);
			setAttr(e, "remindTypes", t.remindTypes);
			setAttr(e, "remindPersons", t.remindPersons);
			node.addChildElement(e);
		}
		for (WfCustomActionExt c : ext.customActions) {
			ExtensionElement e = newElement("customAction");
			setAttr(e, "actionKey", c.actionKey);
			setAttr(e, "name", c.name);
			setAttr(e, "type", c.type);
			setAttr(e, "url", c.url);
			setAttr(e, "expression", c.expression);
			node.addChildElement(e);
		}
		for (WfOperationExt o : ext.operations) {
			ExtensionElement e = newElement("operation");
			setAttr(e, "btnName", o.btnName);
			setAttr(e, "btnOrder", o.btnOrder);
			setAttr(e, "actionType", o.actionType);
			setAttr(e, "enabled", o.enabled);
			// 动作明细
			setAttr(e, "url", o.url);
			setAttr(e, "httpMethod", o.httpMethod);
			setAttr(e, "paramExpr", o.paramExpr);
			setAttr(e, "flowOperation", o.flowOperation);
			setAttr(e, "interfaceName", o.interfaceName);
			setAttr(e, "opinion", o.opinion);
			for (WfRightExt r : o.rights) {
				ExtensionElement re = newElement("right");
				setAttr(re, "rightType", r.rightType);
				setAttr(re, "rightValue", r.rightValue);
				e.addChildElement(re);
			}
			node.addChildElement(e);
		}
		if (ext.extJson != null && !ext.extJson.isEmpty()) {
			ExtensionElement e = newElement("extJson");
			e.setElementText(ext.extJson);
			node.addChildElement(e);
		}
	}

	// ---------------------------------------------------------- wf:link 读/写

	public static WfLinkExt readLink(SequenceFlow flow) {
		ExtensionElement link = firstExt(flow, "link");
		if (link == null) {
			return null;
		}
		WfLinkExt ext = new WfLinkExt();
		ext.isReject = attr(link, "isReject");
		ext.isMustPass = attr(link, "isMustPass");
		ext.conditionCn = attr(link, "conditionCn");
		ext.sortOrder = attr(link, "sortOrder");
		ext.viaGateway = attr(link, "viaGateway");
		ext.viaGatewayKey = attr(link, "viaGatewayKey");
		ExtensionElement extra = firstChild(link, "extraOperations");
		if (extra != null) {
			ext.extraOperations = extra.getElementText();
		}
		return ext;
	}

	public static void writeLink(SequenceFlow flow, WfLinkExt ext) {
		ExtensionElement link = ensureFirstExt(flow, "link");
		setAttr(link, "isReject", ext.isReject);
		setAttr(link, "isMustPass", ext.isMustPass);
		setAttr(link, "conditionCn", ext.conditionCn);
		setAttr(link, "sortOrder", ext.sortOrder);
		setAttr(link, "viaGateway", ext.viaGateway);
		setAttr(link, "viaGatewayKey", ext.viaGatewayKey);
		if (ext.extraOperations != null && !ext.extraOperations.isEmpty()) {
			ExtensionElement e = newElement("extraOperations");
			e.setElementText(ext.extraOperations);
			link.addChildElement(e);
		}
	}

	// ---------------------------------------------------------- wf:processMeta 读/写

	public static WfProcessMetaExt readProcessMeta(Process process) {
		ExtensionElement meta = firstExt(process, "processMeta");
		if (meta == null) {
			return null;
		}
		WfProcessMetaExt ext = new WfProcessMetaExt();
		ext.defKey = attr(meta, "defKey");
		ext.workflowType = attr(meta, "workflowType");
		ext.formId = attr(meta, "formId");
		ext.layoutId = attr(meta, "layoutId");
		ext.grayEnabled = attr(meta, "grayEnabled");
		ext.grayRule = attr(meta, "grayRule");
		return ext;
	}

	public static void writeProcessMeta(Process process, WfProcessMetaExt ext) {
		ExtensionElement meta = ensureFirstExt(process, "processMeta");
		setAttr(meta, "defKey", ext.defKey);
		setAttr(meta, "workflowType", ext.workflowType);
		setAttr(meta, "formId", ext.formId);
		setAttr(meta, "layoutId", ext.layoutId);
		setAttr(meta, "grayEnabled", ext.grayEnabled);
		setAttr(meta, "grayRule", ext.grayRule);
	}

	// ---------------------------------------------------------- 模型类

	public static class WfNodeExt {
		public String nodeType, signOrder, mergeType, passNum, allowReject, allowForward,
			autoApprove, sortOrder, testStatus, multiInstance, formKey, extJson;
		public final List<WfOperatorExt> operators = new ArrayList<>();
		public final List<WfFieldPermExt> fieldPerms = new ArrayList<>();
		public final List<WfDetailPermExt> detailPerms = new ArrayList<>();
		public final List<WfDetailTablePermExt> detailTablePerms = new ArrayList<>();
		public final List<WfDetailFilterExt> detailFilters = new ArrayList<>();
		public final List<WfTimeoutExt> timeouts = new ArrayList<>();
		public final List<WfCustomActionExt> customActions = new ArrayList<>();
		public final List<WfOperationExt> operations = new ArrayList<>();
	}

	public static class WfOperatorExt {
		public String groupNo, opType, objId, bhxj, levelMin, levelMax,
			signOrder, batchNo, groupName, canView, conditionJson,
			isCoadjutant, coadjutants, isPending, isModify, signType;
	}

	public static class WfFieldPermExt {
		public String scope, field, perm, visible, editable, required;
	}

	public static class WfDetailPermExt {
		public String dtKey, field, perm, visible, editable, required;
	}

	/** 明细表字段筛选规则（BPMN 元素 detailFilter）：对应 {@code wf_node_detail_filter} 的逐字段比较规则 */
	public static class WfDetailFilterExt {
		public String dtIndex, modeType, fieldName, compareType, compareValue, isRequired;
	}

	/** 节点级明细表整表权限（BPMN 元素 detailTablePerm）：对应 {@code wf_node_detail_perm}（canAdd/canEdit/…/openPaging） */
	public static class WfDetailTablePermExt {
		public String dtIndex, canAdd, canEdit, canDelete, hideEmpty, defaultRows,
			required, printSerial, allowScroll, openPaging;
	}

	public static class WfTimeoutExt {
		public String seq, enabled, startType, startField, endType, endFixedTime, endField,
			durationMin, actionWay, opinion, operatorIds, remindBeforeOperator, remindTypes, remindPersons;
	}

	public static class WfCustomActionExt {
		public String actionKey, name, type, url, expression;
	}

	public static class WfOperationExt {
		public String btnName, btnOrder, actionType, enabled;
		/** 动作明细：按 actionType 使用（1=URL 取 url/httpMethod/paramExpr，2=流程操作 取 flowOperation/opinion，3=接口 取 interfaceName/paramExpr） */
		public String url, httpMethod, paramExpr, flowOperation, interfaceName, opinion;
		public final List<WfRightExt> rights = new ArrayList<>();
	}

	public static class WfRightExt {
		public String rightType, rightValue;
	}

	public static class WfLinkExt {
		public String isReject, isMustPass, conditionCn, sortOrder, viaGateway, viaGatewayKey, extraOperations;
	}

	// ---------------------------------------------------------- wf:foldedLink 读/写
	//
	// 背景：wf_node_link 含「A→网关→B 折叠为 A→B」的【合成连线】——BPMN 里真实存在的是
	// A→网关 与 网关→B 两条 sequenceFlow，并不存在 A→B，因此折叠连线无法挂在 sequenceFlow 上，
	// 只能作为【流程级】扩展元素承载（挂在 <process> 的 extensionElements 下）。
	// 回填作业时若按 findSequenceFlow(A,B) 查找会查不到而被跳过，导致折叠连线丢失 —— 本元素即为其载体。

	/** 取流程级全部 wf:foldedLink 元素 */
	public static List<WfFoldedLinkExt> readFoldedLinks(Process process) {
		List<WfFoldedLinkExt> result = new ArrayList<>();
		if (process == null) {
			return result;
		}
		for (ExtensionElement e : allExt(process, FOLDED_LINK)) {
			WfFoldedLinkExt f = new WfFoldedLinkExt();
			f.from = attr(e, "from");
			f.to = attr(e, "to");
			f.viaGatewayKey = attr(e, "viaGatewayKey");
			f.isReject = attr(e, "isReject");
			f.isMustPass = attr(e, "isMustPass");
			f.conditionCn = attr(e, "conditionCn");
			f.sortOrder = attr(e, "sortOrder");
			ExtensionElement extra = firstChild(e, "extraOperations");
			if (extra != null) {
				f.extraOperations = extra.getElementText();
			}
			result.add(f);
		}
		return result;
	}

	/**
	 * 写流程级 wf:foldedLink（覆盖式：先清空既有再写入，保证重复回填幂等、不叠加）。
	 */
	public static void writeFoldedLinks(Process process, List<WfFoldedLinkExt> list) {
		if (process == null) {
			return;
		}
		List<ExtensionElement> existing = process.getExtensionElements().get(FOLDED_LINK);
		if (existing != null) {
			existing.clear();
		}
		if (list == null || list.isEmpty()) {
			return;
		}
		for (WfFoldedLinkExt f : list) {
			ExtensionElement e = newElement(FOLDED_LINK);
			setAttr(e, "from", f.from);
			setAttr(e, "to", f.to);
			setAttr(e, "viaGatewayKey", f.viaGatewayKey);
			setAttr(e, "isReject", f.isReject);
			setAttr(e, "isMustPass", f.isMustPass);
			setAttr(e, "conditionCn", f.conditionCn);
			setAttr(e, "sortOrder", f.sortOrder);
			if (f.extraOperations != null && !f.extraOperations.isEmpty()) {
				ExtensionElement x = newElement("extraOperations");
				x.setElementText(f.extraOperations);
				e.addChildElement(x);
			}
			process.addExtensionElement(e);
		}
	}

	public static class WfFoldedLinkExt {
		public String from, to, viaGatewayKey;
		public String isReject, isMustPass, conditionCn, sortOrder, extraOperations;
	}

	public static class WfProcessMetaExt {
		public String defKey, workflowType, formId, layoutId, grayEnabled, grayRule;
	}
}
