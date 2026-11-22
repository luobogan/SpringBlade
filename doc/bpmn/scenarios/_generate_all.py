#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate the full BPMN scenario library for the springblade 'flowtable' workflow platform.

One .bpmn20.xml per situation, each importable by the designer and deployable to the engine.
Scenarios that need a compat switch are flagged in the file header comment.

Run:  python _generate_all.py
Then: python <skill>/scripts/validate_bpmn.py <each file>
"""
import os
import re

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
A_INIT = "${initiator}"

# ---------------------------------------------------------------------------
# Real dev-environment IDs. The engine's WfOperatorResolver#parseIds DROPS any
# non-numeric objId and queries role/user/dept/post by numeric id, so placeholder
# ids (finance_role_id, 1001, ...) resolve to empty and the node is skipped.
# These mappings keep the scenarios human-readable while emitting deployable XML.
# Adjust this block if your dev IDs differ (see log/wf_real_ids.py).
# ---------------------------------------------------------------------------
REAL = {
    # roles (opType=2/43) -> real numeric role_id
    "finance_role_id": "4000000029",   # wf_fin_review
    "dept_mgr_role_id": "4000000031",  # role_dept_leader
    "vice_president_role_id": "4000000026",  # wf_vice_leader
    "gm_role_id": "4000000027",        # wf_general_manager
    "finance_role": "4000000029",
    "supervisor_role_id": "4000000031",
    "hr_role_id": "4000000001",        # workflow (admin holds it)
    "fin_role_id": "4000000028",       # wf_fin_manager
    "hr_reviewer_role_id": "4000000001",
    "reviewer_role_id": "4000000001",
    "senior_role_id": "4000000027",
    "confirm_role_id": "4000000001",
    "fallback_leader_role_id": "4000000031",
    "approve_role_id": "4000000001",
    "main_role_id": "4000000001",
    "timeout_role_id": "4000000001",
    "matrix_role": "4000000001",
    "role_a": "4000000001", "role_b": "4000000001",
    "role_c": "4000000001", "role_d": "4000000001",
    # posts (opType=58) -> real post_id
    "post_manager_id": "3000000002",   # 总裁
    # depts (opType=1) -> real dept_id
    "20000000001": "2000000003",       # 董事长办公室
    # matrix (opType=99)
    "matrix_demo_id": "2110186695577083905",  # E2E矩阵测试 (log/matrix_e2e_approval)
}
# Named users (opType=3) placeholders -> real user ids
USER_MAP = {
    "1001": "676", "1002": "3", "1003": "4",
    "2001": "676", "2002": "3", "2003": "4",
    "3001": "676", "4001": "3", "4002": "4",
    "5001": "676",
}


def _norm_objid(otype, objid):
    """Map a placeholder objId to a real dev id based on operator type."""
    if not objid:
        return objid
    otype = str(otype)
    out = []
    for part in re.split(r"[,，;；\s]+", objid):
        if not part:
            continue
        if part in REAL:
            out.append(REAL[part])
        elif otype == "3" and part in USER_MAP:
            out.append(USER_MAP[part])
        elif part.isdigit():
            out.append(part)  # already numeric
        else:
            out.append(part)  # leave unknown tokens (field names for opType=5 etc.)
    return ",".join(out)


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def op(otype, objid="", sign=0, bhxj=0, extra=""):
    objid = _norm_objid(otype, objid)
    return ('      <wf:operator opType="%s" objId="%s" signOrder="%s" bhxj="%s"%s/>'
            % (otype, objid, sign, bhxj, extra))


def fp(field, perm, scope="main"):
    return '      <wf:fieldPerm scope="%s" field="%s" perm="%s"/>' % (scope, field, perm)


def tm(dur=1440, way="remind", remind="sys", seq=1):
    return ('      <wf:timeout seq="%s" enabled="1" startType="1" endType="1" '
            'durationMin="%s" actionWay="%s" remindTypes="%s"/>' % (seq, dur, way, remind))


def reject(target, label):
    return '      <wf:reject targetNodeKey="%s" label="%s"/>' % (target, label)


def start(eid="StartEvent_1", layer=0, lane=0):
    return (eid, '    <startEvent id="%s" name="开始" flowable:initiator="initiator"/>' % eid,
            layer, lane, 30, 30, "event")


def end(eid, name="归档", layer=0, lane=0, node_type="3"):
    xml = ('    <endEvent id="%s" name="%s">\n'
           '      <extensionElements><wf:node nodeType="%s"/></extensionElements>\n'
           '    </endEvent>' % (eid, name, node_type))
    return (eid, xml, layer, lane, 30, 30, "event")


def utask(eid, name, node_type=1, ops=(), fps=(), tos=(), rejects=(), doc=None,
          assignee=None, groups=None, sign=0, allow_reject=1, auto_approve=0,
          extra="", layer=0, lane=0):
    if groups:
        attrs = ' flowable:candidateGroups="%s"' % groups
    elif assignee:
        attrs = ' flowable:assignee="%s"' % assignee
    else:
        attrs = ' flowable:candidateGroups="default_role"'
    doc_xml = ('      <documentation>%s</documentation>\n' % doc) if doc else ""
    children = []
    children.extend(list(ops))
    children.extend(list(fps))
    children.extend(list(tos))
    children.extend(list(rejects))
    children.append(extra) if extra else None
    child_xml = ("\n".join(children) + "\n") if children else ""
    xml = ('    <userTask id="%s" name="%s"%s>\n'
           '%s'
           '      <extensionElements>\n'
           '        <wf:node nodeType="%s" signOrder="%s" allowReject="%s" autoApprove="%s">\n'
           '%s'
           '        </wf:node>\n'
           '      </extensionElements>\n'
           '    </userTask>' % (eid, name, attrs, doc_xml, node_type, sign,
                                allow_reject, auto_approve, child_xml))
    return (eid, xml, layer, lane, 120, 80, "task")


def gateway(eid, name, kind="exclusive", default=None, layer=0, lane=0):
    d = ' default="%s"' % default if default else ""
    return (eid, '    <%sGateway id="%s" name="%s"%s/>' % (kind, eid, name, d),
            layer, lane, 40, 50, "gateway")


def auto_task(eid, name, tag="serviceTask", impl=None, script_format=None,
              script=None, node_type="6", extra_nodes="", result_variable=None,
              layer=0, lane=0):
    if tag == "serviceTask":
        impl_attr = (' flowable:delegateExpression="%s"' % impl) if impl else ""
        inner = ""
    elif tag == "scriptTask":
        impl_attr = ' scriptFormat="%s"' % (script_format or "groovy")
        if result_variable:
            impl_attr += ' flowable:resultVariable="%s"' % result_variable
        inner = "      <script><![CDATA[%s]]></script>\n" % (script or "")
    elif tag == "receiveTask":
        impl_attr = ""
        inner = ""
    else:
        impl_attr = ""
        inner = ""
    xml = ('    <%s id="%s" name="%s"%s>\n'
           '      <extensionElements>\n'
           '        <wf:node nodeType="%s" signOrder="0" allowReject="0" autoApprove="0">\n'
           '%s'
           '        </wf:node>\n'
           '      </extensionElements>\n'
           '%s'
           '    </%s>' % (tag, eid, name, impl_attr, node_type, extra_nodes, inner, tag))
    return (eid, xml, layer, lane, 120, 80, "task")


class B:
    def __init__(self, pid, name, notes):
        self.pid, self.name, self.notes = pid, name, notes
        self.nodes = []
        self.flows = []

    def add(self, spec):
        eid, xml, layer, lane, w, h, kind = spec
        self.nodes.append(dict(id=eid, xml=xml, x=100 + layer * 210, y=60 + lane * 150,
                               w=w, h=h, kind=kind))
        return self

    def flow(self, eid, src, tgt, name="", cond=None):
        self.flows.append(dict(id=eid, src=src, tgt=tgt, name=name, cond=cond))
        return self

    def _points(self, f):
        src = next(n for n in self.nodes if n["id"] == f["src"])
        tgt = next(n for n in self.nodes if n["id"] == f["tgt"])
        sx, sy, sw, sh = src["x"], src["y"], src["w"], src["h"]
        tx, ty, tw, th = tgt["x"], tgt["y"], tgt["w"], tgt["h"]
        if tx >= sx:
            x1, y1 = sx + sw, sy + sh / 2.0
            x2, y2 = tx, ty + th / 2.0
            if abs(y1 - y2) < 1:
                pts = [(x1, y1), (x2, y2)]
            else:
                xm = (x1 + x2) / 2.0
                pts = [(x1, y1), (xm, y1), (xm, y2), (x2, y2)]
        else:
            x1, y1 = sx + sw / 2.0, sy + sh
            x2, y2 = tx + tw / 2.0, ty + th
            ym = max(y1, y2) + 40
            pts = [(x1, y1), (x1, ym), (x2, ym), (x2, y2)]
        return "".join('<omgdi:waypoint x="%s" y="%s"/>' % (int(x), int(y)) for x, y in pts)

    def xml(self):
        head = ['<?xml version="1.0" encoding="UTF-8"?>', '<!--']
        head.append('  ============================================================================')
        head.append('  SCENARIO: %s' % self.name)
        head.append('  PROCESS_ID: %s' % self.pid)
        for n in self.notes:
            head.append('  %s' % n)
        head.append('  Generated by doc/bpmn/scenarios/_generate_all.py')
        head.append('  ============================================================================')
        head.append('-->')
        head.append('<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"')
        head.append('             xmlns:flowable="http://flowable.org/bpmn"')
        head.append('             xmlns:wf="http://www.springblade.org/workflow"')
        head.append('             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"')
        head.append('             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"')
        head.append('             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"')
        head.append('             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')
        head.append('             targetNamespace="http://www.springblade.org/workflow"')
        head.append('             xsi:schemaLocation="http://www.omg.org/spec/BPMN/20100524/MODEL '
                    'http://www.omg.org/spec/BPMN/20100524/DI/BPMN20.xsd">')
        head.append('')
        head.append('  <process id="%s" name="%s" isExecutable="true">' % (self.pid, self.name))
        for n in self.nodes:
            head.append(n["xml"])
        for f in self.flows:
            cond = f["cond"]
            if cond:
                head.append('    <sequenceFlow id="%s" name="%s" sourceRef="%s" targetRef="%s">'
                            % (f["id"], f["name"], f["src"], f["tgt"]))
                # cond is already XML-escaped at the call site (&gt; &lt; &amp;&amp;);
                # do NOT esc() again or UEL receives a literal "&gt;" it cannot eval.
                head.append('      <conditionExpression xsi:type="tFormalExpression">%s</conditionExpression>'
                            % cond)
                head.append('    </sequenceFlow>')
            else:
                head.append('    <sequenceFlow id="%s" name="%s" sourceRef="%s" targetRef="%s"/>'
                            % (f["id"], f["name"], f["src"], f["tgt"]))
        head.append('  </process>')
        head.append('')
        head.append('  <bpmndi:BPMNDiagram id="BPMNDiagram_1">')
        head.append('    <bpmndi:BPMNPlane bpmnElement="%s" id="BPMNPlane_1">' % self.pid)
        for n in self.nodes:
            head.append('      <bpmndi:BPMNShape bpmnElement="%s" id="%s_di">' % (n["id"], n["id"]))
            head.append('        <omgdc:Bounds x="%s" y="%s" width="%s" height="%s"/>'
                        % (n["x"], n["y"], n["w"], n["h"]))
            head.append('      </bpmndi:BPMNShape>')
        for f in self.flows:
            head.append('      <bpmndi:BPMNEdge bpmnElement="%s" id="%s_di">%s</bpmndi:BPMNEdge>'
                        % (f["id"], f["id"], self._points(f)))
        head.append('    </bpmndi:BPMNPlane>')
        head.append('  </bpmndi:BPMNDiagram>')
        head.append('')
        head.append('</definitions>')
        return "\n".join(head) + "\n"


def save(b, filename):
    path = os.path.join(OUT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(b.xml())
    print("written:", filename)


# ---------------------------------------------------------------------------
# 01 basic approval chain
# ---------------------------------------------------------------------------
def s01():
    b = B("sc_basic_approval", "01 基础审批主链",
          ["COVERS: start -> create(nodeType=0,opType=17) -> approve(nodeType=1,opType=2) -> archive(nodeType=3)",
           "DEPLOY: default config, no compat switch required.",
           "NOTE: create node is a real userTask auto-completed on start; not a startEvent."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT,
                fps=[fp("title", 3), fp("amount", 3)], layer=1, lane=0))
    b.add(utask("Task_Approve", "部门审批", 1, ops=[op(2, "finance_role_id")],
                groups="finance_role_id", fps=[fp("amount", 1)],
                tos=[tm(dur=1440)], layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Approve", "提交")
    b.flow("Flow_3", "Task_Approve", "EndEvent_1", "通过")
    save(b, "01_basic_approval.bpmn20.xml")


# ---------------------------------------------------------------------------
# 02 escalation chain (level-by-level = chained userTasks)
# ---------------------------------------------------------------------------
def s02():
    b = B("sc_escalation_chain", "02 逐级向上审批",
          ["COVERS: dept leader(opType=18) -> vice president(role) -> GM(role), chained userTasks",
           "RULE: real escalation = multiple userTasks in series, never bhxj=3 (that adds whole chain in PARALLEL).",
           "NOTE: opType=18 needs starter.manager_id; paired with opType=2 on same node to avoid empty MI collection.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_DeptLeader", "部门上级审批", 1,
                ops=[op(18), op(2, "dept_mgr_role_id")], groups="dept_mgr_role_id",
                doc="创建人上级由 leaderId(starter) 解析；同节点配角色兜底，防止 MI 集合为空被静默跳过。",
                layer=2, lane=0))
    b.add(utask("Task_VicePresident", "分管领导审批", 1,
                ops=[op(2, "vice_president_role_id")], groups="vice_president_role_id", layer=3, lane=0))
    b.add(utask("Task_GM", "总经理审批", 1,
                ops=[op(2, "gm_role_id")], groups="gm_role_id", layer=4, lane=0))
    b.add(end("EndEvent_1", layer=5, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_DeptLeader", "提交")
    b.flow("Flow_3", "Task_DeptLeader", "Task_VicePresident", "同意")
    b.flow("Flow_4", "Task_VicePresident", "Task_GM", "同意")
    b.flow("Flow_5", "Task_GM", "EndEvent_1", "通过")
    save(b, "02_escalation_chain.bpmn20.xml")


# ---------------------------------------------------------------------------
# 03 countersign (all must approve)
# ---------------------------------------------------------------------------
def s03():
    b = B("sc_countersign", "03 会签全部通过",
          ["COVERS: signOrder=1 (会签), all assignees must approve before token moves on.",
           "OPERATORS: three named users (opType=3) on one node.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Countersign", "三方会签", 1,
                ops=[op(3, "1001", sign=1), op(3, "1002", sign=1), op(3, "1003", sign=1)],
                sign=1, groups="1001,1002,1003",
                doc="会签：三个人全部同意才推进；任一退回即整单退回。", layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Countersign", "提交")
    b.flow("Flow_3", "Task_Countersign", "EndEvent_1", "全部同意")
    save(b, "03_countersign_all.bpmn20.xml")


# ---------------------------------------------------------------------------
# 04 or-sign (any one approves)
# ---------------------------------------------------------------------------
def s04():
    b = B("sc_or_sign", "04 或签任一通过",
          ["COVERS: signOrder=0 (或签), first approver's decision closes sibling tasks.",
           "OPERATORS: role with three candidates (opType=2), or-sign.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_OrSign", "或签审批", 1,
                ops=[op(2, "hr_reviewer_role_id", sign=0)], sign=0,
                groups="hr_reviewer_role_id",
                doc="或签：任意一人通过即推进，其余兄弟任务被 closeSiblings 关闭。", layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_OrSign", "提交")
    b.flow("Flow_3", "Task_OrSign", "EndEvent_1", "任一同意")
    save(b, "04_any_one_approve.bpmn20.xml")


# ---------------------------------------------------------------------------
# 05 sequential approval
# ---------------------------------------------------------------------------
def s05():
    b = B("sc_sequential", "05 依次审批",
          ["COVERS: signOrder=2 (依次), tasks activated one by one in wf_task.id ASC order.",
           "NOTE: batch_no is NOT consumed at runtime; order is by task id.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Sequential", "依次审批", 1,
                ops=[op(3, "2001", sign=2), op(3, "2002", sign=2), op(3, "2003", sign=2)],
                sign=2, groups="2001,2002,2003",
                doc="依次审批：按 wf_task.id ASC 逐个激活，前一人通过后才轮到下一人。", layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Sequential", "提交")
    b.flow("Flow_3", "Task_Sequential", "EndEvent_1", "全部依次通过")
    save(b, "05_sequential_approval.bpmn20.xml")


# ---------------------------------------------------------------------------
# 06 CC / circulate
# ---------------------------------------------------------------------------
def s06():
    b = B("sc_cc_circulate", "06 抄送知会",
          ["COVERS: signOrder=3 (抄送不需提交) / 4 (抄送需提交).",
           "NOTE: CC operators land in wf_ext_task; not blockers to the main approval path.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Approve", "主办审批", 1,
                ops=[op(3, "3001", sign=0)], sign=0, groups="3001", layer=2, lane=0))
    b.add(utask("Task_CC", "抄送知会", 1,
                ops=[op(3, "4001", sign=3), op(3, "4002", sign=3)], sign=3,
                groups="4001,4002", auto_approve=1,
                doc="抄送节点：signOrder=3 不需提交；用了 autoApprove 让它自动过，知会写 wf_ext_task。",
                layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Approve", "提交")
    b.flow("Flow_3", "Task_Approve", "Task_CC", "同意后知会")
    b.flow("Flow_4", "Task_CC", "EndEvent_1", "知会完成")
    save(b, "06_cc_circulate.bpmn20.xml")


# ---------------------------------------------------------------------------
# 07 exclusive gateway + conditional reject
# ---------------------------------------------------------------------------
def s07():
    b = B("sc_exclusive_amount", "07 排他网关金额分级与条件回退",
          ["COVERS: exclusiveGateway amount routing; default flow carries NO condition; conditions use && only.",
           "CONDITIONAL REJECT = gateway split + per-node reject whitelist (engine reject itself has no conditions).",
           "NOTE: condition variable is a main-form field name, not fielddbname.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=1))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=1))
    b.add(gateway("Gate_Amount", "金额分级", default="Flow_Low", layer=2, lane=1))
    b.add(utask("Task_HighApprove", "大额总经理审批", 1,
                ops=[op(2, "gm_role_id")], groups="gm_role_id",
                rejects=[reject("Task_Create", "数据错误退发起人"),
                         reject("Task_Create", "金额异常退发起人")], layer=3, lane=0))
    b.add(utask("Task_LowApprove", "小额主管审批", 1,
                ops=[op(2, "supervisor_role_id")], groups="supervisor_role_id",
                rejects=[reject("Task_Create", "退发起人")], layer=3, lane=2))
    b.add(end("EndEvent_1", layer=4, lane=1))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Gate_Amount", "提交")
    b.flow("Flow_High", "Gate_Amount", "Task_HighApprove", "大额",
           cond="${amount &gt; 20000}")
    b.flow("Flow_Low", "Gate_Amount", "Task_LowApprove", "小额兜底")
    b.flow("Flow_3", "Task_HighApprove", "EndEvent_1", "通过")
    b.flow("Flow_4", "Task_LowApprove", "EndEvent_1", "通过")
    save(b, "07_exclusive_gateway_amount.bpmn20.xml")


# ---------------------------------------------------------------------------
# 08 parallel gateway fork/join
# ---------------------------------------------------------------------------
def s08():
    b = B("sc_parallel_gateway", "08 并行网关分支汇聚",
          ["COVERS: parallelGateway fork -> two parallel approvals -> parallelGateway join -> archive.",
           "NOTE: join waits for all incoming tokens.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=1))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=1))
    b.add(gateway("Fork_1", "并行分叉", kind="parallel", layer=2, lane=1))
    b.add(utask("Task_HR", "人事审批", 1, ops=[op(2, "hr_role_id")],
                groups="hr_role_id", layer=3, lane=0))
    b.add(utask("Task_FIN", "财务审批", 1, ops=[op(2, "fin_role_id")],
                groups="fin_role_id", layer=3, lane=2))
    b.add(gateway("Join_1", "并行汇聚", kind="parallel", layer=4, lane=1))
    b.add(end("EndEvent_1", layer=5, lane=1))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Fork_1", "提交")
    b.flow("Flow_ToHR", "Fork_1", "Task_HR", "人事线")
    b.flow("Flow_ToFIN", "Fork_1", "Task_FIN", "财务线")
    b.flow("Flow_3", "Task_HR", "Join_1", "通过")
    b.flow("Flow_4", "Task_FIN", "Join_1", "通过")
    b.flow("Flow_5", "Join_1", "EndEvent_1", "全部汇聚")
    save(b, "08_parallel_gateway.bpmn20.xml")


# ---------------------------------------------------------------------------
# 09 inclusive gateway
# ---------------------------------------------------------------------------
def s09():
    b = B("sc_inclusive_gateway", "09 包容网关",
          ["COVERS: inclusiveGateway selects one OR more matching branches; join waits for activated ones only.",
           "NOTE: default flow carries NO condition; branch conditions use && only.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=1))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=1))
    b.add(gateway("Gate_Inclusive", "条件分流", kind="inclusive", default="Flow_Direct",
                  layer=2, lane=1))
    b.add(utask("Task_HR", "人事会签", 1, ops=[op(2, "hr_role_id")],
                groups="hr_role_id", layer=3, lane=0))
    b.add(utask("Task_FIN", "财务会签", 1, ops=[op(2, "fin_role_id")],
                groups="fin_role_id", layer=3, lane=2))
    b.add(gateway("Join_Inclusive", "条件汇聚", kind="inclusive", layer=4, lane=1))
    b.add(end("EndEvent_1", layer=5, lane=1))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Gate_Inclusive", "提交")
    b.flow("Flow_NeedHR", "Gate_Inclusive", "Task_HR", "需人事",
           cond="${need_hr == true}")
    b.flow("Flow_NeedFIN", "Gate_Inclusive", "Task_FIN", "需财务",
           cond="${need_fin == true}")
    b.flow("Flow_Direct", "Gate_Inclusive", "Join_Inclusive", "无需会签")
    b.flow("Flow_3", "Task_HR", "Join_Inclusive", "通过")
    b.flow("Flow_4", "Task_FIN", "Join_Inclusive", "通过")
    b.flow("Flow_5", "Join_Inclusive", "EndEvent_1", "汇聚完成")
    save(b, "09_inclusive_gateway.bpmn20.xml")


# ---------------------------------------------------------------------------
# 10 mixed operator types
# ---------------------------------------------------------------------------
def s10():
    b = B("sc_operator_types", "10 操作者类型混合演示",
          ["COVERS: opType 3 person / 2 role / 58 post / 1 dept / 5 field-person / 17 creator / 19 same-dept.",
           "NOTE: all opType values are NUMERIC; 30/97/98 are declared but unresolvable, never use.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请人填单", 0,
                ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_ByPerson", "指定人员审批", 1,
                ops=[op(3, "1001")], groups="1001", layer=2, lane=0))
    b.add(utask("Task_ByRole", "角色审批", 1,
                ops=[op(2, "finance_role_id")], groups="finance_role_id", layer=3, lane=0))
    b.add(utask("Task_ByPost", "岗位审批", 1,
                ops=[op(58, "post_manager_id")], groups="post_manager_id", layer=4, lane=0))
    b.add(utask("Task_ByDept", "部门审批", 1,
                ops=[op(1, "20000000001", bhxj=1)], groups="dept_1001", layer=5, lane=0))
    b.add(utask("Task_ByField", "字段指定人审批", 1,
                ops=[op(5, "approver_user")], groups="field_approver", layer=6, lane=0))
    b.add(utask("Task_SameDept", "本部门审批", 1,
                ops=[op(19)], groups="same_dept", layer=7, lane=0))
    b.add(end("EndEvent_1", layer=8, lane=0))
    prev = "StartEvent_1"
    for i, eid in enumerate(["Task_Create", "Task_ByPerson", "Task_ByRole", "Task_ByPost",
                             "Task_ByDept", "Task_ByField", "Task_SameDept", "EndEvent_1"]):
        b.flow("Flow_%s" % (i + 1), prev, eid, "提交")
        prev = eid
    save(b, "10_operator_types.bpmn20.xml")


# ---------------------------------------------------------------------------
# 11 matrix operator (opType 99)
# ---------------------------------------------------------------------------
def s11():
    b = B("sc_matrix_operator", "11 矩阵操作者审批",
          ["COVERS: opType=99 matrix operator, resolved by byMatrix -> matchUsers from matrixJson.",
           "NOTE: matrixJson is carried on wf:operator; fill real matrixId/conditions for your environment.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    mid = REAL["matrix_demo_id"]
    matrix = ("      <wf:operator opType=\"99\" objId=\"%s\" signOrder=\"0\" bhxj=\"0\" "
              "matrixJson='{\"matrixId\":\"%s\",\"valueField\":\"owner\","
              "\"conditions\":[{\"condField\":\"dept\",\"wfField\":\"-1\"}]}'/>"
              % (mid, mid))
    # pair a real role so the node still resolves if the matrix row has no match
    b.add(utask("Task_Matrix", "矩阵审批", 1, ops=[matrix, op(2, "hr_role_id")],
                groups="matrix_role",
                doc="矩阵操作者：按 matrixJson 的维度匹配，结果维度 owner 给出办理人；配角色兜底防解析为空。", layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Matrix", "提交")
    b.flow("Flow_3", "Task_Matrix", "EndEvent_1", "通过")
    save(b, "11_matrix_operator.bpmn20.xml")


# ---------------------------------------------------------------------------
# 12 creator's leader (opType 18)
# ---------------------------------------------------------------------------
def s12():
    b = B("sc_dept_leader", "12 创建人上级审批",
          ["COVERS: opType=18 (创建人上级, i.e. 部门上级) via leaderId(starter).",
           "RISK: leaderId(null) returns empty with NO warning; pair with opType=2 so MI collection is never empty.",
           "PRECONDITION: blade_user.manager_id maintained; starter present at MI collection injection.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Leader", "创建人上级审批", 1,
                ops=[op(18), op(2, "fallback_leader_role_id")], groups="fallback_leader_role_id",
                doc="紧跟创建节点的上级审批：18 依赖 starter.manager_id；同节点角色兜底防静默跳过。",
                layer=2, lane=0))
    b.add(end("EndEvent_1", layer=3, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Leader", "提交")
    b.flow("Flow_3", "Task_Leader", "EndEvent_1", "通过")
    save(b, "12_dept_leader.bpmn20.xml")


# ---------------------------------------------------------------------------
# 13 reject whitelist (to starter / to named node)
# ---------------------------------------------------------------------------
def s13():
    b = B("sc_reject_whitelist", "13 退回白名单与退发起人",
          ["COVERS: reject whitelist via wf:reject; target must be a 0/1/2 node (never gateway/archive/auto).",
           "NOTE: reject is unconditional; 'conditional reject' = gateway split + per-node whitelist.",
           "NOTE: node menus (submit/reject/forward) are in ext_json, not wf: extension; set in designer.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Approve1", "初审", 1, ops=[op(2, "reviewer_role_id")],
                groups="reviewer_role_id",
                rejects=[reject("Task_Create", "退发起人修改")], layer=2, lane=0))
    b.add(utask("Task_Approve2", "复审", 1, ops=[op(2, "senior_role_id")],
                groups="senior_role_id",
                rejects=[reject("Task_Create", "退发起人"),
                         reject("Task_Approve1", "退初审重审")], layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Approve1", "提交")
    b.flow("Flow_3", "Task_Approve1", "Task_Approve2", "同意")
    b.flow("Flow_4", "Task_Approve2", "EndEvent_1", "通过")
    save(b, "13_reject_whitelist.bpmn20.xml")


# ---------------------------------------------------------------------------
# 14 forward / transfer
# ---------------------------------------------------------------------------
def s14():
    b = B("sc_forward_transfer", "14 转发转办场景",
          ["COVERS: forward (转办) / forwardRetract (撤回) / transfer operation menus on a userTask.",
           "NOTE: only submit/reject/forward are server-enforced; menus live in ext_json.settings.operateMenu.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Handler", "办理人处理", 1, ops=[op(3, "5001")], groups="5001",
                doc="办理人可转办(forward)/撤回转办(forwardRetract)/委托(transfer)。菜单需在设计器配置。",
                layer=2, lane=0))
    b.add(utask("Task_Confirm", "结果确认", 1, ops=[op(2, "confirm_role_id")],
                groups="confirm_role_id", layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Handler", "提交")
    b.flow("Flow_3", "Task_Handler", "Task_Confirm", "办理完成")
    b.flow("Flow_4", "Task_Confirm", "EndEvent_1", "通过")
    save(b, "14_forward_transfer.bpmn20.xml")


# ---------------------------------------------------------------------------
# 15 timeout rules (all four actionWay)
# ---------------------------------------------------------------------------
def s15():
    b = B("sc_timeout_rules", "15 超时规则四种动作",
          ["COVERS: actionWay remind / autoApprove / forward / assign across four nodes.",
           "NOTE: wf:timeout nested in wf:node; durationMin minutes; scanned by WfTimeoutJob every 10 min.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("T_Remind", "提醒节点", 1, ops=[op(2, "role_a")], groups="role_a",
                tos=[tm(dur=60, way="remind", remind="sys,ml")], layer=2, lane=0))
    b.add(utask("T_AutoApprove", "超时自动通过", 1, ops=[op(2, "role_b")], groups="role_b",
                tos=[tm(dur=120, way="autoApprove", remind="", seq=1)], layer=3, lane=0))
    b.add(utask("T_Forward", "超时转办", 1, ops=[op(2, "role_c")], groups="role_c",
                tos=[tm(dur=180, way="forward", remind="sys", seq=1)], layer=4, lane=0))
    b.add(utask("T_Assign", "超时改派", 1, ops=[op(2, "role_d")], groups="role_d",
                tos=[tm(dur=240, way="assign", remind="sys", seq=1)], layer=5, lane=0))
    b.add(end("EndEvent_1", layer=6, lane=0))
    prev = "StartEvent_1"
    for i, eid in enumerate(["Task_Create", "T_Remind", "T_AutoApprove", "T_Forward",
                             "T_Assign", "EndEvent_1"]):
        b.flow("Flow_%s" % (i + 1), prev, eid, "提交")
        prev = eid
    save(b, "15_timeout_rules.bpmn20.xml")


# ---------------------------------------------------------------------------
# 16 field permissions
# ---------------------------------------------------------------------------
def s16():
    b = B("sc_field_permissions", "16 字段权限控制",
          ["COVERS: perm 0 hidden / 1 readonly / 2 editable / 3 required, main + detail (dt1) scope.",
           "NOTE: permission is dual-source: BPMN fieldPerm + formmode layout; both must align.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT,
                fps=[fp("title", 3), fp("amount", 3), fp("id_no", 2)],
                layer=1, lane=0))
    b.add(utask("Task_Finance", "财务审核", 1, ops=[op(2, "fin_role_id")], groups="fin_role_id",
                fps=[fp("title", 1), fp("amount", 1), fp("id_no", 0),
                     fp("budget", 1, scope="dt1")], layer=2, lane=0))
    b.add(utask("Task_Archive", "归档确认", 1, ops=[op(17)], assignee=A_INIT,
                fps=[fp("title", 1), fp("amount", 1)], layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Finance", "提交")
    b.flow("Flow_3", "Task_Finance", "Task_Archive", "通过")
    b.flow("Flow_4", "Task_Archive", "EndEvent_1", "归档")
    save(b, "16_field_permissions.bpmn20.xml")


# ---------------------------------------------------------------------------
# 17 ServiceTask auto node (E1)
# ---------------------------------------------------------------------------
def s17():
    b = B("sc_service_task_e1", "17 服务任务自动节点 E1",
          ["COVERS: ServiceTask auto-executed by wfServiceTaskDelegate -> WfActionExecutor.execute().",
           "REQUIRED SWITCHES: action-gate=true, service-task-exec=true + gray defId; E7 engine-auto-todo-bridge=true.",
           "PRECONDITION: flip switches then RE-PUBLISH so deploy injects delegateExpression; actionKey must be registered.",
           "BOUNDARY: delegate does NOT advance; E7 bridge creates next todo after ACTIVITY_COMPLETED.",
           "DEPLOY: blocked by S0 guard until service-task-exec=true."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    custom = ('      <wf:customAction actionKey="demo_log_field"/>')
    b.add(auto_task("Task_Auto", "自动接口动作", tag="serviceTask",
                    impl="${wfServiceTaskDelegate}", extra_nodes=custom, layer=2, lane=0))
    b.add(utask("Task_Approve", "自动动作后审批", 1, ops=[op(2, "approve_role_id")],
                groups="approve_role_id", layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Auto", "提交")
    b.flow("Flow_3", "Task_Auto", "Task_Approve", "自动完成")
    b.flow("Flow_4", "Task_Approve", "EndEvent_1", "通过")
    save(b, "17_service_task_e1.bpmn20.xml")


# ---------------------------------------------------------------------------
# 18 ScriptTask (S6)
# ---------------------------------------------------------------------------
def s18():
    b = B("sc_script_task_s6", "18 脚本任务自动节点 S6",
          ["COVERS: ScriptTask executed by sandboxed Groovy engine (SecureASTCustomizer whitelist).",
           "REQUIRED SWITCH: s6-script-task-enabled=true then RE-PUBLISH (S0 guard lifts).",
           "BOUNDARY: script runs in engine layer, NOT IWfCustomAction registry; resultVariable writes process var.",
           "DEPLOY: blocked by S0 guard until s6-script-task-enabled=true."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(auto_task("Task_Script", "脚本计算", tag="scriptTask", script_format="groovy",
                    script="6 * 7", result_variable="calculated", layer=2, lane=0))
    b.add(utask("Task_Approve", "脚本结果审批", 1, ops=[op(2, "approve_role_id")],
                groups="approve_role_id", layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Script", "提交")
    b.flow("Flow_3", "Task_Script", "Task_Approve", "脚本完成")
    b.flow("Flow_4", "Task_Approve", "EndEvent_1", "通过")
    save(b, "18_script_task_s6.bpmn20.xml")


# ---------------------------------------------------------------------------
# 19 ReceiveTask wake-up (S4)
# ---------------------------------------------------------------------------
def s19():
    b = B("sc_receive_task_s4", "19 接收任务等待唤醒 S4",
          ["COVERS: ReceiveTask wait state resumed by runtimeService.trigger via WfWakeupService.",
           "REQUIRED SWITCH: s4-wakeup-enabled=true then RE-PUBLISH (S0 guard lifts).",
           "ENTRY: internal admin controller HAS_ROLE_WORKFLOW; external permitAll needs HMAC + gateway whitelist.",
           "RISK: with switch off this node waits forever; archive misjudgement risk if ACT_RU_TASK empty.",
           "DEPLOY: blocked by S0 guard until s4-wakeup-enabled=true."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(auto_task("Task_Receive", "等待外部回调", tag="receiveTask", node_type="5",
                    layer=2, lane=0))
    b.add(utask("Task_AfterWake", "唤醒后审批", 1, ops=[op(2, "approve_role_id")],
                groups="approve_role_id", layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Receive", "提交")
    b.flow("Flow_3", "Task_Receive", "Task_AfterWake", "回调唤醒")
    b.flow("Flow_4", "Task_AfterWake", "EndEvent_1", "通过")
    save(b, "19_receive_task_s4.bpmn20.xml")


# ---------------------------------------------------------------------------
# 20 Timer boundary event (S3)
# ---------------------------------------------------------------------------
def s20():
    b = B("sc_timer_boundary_s3", "20 边界定时器超时分支 S3",
          ["COVERS: boundary timer event on an approval task; on timeout token goes to timeout handler branch.",
           "REQUIRED SWITCH: s3-timer-enabled=true (business-side scanner, moveTimerToExecutableJob then executeJob).",
           "MUTEX: keep blade.flowable.async-executor-enabled=false; startup self-check logs error if both true.",
           "TEST NOTE: neutralizeForTest downgrades timer to manualTask so the test walkthrough still passes.",
           "DEPLOY: default config (timer only warns at deploy)."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Approve", "审批(挂边界定时器)", 1,
                ops=[op(2, "approve_role_id")], groups="approve_role_id", layer=2, lane=0))
    boundary = ('    <boundaryEvent id="Boundary_Timer" name="超时5分钟" '
                'attachedToRef="Task_Approve" cancelActivity="true">\n'
                '      <timerEventDefinition><timeDuration>PT5M</timeDuration></timerEventDefinition>\n'
                '    </boundaryEvent>')
    b.nodes.append(dict(id="Boundary_Timer", xml=boundary, x=365, y=190, w=30, h=30, kind="event"))
    b.add(utask("Task_Timeout", "超时处理", 1, ops=[op(2, "timeout_role_id")],
                groups="timeout_role_id", layer=3, lane=2))
    b.add(end("EndEvent_1", "正常归档", layer=4, lane=0))
    b.add(end("EndEvent_Timeout", "超时归档", layer=4, lane=2))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Approve", "提交")
    b.flow("Flow_3", "Task_Approve", "EndEvent_1", "按时通过")
    b.flow("F_Timer", "Boundary_Timer", "Task_Timeout", "超时触发")
    b.flow("Flow_4", "Task_Timeout", "EndEvent_Timeout", "通过")
    save(b, "20_timer_boundary_s3.bpmn20.xml")


# ---------------------------------------------------------------------------
# 21 business-layer subflow (avoid callActivity hard block)
# ---------------------------------------------------------------------------
def s21():
    b = B("sc_business_subflow", "21 业务层子流程触发",
          ["COVERS: business-layer subflow via node setting subflow.trigger=afterArchive (NOT BPMN callActivity).",
           "NOTE: BPMN callActivity and SubProcess are hard-blocked by S0 deploy guard; business layer is authoritative.",
           "NOTE: subflow trigger is in ext_json.settings, not wf: extension; configure in designer.",
           "DEPLOY: default config."])
    b.add(start(layer=0, lane=0))
    b.add(utask("Task_Create", "申请填单", 0, ops=[op(17)], assignee=A_INIT, layer=1, lane=0))
    b.add(utask("Task_Main", "主流程审批", 1, ops=[op(2, "main_role_id")], groups="main_role_id",
                doc="归档后经 settings.subflow.trigger=afterArchive 触发子流程；子流程全归档回调后主流程继续。",
                layer=2, lane=0))
    b.add(utask("Task_AfterSub", "子流程完成后确认", 1,
                ops=[op(2, "confirm_role_id")], groups="confirm_role_id", layer=3, lane=0))
    b.add(end("EndEvent_1", layer=4, lane=0))
    b.flow("Flow_1", "StartEvent_1", "Task_Create", "提交")
    b.flow("Flow_2", "Task_Create", "Task_Main", "提交")
    b.flow("Flow_3", "Task_Main", "Task_AfterSub", "子流程已归档")
    b.flow("Flow_4", "Task_AfterSub", "EndEvent_1", "通过")
    save(b, "21_business_subflow.bpmn20.xml")


if __name__ == "__main__":
    for fn in (s01, s02, s03, s04, s05, s06, s07, s08, s09, s10, s11,
               s12, s13, s14, s15, s16, s17, s18, s19, s20, s21):
        fn()
    print("DONE")
