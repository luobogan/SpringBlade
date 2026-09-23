package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.Gateway;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.DefinitionSaveDTO;
import org.springblade.workflow.entity.WfNodeDetailPerm;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfWorkflowType;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfNodeDetailPermMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfWorkflowTypeMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormBindingVO;
import org.springblade.workflow.vo.FormConditionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.SimulateResultVO;
import org.springblade.workflow.utils.WfConditionUtil;
import org.springblade.workflow.vo.VersionDiffVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfDefinitionServiceImpl implements IWfDefinitionService {

    /** 表单绑定提示中最多展示的流程名称数量 */
    private static final int MAX_BINDING_NAME = 10;

    /** 流程定义状态：0草稿 1已发布 3测试（三态；2停用已废除，仅存量数据可能残留） */
    private static final int DEF_STATUS_PUBLISHED = 1;

    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeLinkMapper linkMapper;
    private final WfNodeOperatorMapper operatorMapper;
    private final WfNodeFieldPermMapper fieldPermMapper;
    private final WfNodeDetailPermMapper detailPermMapper;
    private final WfWorkflowTypeMapper wfWorkflowTypeMapper;
    private final IWfInstanceService instanceService;
    private final IProcessService processService;
    /** 跨服务清理节点布局（form_layout）用 */
    private final IFormmodeClient formmodeClient;
    /** 流程实例（删除前保护校验：参考 Weaver「有实例禁止删」） */
    private final WfInstanceMapper instanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DefinitionSaveDTO dto) {
        if (dto == null || dto.getDefinition() == null) {
            throw new ServiceException("流程定义不能为空");
        }
        WfProcessDefinition def = dto.getDefinition();
        // 三态归一（草稿/测试/已发布）：「停用(2)」已废除，旧缓存表单或存量数据仍可能提交 2，
        // 服务端统一按草稿落库，杜绝已废除状态被写回（服务端不依赖前端选项收口）
        if (def.getStatus() != null && def.getStatus() == 2) {
            def.setStatus(0);
        }
        if (def.getId() == null) {
            def.setVersion(def.getVersion() == null ? 1 : def.getVersion());
            defMapper.insert(def);
        } else {
            defMapper.updateById(def);
        }

        Long defId = def.getId();
        // 节点 / 出口 / 操作者「整体覆盖」：**仅当 DTO 显式携带对应集合时才重建**。
        // ⚠️ 历史 bug（数据丢失）：此前无条件「先删 nodes/links/operators，再按 DTO 重建」，
        // 而「基础设置」保存只提交 { definition: {...} }（不带 nodes/links）→ 一次普通保存就把
        // 画布上已设计好的节点/出口/操作者全部（逻辑）删除且不重建，且删除为逻辑删除（is_deleted=1）可恢复。
        if (dto.getNodes() != null) {
            // 先按「节点ID」删除操作者，再删节点。老代码误用 eq(nodeId, defId)（nodeId≠defId）导致操作者残留。
            List<WfProcessNode> oldNodes = nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
                .select(WfProcessNode::getId).eq(WfProcessNode::getDefId, defId));
            if (!oldNodes.isEmpty()) {
                List<Long> oldNodeIds = oldNodes.stream().map(WfProcessNode::getId).collect(Collectors.toList());
                operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery().in(WfNodeOperator::getNodeId, oldNodeIds));
            }
            nodeMapper.delete(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
            for (WfProcessNode node : dto.getNodes()) {
                node.setId(null);
                node.setDefId(defId);
                nodeMapper.insert(node);
            }
        }
        if (dto.getLinks() != null) {
            linkMapper.delete(Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, defId));
            for (WfNodeLink link : dto.getLinks()) {
                link.setId(null);
                link.setDefId(defId);
                linkMapper.insert(link);
            }
        }
        if (dto.getOperators() != null) {
            // 操作者按 node_id 归属：清理当前（若上面刚重建则为新）节点的操作者后重建
            List<WfProcessNode> curNodes = nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
                .select(WfProcessNode::getId).eq(WfProcessNode::getDefId, defId));
            if (!curNodes.isEmpty()) {
                List<Long> curNodeIds = curNodes.stream().map(WfProcessNode::getId).collect(Collectors.toList());
                operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery().in(WfNodeOperator::getNodeId, curNodeIds));
            }
            for (WfNodeOperator op : dto.getOperators()) {
                op.setId(null);
                operatorMapper.insert(op);
            }
        }
        return defId;
    }

    @Override
    public WfProcessDefinition detail(Long id) {
        return defMapper.selectById(id);
    }

    @Override
    public List<WfProcessDefinition> listByForm(Long formId) {
        // 记录级收口：非流程管理员只返回「已发布」的定义 —— 发起页（全体员工）
        // 用的是同一个接口，草稿与停用版本属建模中间态，不该出现在员工的
        //「新建流程」列表里（前端虽已按 status 过滤，但服务端不能依赖前端）。
        boolean admin = WfAuthUtil.isAdmin();
        return defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(formId != null, WfProcessDefinition::getFormId, formId)
            .eq(!admin, WfProcessDefinition::getStatus, DEF_STATUS_PUBLISHED)
            .orderByDesc(WfProcessDefinition::getVersion));
    }

    @Override
    public FormBindingVO formBinding(Long formId) {
        FormBindingVO vo = new FormBindingVO();
        if (formId == null) {
            return vo;
        }

        List<WfProcessDefinition> defs = defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getFormId, formId)
            .eq(WfProcessDefinition::getIsDeleted, 0)
            .orderByAsc(WfProcessDefinition::getVersion));
        int instanceCount = instanceService.countByForm(formId);

        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (WfProcessDefinition def : defs) {
            ids.add(String.valueOf(def.getId()));
            // 只展示前 10 个，避免提示信息过长
            if (names.size() < MAX_BINDING_NAME) {
                String name = def.getName() == null ? def.getProcKey() : def.getName();
                names.add(def.getVersion() == null ? name : name + "（v" + def.getVersion() + "）");
            }
        }

        vo.setDefinitionIds(ids);
        vo.setDefinitionNames(names);
        vo.setDefinitionCount(defs.size());
        vo.setInstanceCount(instanceCount);
        vo.setBound(!defs.isEmpty() || instanceCount > 0);
        return vo;
    }

    @Override
    public List<WfProcessNode> nodes(Long defId) {
        return nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .orderByAsc(WfProcessNode::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean configOperator(Long defId, String nodeKey, List<WfNodeOperator> operators) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        if (node == null) {
            throw new ServiceException("节点不存在: " + nodeKey);
        }
        operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
            .eq(WfNodeOperator::getNodeId, node.getId()));
        if (operators != null) {
            for (WfNodeOperator op : operators) {
                op.setId(null);
                op.setNodeId(node.getId());
                operatorMapper.insert(op);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncOperatorToNodes(Long defId, String fromNodeKey, List<String> targetNodeKeys) {
        WfProcessNode from = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, fromNodeKey)
            .last("LIMIT 1"));
        if (from == null) {
            throw new ServiceException("源节点不存在: " + fromNodeKey);
        }
        List<WfNodeOperator> src = operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
            .eq(WfNodeOperator::getNodeId, from.getId()));
        if (targetNodeKeys == null || targetNodeKeys.isEmpty()) {
            return true;
        }
        for (String tk : targetNodeKeys) {
            if (tk == null || tk.isBlank() || tk.equals(fromNodeKey)) {
                continue;
            }
            WfProcessNode target = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, tk)
                .last("LIMIT 1"));
            if (target == null) {
                continue;
            }
            operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
                .eq(WfNodeOperator::getNodeId, target.getId()));
            for (WfNodeOperator op : src) {
                WfNodeOperator copy = new WfNodeOperator();
                org.springframework.beans.BeanUtils.copyProperties(op, copy, "id");
                copy.setId(null);
                copy.setNodeId(target.getId());
                operatorMapper.insert(copy);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deploy(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        // BPMN 由流程设计器（bpmn-js）产出后，真正部署到 Flowable 引擎
        if (def.getBpmnXml() != null && !def.getBpmnXml().isBlank()) {
            if (def.getProcKey() == null || def.getProcKey().isBlank()) {
                throw new ServiceException("流程定义缺少 procKey（应由画布 BPMN process id 提供）");
            }
            // 发布门禁：正式部署前的硬校验，杜绝「测试绿灯、正式跑不了」（测试态靠关校验 + 消毒才跑通）
            validateForDeploy(def);
            // 部署前把「出口条件」注入到对应 sequenceFlow，保证 Flowable 运行时按条件流转
            String deployXml = injectLinkConditions(def.getBpmnXml(), links(defId));
            // 落库 deployment_id：此前 deployProcess 的返回值被丢弃，导致无法精确比对
            // 「引擎 latest == 正式部署」（只能靠部署时间与消毒标记间接判读，见巡检 ⑥）。
            // 落库后：① 巡检可直接 JOIN 比对；② 发起自检可给出 engineDeploymentMatched 标志。
            def.setDeploymentId(processService.deployProcess(def.getProcKey(), deployXml));
            // 回写「激活版本的流程定义ID」（方案 §3 / 迁移 _015）：
            // 发起时据此走 startProcessInstanceById，精确绑定这一版，与「哪个部署最新」解耦。
            def.setProcDefId(processService.latestProcDefId(def.getProcKey()));
        } else {
            throw new ServiceException("尚无 BPMN 定义，请先在「流程画布」中设计并保存");
        }
        def.setStatus(1);
        defMapper.updateById(def);
        // 发布即激活（版本控制）：组内锚点统一切到本版本；同组其它已发布版本转停用。
        // 在途实例由 Flowable 绑定创建时的 ACT_RE_PROCDEF.ID_ 原生隔离，不受新版本影响。
        promoteActiveVersion(def);
        log.info("[blade-workflow] 流程定义已发布并部署到引擎（并激活为当前版本）. "
                + "defId={}, procKey={}, version={}, deploymentId={}, procDefId={}",
            defId, def.getProcKey(), def.getVersion(), def.getDeploymentId(), def.getProcDefId());
        return true;
    }

    @Override
    public String deployForTest(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        if (def.getBpmnXml() == null || def.getBpmnXml().isBlank()) {
            throw new ServiceException("尚无 BPMN 定义，请先在「流程画布」中设计并保存");
        }
        if (def.getProcKey() == null || def.getProcKey().isBlank()) {
            throw new ServiceException("流程定义缺少 procKey（应由画布 BPMN process id 提供）");
        }
        // 测试部署：注入出口条件 + 测试态消毒（businessRuleTask 降级等），不改 status、不激活版本。
        // ⚠️ 用独立 key（procKey + "__test"）：Flowable 的 startProcessInstanceByKey 取「该 key 最新部署」，
        //    若测试沿用正式 procKey，每次测试都会把正式版本顶掉（未清理时正式发起跑的是「关校验+消毒」的测试 BPMN）。
        //    发起侧必须成对使用同一 key：WfTestServiceImpl 把 StartProcessDTO.engineKey 设为下面这个值。
        String testKey = def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX;
        // ⚠️ 必须把 BPMN 的 <process id> 一并改成 testKey（见 neutralizeForTest(_, testKey)），
        //    否则引擎里不存在 __test 这个流程 key，测试发起会报 no process definition。
        String deployXml = neutralizeForTest(injectLinkConditions(def.getBpmnXml(), links(defId)), testKey);
        String deploymentId = processService.deployProcessForTest(testKey, deployXml);
        log.info("[blade-workflow] 流程定义已测试部署到引擎（未改发布状态，独立 key 不顶正式版本）. "
            + "defId={}, testKey={}, deploymentId={}", defId, testKey, deploymentId);
        return deploymentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveBpmn(Long defId, String bpmnXml) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        if (bpmnXml == null || bpmnXml.isBlank()) {
            throw new ServiceException("BPMN 内容不能为空");
        }

        // 解析 BPMN，提取 userTask 作为流程节点
        // 前端传来的 bpmnXml 为 base64（规避 SpringBlade XSS 过滤器对请求体“去标签”，会把 BPMN 的
        // <bpmn:*> 标签整段删掉、只剩文本而解析失败）。此处解码；若已是含 < 的 XML（XSS 已跳过/旧调用）则直接用。
        String xmlSource;
        if (bpmnXml != null && bpmnXml.indexOf('<') >= 0) {
            xmlSource = bpmnXml;
        } else {
            xmlSource = new String(java.util.Base64.getDecoder().decode(bpmnXml), StandardCharsets.UTF_8);
        }
        int docStart = xmlSource.indexOf("<?xml");
        if (docStart < 0) {
            docStart = xmlSource.indexOf("<bpmn:definitions");
        }
        if (docStart > 0) {
            xmlSource = xmlSource.substring(docStart);
        }
        final String xmlInput = xmlSource;

        Process process;
        BpmnModel model;
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            model = converter.convertToBpmnModel(
                () -> new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8)), false, false);
        } catch (Exception e) {
            // 兜底：bpmn-js 导出的 bpmndi 图形交换信息偶尔会让 flowable 的 StAX 解析抛
            // "Error reading XML"；保存画布只关心流程结构（flow elements），故剔除
            // <bpmndi:BPMNDiagram> 整段后重试。
            try {
                String stripped = xmlInput.replaceAll("(?s)<bpmndi:BPMNDiagram.*?</bpmndi:BPMNDiagram>", "");
                BpmnXMLConverter converter = new BpmnXMLConverter();
                model = converter.convertToBpmnModel(
                    () -> new ByteArrayInputStream(stripped.getBytes(StandardCharsets.UTF_8)), false, false);
                log.warn("[blade-workflow] BPMN 首次解析失败，剔除 bpmndi 后重试成功. defId={}", defId, e);
            } catch (Exception e2) {
                Throwable cause = e2.getCause() != null ? e2.getCause() : e2;
                String causeMsg = cause.getMessage();
                // 落盘完整 XML 便于精确定位
                try {
                    java.nio.file.Files.writeString(
                        java.nio.file.Path.of("d:/temp/bpmn_" + defId + ".xml"), xmlInput, StandardCharsets.UTF_8);
                } catch (Exception ignore) {
                    // ignore
                }
                byte[] head = xmlInput.substring(0, Math.min(xmlInput.length(), 64)).getBytes(StandardCharsets.UTF_8);
                StringBuilder hex = new StringBuilder();
                for (byte b : head) {
                    hex.append(String.format("%02x ", b & 0xff));
                }
                log.error("[blade-workflow] BPMN 解析失败. defId={}, cause={}, headHex={}", defId, causeMsg, hex, e2);
                throw new ServiceException("BPMN 解析失败：" + causeMsg);
            }
        }
        process = model.getMainProcess();
        String procId = process.getId();
        if (procId != null && !procId.isBlank()) {
            // 以画布 process id 为准，保证引擎部署一致
            def.setProcKey(procId);
        }
        def.setBpmnXml(xmlInput);
        defMapper.updateById(def);

        // 解析画布元素 → 按 nodeKey upsert 节点。
        // 关键：不能整体 delete+insert，否则节点信息（类型/审批方式/扩展/操作者）会被清空。
        Map<String, WfProcessNode> existNodes = new LinkedHashMap<>();
        for (WfProcessNode n : nodes(defId)) {
            existNodes.putIfAbsent(n.getNodeKey(), n);
        }
        Set<String> seenNodeKeys = new HashSet<>();
        // 列表顺序（sortOrder）由用户在「节点信息」列表拖拽维护：已有节点保留其 sortOrder，
        // 仅给新增节点续接（现有最大值 + 1 …），避免保存画布时把手动排序打回流程顺序。
        int newSort = 1;
        for (WfProcessNode n : existNodes.values()) {
            if (n.getSortOrder() != null && n.getSortOrder() >= newSort) {
                newSort = n.getSortOrder() + 1;
            }
        }
        for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
            Integer nodeType = nodeTypeOf(fe);
            if (nodeType == null) {
                continue;
            }
            String key = fe.getId();
            String nodeName = defaultNodeName(fe, nodeType);
            WfProcessNode exist = existNodes.get(key);
            if (exist != null) {
                // 保留既有语义属性与操作者，仅同步画布名称（sortOrder 由列表拖拽维护，不覆盖）
                exist.setNodeName(nodeName);
                nodeMapper.updateById(exist);
            } else {
                // ⚠️ 唯一索引 uk_def_node(def_id,node_key) 不含 is_deleted：节点被逻辑删除后
                //    行仍在、唯一键仍被占用，而 @TableLogic 让 nodes(defId) 看不到它，
                //    于是画布把该节点重新加回来时会走到这里 insert → DuplicateKeyException。
                //    故先查「含已删除行」：命中则复活复用原行，未命中才新建。
                WfProcessNode ghost = nodeMapper.selectAnyByDefAndKey(defId, key);
                if (ghost != null) {
                    nodeMapper.reviveNode(defId, key, nodeName, nodeType, 0, newSort++);
                    log.info("[blade-workflow] saveBpmn 复活逻辑删除节点: defId={}, nodeKey={}, oldId={}", defId, key, ghost.getId());
                } else {
                    WfProcessNode node = new WfProcessNode();
                    node.setDefId(defId);
                    node.setNodeKey(key);
                    node.setNodeName(nodeName);
                    node.setNodeType(nodeType);
                    node.setSignOrder(0); // 或签
                    node.setSortOrder(newSort++);
                    nodeMapper.insert(node);
                }
            }
            seenNodeKeys.add(key);
        }
        // 删除画布上已不存在的节点及其操作者 / 字段权限 / 明细权限。
        // ⚠️ 字段权限按 (def_id, node_key) 存储，必须一并清理：
        //    否则节点删除后权限行残留成孤儿，日后新建节点若复用了同一 nodeKey，旧权限矩阵会「复活」。
        for (WfProcessNode n : existNodes.values()) {
            if (!seenNodeKeys.contains(n.getNodeKey())) {
                operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery().eq(WfNodeOperator::getNodeId, n.getId()));
                fieldPermMapper.delete(Wrappers.<WfNodeFieldPerm>lambdaQuery()
                    .eq(WfNodeFieldPerm::getDefId, defId)
                    .eq(WfNodeFieldPerm::getNodeKey, n.getNodeKey()));
                detailPermMapper.delete(Wrappers.<WfNodeDetailPerm>lambdaQuery()
                    .eq(WfNodeDetailPerm::getDefId, defId)
                    .eq(WfNodeDetailPerm::getNodeKey, n.getNodeKey()));
                nodeMapper.deleteById(n.getId());
                // 跨服务清理该节点的布局（form_layout），避免画布删节点后留下孤儿布局
                try {
                    if (def.getFormId() != null && formmodeClient != null) {
                        formmodeClient.deleteFormLayoutByNode(def.getFormId(), n.getNodeKey());
                    }
                } catch (Exception e) {
                    log.warn("[blade-workflow] 清理节点布局失败（节点已移除）. defId={}, nodeKey={}", defId, n.getNodeKey(), e);
                }
            }
        }

        // 解析 SequenceFlow → 按 from→to upsert 出口（保留已配置的条件/必经/退回等）。
        // 关键：把「经过网关」的连线折叠成「节点 → 节点」出口（A→网关→B 记为 A→B，
        // via_gateway=1），否则网关分支在「出口信息」里完全不可见、条件也无法配置，
        // 且部署时条件注入（injectLinkConditions 按 A→B 匹配）也会漏掉网关分支。
        Map<String, WfNodeLink> existLinks = new LinkedHashMap<>();
        for (WfNodeLink l : links(defId)) {
            existLinks.putIfAbsent(linkKey(l.getFromNodeKey(), l.getToNodeKey()), l);
        }
        // 全量出边（含网关端点），按物理连线建出口；cond 取 BPMN sequenceFlow 条件（设计期多为空，以库里为准）
        Map<String, List<SeqEdge>> outEdges = new LinkedHashMap<>();
        for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
            if (!(fe instanceof SequenceFlow sf)) {
                continue;
            }
            String from = sf.getSourceRef();
            String to = sf.getTargetRef();
            if (from == null || to == null) {
                continue;
            }
            outEdges.computeIfAbsent(from, k -> new ArrayList<>())
                .add(new SeqEdge(to, sf.getConditionExpression()));
        }
        // 边界事件不是独立的流转节点：它附加在宿主上，其出线应视为「宿主的逃逸出口」（超时/异常等），
        // 必须归入宿主，否则——
        // 1) bpmn-js「追加边界事件」会把宿主原出线(A→B)的 sourceRef 改挂到边界事件(BE→B)，
        //    删除边界事件时这条线随之一并被删，宿主原出口(含出口条件)丢失；
        // 2) 会产生 BE→X 的孤立出口，出口信息里出现无意义的边界事件连线。
        // 故：把边界事件出线并入宿主、并把边界事件本身从出边起点中剔除。
        Map<String, String> beHost = new LinkedHashMap<>();
        for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
            if (fe instanceof BoundaryEvent be && be.getAttachedToRef() != null
                && seenNodeKeys.contains(be.getAttachedToRef().getId())) {
                beHost.put(be.getId(), be.getAttachedToRef().getId());
            }
        }
        for (Map.Entry<String, String> en : beHost.entrySet()) {
            List<SeqEdge> bes = outEdges.remove(en.getKey());
            if (bes != null && !bes.isEmpty()) {
                outEdges.computeIfAbsent(en.getValue(), k -> new ArrayList<>()).addAll(bes);
            }
        }
        // 旧折叠出口的条件迁移：历史数据把「A→B（经网关 G）」记成一条逻辑出口，其条件实际
        // 应挂在 G→B 这条物理线上。改成按物理连线存出口后，若直接丢弃会丢失用户已配置的条件，
        // 故先按 (G→B) 收集旧条件，供新建 / 补空物理出口时回填。
        Map<String, String> legacyBranchCond = new LinkedHashMap<>();
        for (WfNodeLink l : existLinks.values()) {
            String gw = l.getViaGatewayKey();
            if (gw != null && !gw.isBlank() && l.getToNodeKey() != null
                && l.getConditionExpr() != null && !l.getConditionExpr().isBlank()) {
                legacyBranchCond.putIfAbsent(linkKey(gw, l.getToNodeKey()), l.getConditionExpr());
            }
        }
        Set<String> seenLinkKeys = new HashSet<>();
        int linkSort = 1;
        // 每条物理连线各存一条出口（不再折叠）：网关已入 wf_process_node（节点类型 7），
        // 故 from/to 两端都是真实节点，「节点→网关」「网关→节点」各自成为独立出口，
        // 画布上点任意一条线都能查看 / 编辑它自己的出口信息。
        // 边界事件的出线已在 outEdges 中并入宿主，不会出现 BE→X 孤立出口。
        for (Map.Entry<String, List<SeqEdge>> en : outEdges.entrySet()) {
            String from = en.getKey();
            if (!seenNodeKeys.contains(from)) {
                continue;
            }
            for (SeqEdge e : en.getValue()) {
                String to = e.to;
                if (!seenNodeKeys.contains(to)) {
                    continue;
                }
                String k = linkKey(from, to);
                String cond = e.cond;
                WfNodeLink exist = existLinks.get(k);
                if (exist != null) {
                    exist.setSortOrder(linkSort++);
                    exist.setViaGateway(0);
                    exist.setViaGatewayKey(null);
                    // 已存在但没有条件的：优先用 BPMN 上的条件，否则回填旧折叠出口迁移来的
                    // 条件（仅补空，绝不覆盖用户现已配置的条件）
                    if (exist.getConditionExpr() == null || exist.getConditionExpr().isBlank()) {
                        String c = (cond != null && !cond.isBlank()) ? cond : legacyBranchCond.get(k);
                        if (c != null && !c.isBlank()) {
                            exist.setConditionExpr(c);
                        }
                    }
                    linkMapper.updateById(exist);
                } else {
                    WfNodeLink link = new WfNodeLink();
                    link.setDefId(defId);
                    link.setFromNodeKey(from);
                    link.setToNodeKey(to);
                    link.setIsReject(0);
                    link.setIsMustPass(0);
                    link.setViaGateway(0);
                    link.setViaGatewayKey(null);
                    String c = (cond != null && !cond.isBlank()) ? cond : legacyBranchCond.get(k);
                    if (c != null && !c.isBlank()) {
                        link.setConditionExpr(c);
                    }
                    link.setSortOrder(linkSort++);
                    linkMapper.insert(link);
                }
                seenLinkKeys.add(k);
            }
        }
        // 删除画布上已不存在的出口
        for (WfNodeLink l : existLinks.values()) {
            if (!seenLinkKeys.contains(linkKey(l.getFromNodeKey(), l.getToNodeKey()))) {
                linkMapper.deleteById(l.getId());
            }
        }
        log.info("[blade-workflow] BPMN 已保存并解析. defId={}, procKey={}, nodeCount={}, linkCount={}",
            defId, def.getProcKey(), seenNodeKeys.size(), seenLinkKeys.size());
        return defId;
    }

    @Override
    public String getBpmn(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        return def == null ? null : def.getBpmnXml();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAsNewVersion(Long defId) {
        WfProcessDefinition old = defMapper.selectById(defId);
        if (old == null) {
            throw new ServiceException("流程定义不存在");
        }
        // 版本号 = 同 procKey 组内最大值 + 1（对齐 ecology 防重；唯一键 uk_proc_key_version 兜底并发）
        int newVersion = defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, old.getProcKey()))
            .stream()
            .mapToInt(d -> d.getVersion() == null ? 0 : d.getVersion())
            .max().orElse(0) + 1;

        WfProcessDefinition neo = new WfProcessDefinition();
        neo.setProcKey(old.getProcKey());
        neo.setFormId(old.getFormId());
        neo.setName(old.getName());
        neo.setVersion(newVersion);
        // 版本组锚点：新版本继承所在组的锚点（NULL = 尚未成组，锚点即旧版本自身）
        neo.setActiveVersionId(anchorOf(old));
        neo.setIsFree(old.getIsFree());
        neo.setFreeWfType(old.getFreeWfType());
        neo.setType(old.getType());
        neo.setFormType(old.getFormType());
        neo.setDescription(old.getDescription());
        neo.setSortOrder(old.getSortOrder());
        // 完整复制 BPMN（画布结构随版本留存；修复此前新版本画布为空的缺口）
        neo.setBpmnXml(old.getBpmnXml());
        // 新版本是「未部署」的草稿：deployment_id 不继承（它是「本行最后一次正式部署」的产物）
        neo.setDeploymentId(null);
        neo.setStatus(0);
        defMapper.insert(neo);

        // 复制节点（节点ID重新生成，与 ecology 版本模型一致），并建立 旧节点id-新节点id 映射
        List<WfProcessNode> nodes = nodes(defId);
        Map<Long, Long> nodeIdMap = new LinkedHashMap<>();
        for (WfProcessNode n : nodes) {
            Long oldNodeId = n.getId();
            n.setId(null);
            n.setDefId(neo.getId());
            nodeMapper.insert(n);
            if (oldNodeId != null) {
                nodeIdMap.put(oldNodeId, n.getId());
            }
        }
        // 复制出口
        List<WfNodeLink> links = links(defId);
        for (WfNodeLink l : links) {
            l.setId(null);
            l.setDefId(neo.getId());
            linkMapper.insert(l);
        }
        // 复制节点字段权限（defId + nodeKey 关联，nodeKey 不变，仅换 defId）
        List<WfNodeFieldPerm> perms = fieldPermMapper.selectList(
            Wrappers.<WfNodeFieldPerm>lambdaQuery().eq(WfNodeFieldPerm::getDefId, defId));
        for (WfNodeFieldPerm p : perms) {
            p.setId(null);
            p.setDefId(neo.getId());
            fieldPermMapper.insert(p);
        }
        // 复制节点级明细表权限（defId + nodeKey 关联）
        List<WfNodeDetailPerm> detailPerms = detailPermMapper.selectList(
            Wrappers.<WfNodeDetailPerm>lambdaQuery().eq(WfNodeDetailPerm::getDefId, defId));
        for (WfNodeDetailPerm p : detailPerms) {
            p.setId(null);
            p.setDefId(neo.getId());
            detailPermMapper.insert(p);
        }
        // 复制节点操作者（nodeId 关联 wf_process_node.id，须经映射换成新节点ID）
        for (Map.Entry<Long, Long> e : nodeIdMap.entrySet()) {
            List<WfNodeOperator> ops = operatorMapper.selectList(
                Wrappers.<WfNodeOperator>lambdaQuery().eq(WfNodeOperator::getNodeId, e.getKey()));
            for (WfNodeOperator op : ops) {
                op.setId(null);
                op.setNodeId(e.getValue());
                operatorMapper.insert(op);
            }
        }
        log.info("[blade-workflow] 已另存为新版本. sourceDefId={}, newDefId={}, version={}, 复制节点={} 出口={} 字段权限={} 明细权限={} 操作者组={}",
            defId, neo.getId(), newVersion, nodes.size(), links.size(),
            perms.size(), detailPerms.size(), nodeIdMap.size());
        return neo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean testDeploy(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        if (def.getBpmnXml() == null || def.getBpmnXml().isBlank()) {
            throw new ServiceException("尚无 BPMN 定义，请先在「流程画布」中设计并保存");
        }
        if (def.getProcKey() == null || def.getProcKey().isBlank()) {
            throw new ServiceException("流程定义缺少 procKey（应由画布 BPMN process id 提供）");
        }
        // 先确保已部署到测试引擎（独立 __test key，不顶正式版本、不激活版本）
        deployForTest(defId);
        // 置为「测试」态：已部署测试引擎，可发起测试单，但不可正式发起
        def.setStatus(3);
        defMapper.updateById(def);
        log.info("[blade-workflow] 流程定义已标记为测试态（部署到测试引擎）. defId={}, procKey={}",
            defId, def.getProcKey());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        int status = def.getStatus() == null ? 0 : def.getStatus();
        if (status != 1 && status != 3) {
            throw new ServiceException("仅「已发布 / 测试」状态可撤回为草稿");
        }
        // 挂起引擎部署：正式态挂起正式 procDefId；测试态挂起 __test 最新 procDefId
        if (status == 1) {
            processService.suspendProcessDefinition(def.getProcDefId());
        } else {
            processService.suspendProcessDefinition(
                processService.latestProcDefId(def.getProcKey() + WorkflowConstant.TEST_DEPLOY_KEY_SUFFIX));
        }
        def.setStatus(0);
        defMapper.updateById(def);
        log.info("[blade-workflow] 流程定义已撤回为草稿并挂起引擎部署. defId={}, procKey={}, 原状态={}",
            defId, def.getProcKey(), status);
        return true;
    }

    @Override
    public List<WfProcessDefinition> versions(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        return versionGroup(anchorOf(def));
    }

    @Override
    public VersionDiffVO diff(Long defId, Long targetId) {
        WfProcessDefinition s = defMapper.selectById(defId);
        WfProcessDefinition t = defMapper.selectById(targetId);
        if (s == null || t == null) {
            throw new ServiceException("流程定义不存在");
        }
        VersionDiffVO vo = new VersionDiffVO();
        vo.setSourceDefId(s.getId());
        vo.setSourceVersion(s.getVersion());
        vo.setTargetDefId(t.getId());
        vo.setTargetVersion(t.getVersion());

        // 节点对比：按 nodeKey 匹配
        Map<String, WfProcessNode> sn = new LinkedHashMap<>();
        for (WfProcessNode n : nodes(defId)) {
            sn.putIfAbsent(n.getNodeKey(), n);
        }
        Map<String, WfProcessNode> tn = new LinkedHashMap<>();
        for (WfProcessNode n : nodes(targetId)) {
            tn.putIfAbsent(n.getNodeKey(), n);
        }
        for (Map.Entry<String, WfProcessNode> e : sn.entrySet()) {
            WfProcessNode ts = tn.get(e.getKey());
            if (ts == null) {
                vo.getRemovedNodes().add(toNodeDiff(e.getValue(), null));
            } else if (nodeChanged(e.getValue(), ts)) {
                vo.getChangedNodes().add(toNodeDiff(e.getValue(), ts));
            }
        }
        for (Map.Entry<String, WfProcessNode> e : tn.entrySet()) {
            if (!sn.containsKey(e.getKey())) {
                vo.getAddedNodes().add(toNodeDiff(null, e.getValue()));
            }
        }

        // 出口对比：按 fromNodeKey-toNodeKey 匹配
        Map<String, WfNodeLink> sl = new LinkedHashMap<>();
        for (WfNodeLink l : links(defId)) {
            sl.putIfAbsent(l.getFromNodeKey() + "->" + l.getToNodeKey(), l);
        }
        Map<String, WfNodeLink> tl = new LinkedHashMap<>();
        for (WfNodeLink l : links(targetId)) {
            tl.putIfAbsent(l.getFromNodeKey() + "->" + l.getToNodeKey(), l);
        }
        for (Map.Entry<String, WfNodeLink> e : sl.entrySet()) {
            WfNodeLink tLink = tl.get(e.getKey());
            if (tLink == null) {
                vo.getRemovedLinks().add(toLinkDiff(e.getValue(), null));
            } else if (linkChanged(e.getValue(), tLink)) {
                vo.getChangedLinks().add(toLinkDiff(e.getValue(), tLink));
            }
        }
        for (Map.Entry<String, WfNodeLink> e : tl.entrySet()) {
            if (!sl.containsKey(e.getKey())) {
                vo.getAddedLinks().add(toLinkDiff(null, e.getValue()));
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateVersion(Long defId) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        // 仅切换版本组锚点：不部署引擎、不改 status（区别于 deploy 的“发布即激活”）
        setGroupAnchor(def);
        log.info("[blade-workflow] 已切换当前（激活）版本. defId={}, procKey={}, version={}",
            defId, def.getProcKey(), def.getVersion());
        return true;
    }

    /** 版本组锚点：active_version_id 为空时视为单版本流程（锚点 = 自身） */
    private Long anchorOf(WfProcessDefinition def) {
        return def.getActiveVersionId() != null ? def.getActiveVersionId() : def.getId();
    }

    /** 版本组：active_version_id = 锚点 OR id = 锚点（对齐 ecology WorkflowVersion 组查询语义） */
    private List<WfProcessDefinition> versionGroup(Long anchor) {
        return defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .and(w -> w.eq(WfProcessDefinition::getActiveVersionId, anchor)
                .or().eq(WfProcessDefinition::getId, anchor))
            .orderByAsc(WfProcessDefinition::getVersion));
    }

    /** 版本组锚点统一切到本版本（不含发布停用等副作用） */
    private void setGroupAnchor(WfProcessDefinition def) {
        Long target = def.getId();
        for (WfProcessDefinition g : versionGroup(anchorOf(def))) {
            if (!Objects.equals(g.getActiveVersionId(), target)) {
                g.setActiveVersionId(target);
                defMapper.updateById(g);
            }
        }
    }

    /** 发布即激活：组内锚点统一切到本版本。旧版本保持各自 status 不变（已发布即一直为已发布，
     *  当前生效版本由 activeVersionId 锚点决定，不再用「停用(2)」态表达，三态模型：草稿/测试/已发布） */
    private void promoteActiveVersion(WfProcessDefinition def) {
        setGroupAnchor(def);
    }

    private VersionDiffVO.NodeDiff toNodeDiff(WfProcessNode s, WfProcessNode t) {
        VersionDiffVO.NodeDiff d = new VersionDiffVO.NodeDiff();
        d.setNodeKey((s != null ? s : t).getNodeKey());
        if (s != null) {
            d.setSourceName(s.getNodeName());
            d.setSourceType(s.getNodeType());
            d.setSourceSignOrder(s.getSignOrder());
        }
        if (t != null) {
            d.setTargetName(t.getNodeName());
            d.setTargetType(t.getNodeType());
            d.setTargetSignOrder(t.getSignOrder());
        }
        return d;
    }

    private boolean nodeChanged(WfProcessNode s, WfProcessNode t) {
        return !Objects.equals(s.getNodeName(), t.getNodeName())
            || !Objects.equals(s.getNodeType(), t.getNodeType())
            || !Objects.equals(s.getSignOrder(), t.getSignOrder());
    }

    private VersionDiffVO.LinkDiff toLinkDiff(WfNodeLink s, WfNodeLink t) {
        VersionDiffVO.LinkDiff d = new VersionDiffVO.LinkDiff();
        d.setFromNodeKey((s != null ? s : t).getFromNodeKey());
        d.setToNodeKey((s != null ? s : t).getToNodeKey());
        if (s != null) {
            d.setSourceConditionCn(s.getConditionCn());
            d.setSourceIsReject(s.getIsReject());
        }
        if (t != null) {
            d.setTargetConditionCn(t.getConditionCn());
            d.setTargetIsReject(t.getIsReject());
        }
        return d;
    }

    private boolean linkChanged(WfNodeLink s, WfNodeLink t) {
        return !Objects.equals(s.getConditionCn(), t.getConditionCn())
            || !Objects.equals(s.getConditionExpr(), t.getConditionExpr())
            || !Objects.equals(s.getIsReject(), t.getIsReject())
            || !Objects.equals(s.getIsMustPass(), t.getIsMustPass());
    }

    @Override
    public InstanceVO instanceDetail(Long id) {
        return instanceService.detail(id);
    }

    @Override
    public List<WfNodeLink> links(Long defId) {
        return linkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
            .eq(WfNodeLink::getDefId, defId)
            .orderByAsc(WfNodeLink::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfProcessNode updateNode(Long defId, String nodeKey, WfProcessNode patch) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        if (node == null) {
            throw new ServiceException("节点不存在: " + nodeKey);
        }
        if (patch != null) {
            if (patch.getNodeName() != null) {
                node.setNodeName(patch.getNodeName());
            }
            if (patch.getNodeType() != null) {
                node.setNodeType(patch.getNodeType());
            }
            if (patch.getSignOrder() != null) {
                node.setSignOrder(patch.getSignOrder());
            }
            if (patch.getMergeType() != null) {
                node.setMergeType(patch.getMergeType());
            }
            if (patch.getPassNum() != null) {
                node.setPassNum(patch.getPassNum());
            }
            if (patch.getAllowReject() != null) {
                node.setAllowReject(patch.getAllowReject());
            }
            if (patch.getAllowForward() != null) {
                node.setAllowForward(patch.getAllowForward());
            }
            if (patch.getAutoApprove() != null) {
                node.setAutoApprove(patch.getAutoApprove());
            }
            if (patch.getSortOrder() != null) {
                node.setSortOrder(patch.getSortOrder());
            }
            if (patch.getExtJson() != null) {
                node.setExtJson(patch.getExtJson());
            }
        }
        nodeMapper.updateById(node);
        return node;
    }

    @Override
    public List<WfNodeOperator> nodeOperators(Long defId, String nodeKey) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        if (node == null) {
            return Collections.emptyList();
        }
        return operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
            .eq(WfNodeOperator::getNodeId, node.getId())
            .orderByAsc(WfNodeOperator::getGroupNo)
            .orderByAsc(WfNodeOperator::getBatchNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfNodeLink saveLink(Long defId, WfNodeLink link) {
        if (link == null) {
            throw new ServiceException("出口不能为空");
        }
        link.setDefId(defId);
        if (link.getId() == null) {
            if (link.getIsReject() == null) {
                link.setIsReject(0);
            }
            if (link.getIsMustPass() == null) {
                link.setIsMustPass(0);
            }
            if (link.getSortOrder() == null) {
                link.setSortOrder(0);
            }
            linkMapper.insert(link);
        } else {
            WfNodeLink exist = linkMapper.selectById(link.getId());
            if (exist == null || !defId.equals(exist.getDefId())) {
                throw new ServiceException("出口不存在");
            }
            linkMapper.updateById(link);
        }
        return link;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLink(Long defId, Long linkId) {
        if (linkId == null) {
            return false;
        }
        WfNodeLink exist = linkMapper.selectById(linkId);
        if (exist == null || !defId.equals(exist.getDefId())) {
            return false;
        }
        linkMapper.deleteById(linkId);
        return true;
    }

    /**
     * 移除节点并级联清理其全部关联数据，避免残留无用 / 孤立数据。
     *
     * <p>清理顺序（先明细后主记录）：
     * ① 操作者 wf_node_operator（按 node_id）
     * ② 字段权限 wf_node_field_perm、明细权限 wf_node_detail_perm（按 def_id + node_key）
     * ③ 出口连线 wf_node_link（进出该节点的都要删，否则连线指向已不存在的节点）
     * ④ 节点本身 wf_process_node
     * ⑤ 跨服务清理该节点保存过的布局 form_layout（best-effort：表单服务不可用时仅告警，不回滚主流程）</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null || nodeKey.isEmpty()) {
            return false;
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        if (node == null) {
            throw new ServiceException("节点不存在: " + nodeKey);
        }

        // ① 操作者
        if (node.getId() != null) {
            operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
                .eq(WfNodeOperator::getNodeId, node.getId()));
        }
        // ② 字段权限 / 明细权限
        fieldPermMapper.delete(Wrappers.<WfNodeFieldPerm>lambdaQuery()
            .eq(WfNodeFieldPerm::getDefId, defId)
            .eq(WfNodeFieldPerm::getNodeKey, nodeKey));
        detailPermMapper.delete(Wrappers.<WfNodeDetailPerm>lambdaQuery()
            .eq(WfNodeDetailPerm::getDefId, defId)
            .eq(WfNodeDetailPerm::getNodeKey, nodeKey));
        // ③ 出口连线（进 / 出）
        linkMapper.delete(Wrappers.<WfNodeLink>lambdaQuery()
            .eq(WfNodeLink::getDefId, defId)
            .and(w -> w.eq(WfNodeLink::getFromNodeKey, nodeKey)
                .or()
                .eq(WfNodeLink::getToNodeKey, nodeKey)));
        // ④ 节点本身
        nodeMapper.deleteById(node.getId());

        // ⑤ 节点布局（form_layout）：没保存过则无记录，保存过的一并删除，避免孤儿布局
        try {
            WfProcessDefinition def = defMapper.selectById(defId);
            if (def != null && def.getFormId() != null && formmodeClient != null) {
                formmodeClient.deleteFormLayoutByNode(def.getFormId(), nodeKey);
            }
        } catch (Exception e) {
            // 跨服务清理失败不影响节点移除结果，仅记录告警（布局记录可在表单侧单独清理）
            log.warn("[blade-workflow] 清理节点布局失败（节点已移除）. defId={}, nodeKey={}", defId, nodeKey, e);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDefinition(Long id) {
        WfProcessDefinition def = defMapper.selectById(id);
        if (def == null) {
            return false;
        }
        // 删除前保护：参考 Weaver WFMainManager.getCheckBox —— 同一流程版本组（按 active_version_id 锚点）内
        // 只要存在「正式（非测试）且在用（未删除）」的流程实例，就禁止删除，避免误删正在被使用的流程。
        Long anchor = anchorOf(def);
        List<WfProcessDefinition> group = versionGroup(anchor);
        List<Long> groupIds = group.stream().map(WfProcessDefinition::getId).collect(Collectors.toList());
        Long used = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
            .in(WfInstance::getDefId, groupIds)
            .eq(WfInstance::getIsTest, 0)
            .eq(WfInstance::getIsDeleted, 0));
        if (used != null && used > 0) {
            throw new ServiceException("该流程正在使用中（存在流程实例），无法删除");
        }
        // ① 节点列表（用于级联清理操作者 / 布局）
        List<WfProcessNode> nodes = nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, id));
        List<Long> nodeIds = nodes.stream().map(WfProcessNode::getId)
            .filter(Objects::nonNull).collect(Collectors.toList());
        // ② 操作者（按 node_id 批量）
        if (!nodeIds.isEmpty()) {
            operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
                .in(WfNodeOperator::getNodeId, nodeIds));
        }
        // ③ 字段权限 / 明细权限 / 出口连线（按 def_id 整定义清理）
        fieldPermMapper.delete(Wrappers.<WfNodeFieldPerm>lambdaQuery().eq(WfNodeFieldPerm::getDefId, id));
        detailPermMapper.delete(Wrappers.<WfNodeDetailPerm>lambdaQuery().eq(WfNodeDetailPerm::getDefId, id));
        linkMapper.delete(Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, id));
        // ④ 节点本身
        nodeMapper.delete(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, id));
        // ⑤ 表单布局（best-effort，跨服务）
        try {
            if (def.getFormId() != null && formmodeClient != null) {
                for (WfProcessNode n : nodes) {
                    formmodeClient.deleteFormLayoutByNode(def.getFormId(), n.getNodeKey());
                }
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 清理流程布局失败（定义已移除）. defId={}", id, e);
        }
        // ⑥ 删除定义本身（逻辑删除或物理删除取决于实体 @TableLogic 配置；均会从列表消失）
        defMapper.deleteById(id);
        return true;
    }

    @Override
    public FormConditionVO getFormCondition(String method, Long id) {
        boolean edit = "edit".equalsIgnoreCase(method);
        FormConditionVO vo = new FormConditionVO();
        vo.setMethod(edit ? "edit" : "add");
        vo.setTitle(edit ? "编辑路径" : "添加路径");
        vo.setFields(buildDefinitionFields());
        return vo;
    }

    @Override
    public List<BrowserOptionVO> listBrowserOptions(String type, String keyword) {
        if (!"wftype".equalsIgnoreCase(type)) {
            return Collections.emptyList();
        }
        var q = Wrappers.<WfWorkflowType>lambdaQuery()
            .eq(WfWorkflowType::getStatus, 1)
            .orderByAsc(WfWorkflowType::getSortOrder);
        if (keyword != null && !keyword.isBlank()) {
            q.like(WfWorkflowType::getTypeName, keyword);
        }
        return wfWorkflowTypeMapper.selectList(q).stream().map(t -> {
            BrowserOptionVO o = new BrowserOptionVO();
            o.setValue(t.getId());
            o.setLabel(t.getTypeName());
            o.setDescription(t.getTypeDesc());
            return o;
        }).collect(Collectors.toList());
    }

    @Override
    public BrowserOptionVO getBrowserOption(String type, Long id) {
        if (!"wftype".equalsIgnoreCase(type) || id == null) {
            return null;
        }
        WfWorkflowType t = wfWorkflowTypeMapper.selectById(id);
        if (t == null) {
            return null;
        }
        BrowserOptionVO o = new BrowserOptionVO();
        o.setValue(t.getId());
        o.setLabel(t.getTypeName());
        o.setDescription(t.getTypeDesc());
        return o;
    }

    @Override
    public BrowserOptionVO saveBrowserOption(String type, WfWorkflowType data) {
        if (!"wftype".equalsIgnoreCase(type)) {
            throw new ServiceException("不支持的浏览框类型：" + type);
        }
        if (data == null || data.getTypeName() == null || data.getTypeName().isBlank()) {
            throw new ServiceException("路径类型名称不能为空");
        }
        if (data.getSortOrder() == null) {
            data.setSortOrder(0);
        }
        // tenant_id / create_user 等由 MetaObjectHandler 自动填充；status 默认 1（启用）
        wfWorkflowTypeMapper.insert(data);
        BrowserOptionVO o = new BrowserOptionVO();
        o.setValue(data.getId());
        o.setLabel(data.getTypeName());
        o.setDescription(data.getTypeDesc());
        return o;
    }

    /** 元素 → 节点类型；非节点元素（网关/子流程等）返回 null。0创建 1审批 3归档 */
    private Integer nodeTypeOf(FlowElement fe) {
        if (fe instanceof StartEvent) {
            return 0;
        }
        if (fe instanceof UserTask) {
            return 1;
        }
        if (fe instanceof EndEvent) {
            return 3;
        }
        // 网关 → 网关节点（7）。此前网关不入 wf_process_node，连线被折叠成「节点→节点」逻辑出口
        // （A→网关→B 记为 A→B），导致网关旁的线（A→网关 / 网关→B）没有属于自己的出口记录、
        // 「出口信息」里点不出详情。改为网关入节点表后，每条物理连线各存一条出口，
        // 每条线都能独立查看 / 配置出口信息。
        if (fe instanceof Gateway) {
            return 7;
        }
        // 中间事件（中间件，catch/throw 都落到这里）/ 边界事件：画布上追加后也要落成「等待」节点，
        // 否则节点信息与出口会丢失。（flowable 无 IntermediateThrowEvent 类，抛出事件解析为
        // IntermediateCatchEvent 或 ThrowEvent 基类，故用 ThrowEvent 兜底）
        if (fe instanceof IntermediateCatchEvent || fe instanceof ThrowEvent
            || fe instanceof BoundaryEvent) {
            return 5;
        }
        // 服务任务 → 自动处理
        if (fe instanceof ServiceTask) {
            return 6;
        }
        // 画布「更改元素」（bpmn-js 替换元素）会为新元素**重新生成 id**（原 id 仍被旧元素占用，
        // moddle ids 拒绝复用）。若此处不识别新类型，saveBpmn 会跳过它 → 「节点信息」不生成，
        // 且旧节点因「画布上已不存在」被连带删除。故补齐其余任务类元素。
        // 调用活动（调用子流程）→ 自动处理
        if (fe instanceof CallActivity) {
            return 6;
        }
        if (fe instanceof Task) {
            // 发送 / 脚本 / 业务规则任务 → 自动处理；接收任务 → 等待（等外部消息）
            if (fe instanceof SendTask || fe instanceof ScriptTask
                || fe instanceof BusinessRuleTask) {
                return 6;
            }
            if (fe instanceof ReceiveTask) {
                return 5;
            }
            // 通用任务 / 手工任务（UserTask 已在前面返回）→ 审批
            return 1;
        }
        return null;
    }

    /** 节点默认名称：优先取画布名称，缺省按类型兜底 */
    private String defaultNodeName(FlowElement fe, Integer nodeType) {
        if (fe.getName() != null && !fe.getName().isBlank()) {
            return fe.getName();
        }
        if (fe instanceof IntermediateCatchEvent || fe instanceof ThrowEvent) {
            return "中间事件";
        }
        if (fe instanceof BoundaryEvent) {
            return "边界事件";
        }
        if (fe instanceof ServiceTask) {
            return "自动处理";
        }
        // 画布「更改元素」新纳入识别的任务类型：补中文默认名（节点信息列表不显示裸 id）
        if (fe instanceof ReceiveTask) {
            return "等待";
        }
        if (fe instanceof SendTask || fe instanceof ScriptTask
            || fe instanceof BusinessRuleTask || fe instanceof CallActivity) {
            return "自动处理";
        }
        if (fe instanceof Task) {
            return "审批";
        }
        if (fe instanceof Gateway) {
            return "网关";
        }
        if (nodeType != null && nodeType == 0) {
            return "开始";
        }
        if (nodeType != null && nodeType == 3) {
            return "结束";
        }
        return fe.getId();
    }

    private String linkKey(String from, String to) {
        return from + "→" + to;
    }

    /**
     * 测试态 BPMN 消毒：把「依赖可选依赖 / 运行期必失败 / 必卡死等待」的元素降级为 manualTask
     * （具体类、运行时自动通过），让草稿能用「关闭校验」的方式部署并跑通主干路径，
     * 而不引入 Drools 等重型依赖、也不因未配置元素卡死。
     * 当前处理：
     *  - businessRuleTask（Flowable 解析即需 org.kie / Drools 类）；
     *  - intermediateThrowEvent（未配置 / 不支持的抛出事件，运行期失败）；
     *  - intermediateCatchEvent / boundaryEvent 含 timerEventDefinition（未配置则卡死、已配置则测试不应真等待）。
     * 另外把 main process 的 id 改写为测试独立 key（{@code procKey + "__test"}）——Flowable 的流程 key
     * 取自 BPMN 的 {@code <process id>}，不改这里则引擎中不存在该 key（测试发起报 no process definition）。
     * 消毒失败时回退原 XML，保证部署不中断。
     */
    private String neutralizeForTest(String bpmnXml, String testKey) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return bpmnXml;
        }
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            BpmnModel model = converter.convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, false);
            Process process = model.getMainProcess();
            boolean changed = false;
            // ⚠️ 关键：Flowable 的「流程 key」来自 BPMN 的 <process id="...">，**不是**部署名/资源名。
            //    测试必须把 process id 改成独立 key（procKey + "__test"），否则：
            //      ① 引擎里不存在 __test 这个 key → 测试发起 startProcessInstanceByKey("xxx__test")
            //         报 "no processes deployed with key 'xxx__test'"（即日志 no process definition）；
            //      ② 测试部署会落到正式 procKey 的版本序列上，把正式版本顶掉（一致性规范 §1.5 风险1）。
            if (testKey != null && !testKey.isBlank() && !testKey.equals(process.getId())) {
                log.info("[blade-workflow] 测试态改用独立流程 key：process id {} → {}", process.getId(), testKey);
                process.setId(testKey);
                changed = true;
            }
            for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
                if (needsNeutralize(fe)) {
                    ManualTask task = new ManualTask();
                    task.setId(fe.getId());
                    task.setName(fe.getName());
                    process.removeFlowElement(fe.getId());
                    process.addFlowElement(task);
                    changed = true;
                    log.info("[blade-workflow] 测试态消毒：{} 降级为 manualTask（自动通过） id={}",
                        fe.getClass().getSimpleName(), fe.getId());
                }
            }
            if (!changed) {
                return bpmnXml;
            }
            byte[] out = converter.convertToXML(model);
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[blade-workflow] 测试态 BPMN 消毒失败，使用原 BPMN 部署: {}", e.getMessage());
            return bpmnXml;
        }
    }

    /** 测试态需降级为自动通过占位的元素：依赖可选依赖 / 运行期必失败 / 必卡死等待。 */
    private boolean needsNeutralize(FlowElement fe) {
        if (fe instanceof BusinessRuleTask) {
            return true; // 解析期需 org.kie（Drools），本工程未引入
        }
        if (fe instanceof ThrowEvent) {
            return true; // 中间抛出事件：未配置 / 不支持的抛出事件（如缺 messageRef 的 message throw）运行期失败
        }
        if (fe instanceof IntermediateCatchEvent ice) {
            return hasTimer(ice.getEventDefinitions()); // 定时捕获：未配置卡死、已配置测试不应真等待
        }
        if (fe instanceof BoundaryEvent be) {
            return hasTimer(be.getEventDefinitions());
        }
        return false;
    }

    private boolean hasTimer(List<EventDefinition> defs) {
        if (defs == null) {
            return false;
        }
        for (EventDefinition ed : defs) {
            if (ed instanceof TimerEventDefinition) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发布门禁（正式部署前）：测试态靠「关闭校验 + {@link #neutralizeForTest 消毒}」才能跑通，
     * 正式部署保留完整校验，故必须在这里把「测试过、正式跑不了」的情况拦住。
     *
     * <p><b>① 元素级（硬拦）</b>：{@code businessRuleTask} 解析期需要 org.kie（Drools），
     * 本工程未引入 → 测试被降级成 manualTask「自动通过」，正式部署必被引擎拒。指名道姓报错。</p>
     *
     * <p><b>② 元素级（强提示）</b>：{@code ThrowEvent}（未配置/不支持的抛出事件运行期失败）、
     * 含 timer 的 catch/boundary 事件（正式环境会真等待）→ 记 warn，不阻断（可能是刻意设计）。</p>
     *
     * <p><b>③ 结构级（硬拦）</b>：BPMN 中的节点集合必须 ⊆/≡ {@code wf_process_node} 存活节点。
     * 画布改了没保存、或节点配置被逻辑删除时，引擎按 BPMN 跑而语义层查不到节点配置
     * （发起后无人可办、网关出口条件缺失），属于典型「测试能跑、正式跑不通」。</p>
     */
    private void validateForDeploy(WfProcessDefinition def) {
        BpmnModel model;
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            model = converter.convertToBpmnModel(
                () -> new ByteArrayInputStream(def.getBpmnXml().getBytes(StandardCharsets.UTF_8)), false, false);
        } catch (Exception e) {
            throw new ServiceException("发布被拒绝：BPMN 解析失败（" + e.getMessage() + "）");
        }
        Process process = model.getMainProcess();

        List<String> hardBlock = new ArrayList<>();
        List<String> warn = new ArrayList<>();
        List<String> bpmnNodeKeys = new ArrayList<>();
        for (FlowElement fe : process.getFlowElements()) {
            String label = (fe.getName() == null || fe.getName().isBlank())
                ? fe.getId() : (fe.getName() + "(" + fe.getId() + ")");
            if (fe instanceof BusinessRuleTask) {
                // 与测试态降级清单一致：正式环境无 Drools，部署期即失败
                hardBlock.add(label);
            } else if (fe instanceof ThrowEvent) {
                warn.add(label + " 抛出事件：正式环境未配置则运行期失败");
            } else if (fe instanceof IntermediateCatchEvent ice) {
                if (hasTimer(ice.getEventDefinitions())) {
                    warn.add(label + " 定时捕获事件：正式环境将真等待");
                }
            } else if (fe instanceof BoundaryEvent be) {
                if (hasTimer(be.getEventDefinitions())) {
                    warn.add(label + " 定时边界事件：正式环境将真等待");
                }
            }
            if (nodeTypeOf(fe) != null) {
                bpmnNodeKeys.add(fe.getId());
            }
        }
        if (!hardBlock.isEmpty()) {
            throw new ServiceException("发布被拒绝：以下节点使用了正式环境不支持的元素（业务规则任务需 Drools 依赖，"
                + "测试态被自动降级才「通过」）→ 请改为「自动处理/脚本任务」等类型并在画布重新保存："
                + String.join("、", hardBlock));
        }
        if (!warn.isEmpty()) {
            log.warn("[blade-workflow] 发布提示：正式环境可能存在运行期失败/等待的节点（不阻断发布）：{}",
                String.join("；", warn));
        }

        // ③ 结构一致性：BPMN 节点 vs 存活节点配置
        List<WfProcessNode> aliveNodes = nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, def.getId()));
        Set<String> alive = aliveNodes.stream().map(WfProcessNode::getNodeKey)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        List<String> missingConf = bpmnNodeKeys.stream().filter(k -> !alive.contains(k)).collect(Collectors.toList());
        if (!missingConf.isEmpty()) {
            throw new ServiceException("发布被拒绝：BPMN 中有 " + missingConf.size() + " 个节点在流程配置里不存在（"
                + String.join("、", missingConf) + "）→ 引擎会按 BPMN 跑，但这些节点没有办理人/出口配置，"
                + "发起后会卡住或无人可办。请先在画布「保存」以按其重建节点配置，再发布。");
        }
        Set<String> bpmnSet = new HashSet<>(bpmnNodeKeys);
        List<String> ghostConf = alive.stream().filter(k -> !bpmnSet.contains(k)).collect(Collectors.toList());
        if (!ghostConf.isEmpty()) {
            log.warn("[blade-workflow] 发布提示：流程配置里有 {} 个节点不在 BPMN 中（多为已删除节点，不影响运行）：{}",
                ghostConf.size(), String.join("、", ghostConf));
        }
        log.info("[blade-workflow] 发布门禁通过. defId={}, bpmnNodes={}, aliveNodes={}", def.getId(),
            bpmnNodeKeys.size(), alive.size());
    }

    /**
     * 把出口条件表达式注入 BPMN 的 sequenceFlow.conditionExpression，返回新的 XML。
     * 注入失败时回退原 XML，保证部署不中断。
     */
    private String injectLinkConditions(String bpmnXml, List<WfNodeLink> links) {
        if (bpmnXml == null || bpmnXml.isBlank() || links == null || links.isEmpty()) {
            return bpmnXml;
        }
        Map<String, String> condMap = new LinkedHashMap<>();
        for (WfNodeLink l : links) {
            if (l.getConditionExpr() != null && !l.getConditionExpr().isBlank()) {
                condMap.putIfAbsent(linkKey(l.getFromNodeKey(), l.getToNodeKey()), l.getConditionExpr());
            }
        }
        if (condMap.isEmpty()) {
            return bpmnXml;
        }
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            BpmnModel model = converter.convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, false);
            Process process = model.getMainProcess();
            // 全量出边邻接（source → 其全部 sequenceFlow），用于把「节点→节点」逻辑出口
            // 映射到实际 sequenceFlow：直接连线对 source=from&target=to；网关分支则取
            // from 经网关穿透、末跳进入 to 的那条（即网关分支判别条件应挂的线）。
            Map<String, List<SequenceFlow>> outFlows = new LinkedHashMap<>();
            for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
                if (fe instanceof SequenceFlow sf && sf.getSourceRef() != null && sf.getTargetRef() != null) {
                    outFlows.computeIfAbsent(sf.getSourceRef(), k -> new ArrayList<>()).add(sf);
                }
            }
            for (Map.Entry<String, String> en : condMap.entrySet()) {
                String[] pair = en.getKey().split("→", 2);
                if (pair.length != 2) {
                    continue;
                }
                String from = pair[0];
                String to = pair[1];
                SequenceFlow target = findBranchFlow(outFlows, from, to);
                if (target != null) {
                    target.setConditionExpression(en.getValue());
                }
            }
            byte[] out = converter.convertToXML(model);
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[blade-workflow] 出口条件注入失败，使用原 BPMN 部署（条件将不生效）: {}", e.getMessage());
            return bpmnXml;
        }
    }

    /**
     * 在 BPMN 出边邻接里，找到「逻辑出口 from→to」对应的实际 sequenceFlow：
     * 优先直接连线（source=from & target=to）；否则从 from 经网关（非 from/to 的中间顶点）穿透 BFS，
     * 命中 target=to 的那条即网关分支末跳线。
     */
    private SequenceFlow findBranchFlow(Map<String, List<SequenceFlow>> outFlows, String from, String to) {
        // 直接连线优先
        for (SequenceFlow sf : outFlows.getOrDefault(from, Collections.emptyList())) {
            if (to.equals(sf.getTargetRef())) {
                return sf;
            }
        }
        // 网关穿透：中间顶点既非 from 也非 to
        java.util.Deque<String> queue = new java.util.ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(from);
        visited.add(from);
        while (!queue.isEmpty()) {
            String v = queue.poll();
            for (SequenceFlow sf : outFlows.getOrDefault(v, Collections.emptyList())) {
                String w = sf.getTargetRef();
                if (to.equals(w)) {
                    return sf;
                }
                if (!from.equals(w) && !to.equals(w) && visited.add(w)) {
                    queue.add(w);
                }
            }
        }
        return null;
    }

    /**
     * 构建「流程（路径）」表单字段清单（condition 驱动）。
     *
     * <p>字段的增减、标签、必填、控件类型、选项、校验与提示均在此维护，
     * 前端自动渲染，无需改前端代码。<b>该结构对齐 ecology
     * {@code GetBaseInfoConditionCmd.getBaseInfoItemList()}</b> 的 conditioninfo 模型
     * （SearchConditionItem：类型 / 标签 / key / 值 / rules / options / viewAttr /
     * selectLinkage / browser 参数）；差异仅在控件渲染由前端 SchemaForm 承担。</p>
     */
    private List<FormFieldVO> buildDefinitionFields() {
        List<FormFieldVO> fields = new ArrayList<>();

        // 1. 路径名称 —— INPUT，必填，最大 200（对齐 ecology workflowName: rules=required|stringLength:200）
        FormFieldVO name = field("name", "路径名称", "input", true);
        name.setPlaceholder("请输入路径名称");
        name.setMaxLength(200);
        name.setRules("required|stringLength:200");
        fields.add(name);

        // 2. 路径类型 —— 对齐 ecology getWorkflowTypeItem（BROWSER "wftype" 浏览框，带新增按钮）
        //    点击弹出选择弹窗，回写 workflow_type.id（前端 BrowserBox 通过 /definition/browser/wftype 取数）
        FormFieldVO type = field("type", "路径类型", "browser", true);
        type.setPlaceholder("请选择路径类型");
        type.setRules("required");
        type.setDataSource("wftype");
        FormFieldVO.BrowserParamVO browser = new FormFieldVO.BrowserParamVO();
        browser.setType("wftype");
        browser.setIcon("workflow");
        browser.setIconBgcolor("#4f8cff");
        browser.setHasAdd(true);
        browser.setMultiple(false);
        type.setBrowser(browser);
        fields.add(type);

        // 3. 对应表单 —— 对齐 ecology getTargetFormItem（isBill SELECT_LINKAGE 联动自定义/系统表单浏览框）
        //    formType 选择 0/1 时，由前端 formSelect 联动显示对应表单浏览（control=formSelect）
        FormFieldVO form = field("form", "对应表单", "formSelect", true);
        form.setDataSource("formList");
        form.setOptions(Arrays.asList(
            option(0, "自定义表单"),
            option(1, "系统表单")
        ));
        FormFieldVO.EmitsVO emits = new FormFieldVO.EmitsVO();
        emits.setTypeKey("formType");
        emits.setIdKey("formId");
        form.setEmits(emits);
        fields.add(form);

        // 4. 路径描述 —— TEXTAREA，最大 1000（对齐 ecology workflowDesc: rules=stringLength:1000）
        FormFieldVO desc = field("description", "路径描述", "textarea", false);
        desc.setPlaceholder("请输入路径描述");
        desc.setRows(4);
        desc.setMaxLength(1000);
        desc.setRules("stringLength:1000");
        fields.add(desc);

        // 5. 流程状态 —— 三态：0草稿 1已发布 3测试（已发布/测试均已部署到引擎，不变式由
        //   deploy/deployForTest 构造保证；「停用(2)」已废除，下线统一走「撤回」→ 草稿并挂起引擎。
        //   测试态流程：仅 /formmode/test 可选，不进正式发起页 /workflow/create）
        FormFieldVO status = field("status", "流程状态", "select", false);
        status.setDefaultValue(0);
        status.setOptions(Arrays.asList(
            option(0, "草稿"),
            option(1, "已发布"),
            option(3, "测试")
        ));
        fields.add(status);

        // 6. 显示顺序 —— 对齐 ecology showOrder（INPUT，最大长度 8）
        FormFieldVO order = field("sortOrder", "显示顺序", "number", false);
        order.setDefaultValue(0);
        order.setMin(0);
        order.setMax(99999999);
        fields.add(order);

        // 7. 是否为自由流程 —— SWITCH（对齐 ecology isFreeWf）
        FormFieldVO free = field("isFree", "是否为自由流程", "switch", false);
        free.setDefaultValue(0);
        free.setTip("提示：自由流程一旦启用，保存后即不允许关闭");
        fields.add(free);

        // 8. 自由流程类型（简易/高级）—— 仅当「是否为自由流程」开启时显示
        //    对齐 ecology newFreeWfType（getBaseInfoItemList 中 isFree==1 的分支）
        FormFieldVO freeType = field("freeWfType", "自由流程类型", "select", false);
        freeType.setDefaultValue(1);
        freeType.setOptions(Arrays.asList(
            option(1, "简易模式"),
            option(2, "高级模式")
        ));
        FormFieldVO.VisibleWhenVO vis = new FormFieldVO.VisibleWhenVO();
        vis.setKey("isFree");
        vis.setEquals(true);
        freeType.setVisibleWhen(vis);
        fields.add(freeType);

        return fields;
    }

    private FormFieldVO field(String key, String label, String control, boolean required) {
        FormFieldVO f = new FormFieldVO();
        f.setKey(key);
        f.setLabel(label);
        f.setControl(control);
        f.setRequired(required);
        return f;
    }

    private FormFieldVO.OptionVO option(Object value, String label) {
        FormFieldVO.OptionVO o = new FormFieldVO.OptionVO();
        o.setValue(value);
        o.setLabel(label);
        return o;
    }

    // ============================================================
    // 流程模拟运行（设计期校验）：带模拟表单数据走查节点 / 网关条件
    // ============================================================

    /**
     * 流程模拟运行：从开始节点出发，按出口条件（结合模拟表单数据）选择分支，逐节点校验其配置
     * 是否足以正常流转，回写每条节点的测试状态（test_status）；全部通过方可生成出口属性。
     *
     * <p>走查逻辑：每个节点按 nodeType 校验（人工节点需配置操作者）；某个节点有多条出口时，
     * 取「无条件」或「条件求值为真」的分支（条件都不满足时退回走无条件的默认分支），并行网关
     * 会同时走多条；路径环与步数均设上限，避免死循环。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SimulateResultVO simulate(Long defId, Map<String, Object> formData) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        List<WfProcessNode> nodeList = nodes(defId);
        List<WfNodeLink> linkList = links(defId);
        // 先重置全部节点测试状态，再标记本次走查到的节点
        for (WfProcessNode n : nodeList) {
            if (n.getTestStatus() == null || n.getTestStatus() != 0) {
                n.setTestStatus(0);
                nodeMapper.updateById(n);
            }
        }
        Map<String, WfProcessNode> nodeMap = new LinkedHashMap<>();
        for (WfProcessNode n : nodeList) {
            nodeMap.putIfAbsent(n.getNodeKey(), n);
        }
        Map<String, List<WfNodeLink>> outLinks = new LinkedHashMap<>();
        for (WfNodeLink l : linkList) {
            outLinks.computeIfAbsent(l.getFromNodeKey(), k -> new ArrayList<>()).add(l);
        }

        SimulateResultVO result = new SimulateResultVO();
        List<SimulateResultVO.NodeSimResult> nodeResults = new ArrayList<>();
        List<SimulateResultVO.PathStep> path = new ArrayList<>();
        Map<String, Integer> nodeStatus = new LinkedHashMap<>();
        Map<String, String> nodeMsg = new LinkedHashMap<>();
        // 未带模拟表单数据 → 全量走查（忽略条件，验证连通性）；带数据 → 按条件选分支
        boolean full = formData == null || formData.isEmpty();

        // 起点：创建节点（nodeType=0）；否则取无入边的节点；再否则取第一个节点
        String startKey = nodeList.stream().filter(n -> n.getNodeType() != null && n.getNodeType() == 0)
            .map(WfProcessNode::getNodeKey).findFirst().orElse(null);
        if (startKey == null) {
            Set<String> hasIncoming = new HashSet<>();
            for (WfNodeLink l : linkList) {
                hasIncoming.add(l.getToNodeKey());
            }
            startKey = nodeList.stream().filter(n -> !hasIncoming.contains(n.getNodeKey()))
                .map(WfProcessNode::getNodeKey).findFirst().orElse(null);
        }
        if (startKey == null && !nodeList.isEmpty()) {
            startKey = nodeList.get(0).getNodeKey();
        }

        // 已配置「表单内容（字段权限）」的节点集合：创建 / 归档节点必须配置，
        // 否则模拟运行不应判「走通」（此前只校验审批/提交的操作者，创建与归档即使全空也显示通过）。
        Set<String> fieldPermNodeKeys = new HashSet<>();
        for (WfNodeFieldPerm p : fieldPermMapper.selectList(
                Wrappers.<WfNodeFieldPerm>lambdaQuery().eq(WfNodeFieldPerm::getDefId, defId))) {
            if (p.getNodeKey() != null) {
                fieldPermNodeKeys.add(p.getNodeKey());
            }
        }

        Set<String> visitedNodes = new HashSet<>();
        int[] steps = {0};
        final int MAX_STEPS = 500;
        WalkCtx ctx = new WalkCtx(nodeMap, outLinks, formData, nodeStatus, nodeMsg, path, full,
            fieldPermNodeKeys);
        if (startKey != null) {
            walkNode(startKey, visitedNodes, steps, MAX_STEPS, ctx);
        }

        // 汇总逐节点结果（含未走到但存在的节点，统一标记未测试）
        boolean allPassed = true;
        int unreached = 0;
        int blocked = 0;
        for (WfProcessNode n : nodeList) {
            Integer st = nodeStatus.get(n.getNodeKey());
            if (st == null) {
                st = 0;
            }
            if (st != 1) {
                allPassed = false;
                if (st == 0) {
                    unreached++;
                } else if (st == 2) {
                    blocked++;
                }
            }
            SimulateResultVO.NodeSimResult nr = new SimulateResultVO.NodeSimResult();
            nr.setNodeKey(n.getNodeKey());
            nr.setNodeName(n.getNodeName());
            nr.setNodeType(n.getNodeType());
            nr.setStatus(st);
            if (st == 1) {
                nr.setMessage("走通");
            } else if (st == 2) {
                nr.setMessage(nodeMsg.getOrDefault(n.getNodeKey(), "流程走不通（连线可能画错或节点未配置）"));
            } else {
                nr.setMessage("未走到该节点（与起点不连通，可能连线缺失或画错）");
            }
            nodeResults.add(nr);
        }
        // 回写测试状态到库（已在 walkNode 内对走到节点标记，这里对未走到也统一落库）
        for (WfProcessNode n : nodeList) {
            Integer st = nodeStatus.getOrDefault(n.getNodeKey(), 0);
            if (!Objects.equals(n.getTestStatus(), st)) {
                n.setTestStatus(st);
                nodeMapper.updateById(n);
            }
        }
        result.setAllPassed(allPassed && !path.isEmpty());
        result.setNodes(nodeResults);
        result.setPath(path);
        if (allPassed && !path.isEmpty()) {
            result.setSummary("流程图走通：所有节点均从起点可达且能到达终点。");
        } else {
            StringBuilder sb = new StringBuilder("流程图未完全走通：");
            if (unreached > 0) {
                sb.append(unreached).append(" 个节点未连通（可能连线缺失/画错）；");
            }
            if (blocked > 0) {
                sb.append(blocked).append(" 个节点走不通（死路或节点未配置）；");
            }
            sb.append("请检查并修正连线或节点设置。");
            result.setSummary(sb.toString());
        }
        return result;
    }

    /** 走查上下文（避免方法参数过长） */
    private static class WalkCtx {
        final Map<String, WfProcessNode> nodeMap;
        final Map<String, List<WfNodeLink>> outLinks;
        final Map<String, Object> formData;
        final Map<String, Integer> nodeStatus;
        final Map<String, String> nodeMsg;
        final List<SimulateResultVO.PathStep> path;
        /** 无模拟数据时按「全量走查」：忽略条件、所有分支都走，验证可达性与死路 */
        final boolean full;
        /** 已配置「表单内容（字段权限）」的节点 key 集合（创建/归档节点据此校验） */
        final Set<String> fieldPermNodeKeys;

        WalkCtx(Map<String, WfProcessNode> nodeMap, Map<String, List<WfNodeLink>> outLinks,
                Map<String, Object> formData, Map<String, Integer> nodeStatus,
                Map<String, String> nodeMsg, List<SimulateResultVO.PathStep> path, boolean full,
                Set<String> fieldPermNodeKeys) {
            this.fieldPermNodeKeys = fieldPermNodeKeys;
            this.nodeMap = nodeMap;
            this.outLinks = outLinks;
            this.formData = formData;
            this.nodeStatus = nodeStatus;
            this.nodeMsg = nodeMsg;
            this.path = path;
            this.full = full;
        }
    }

    /** 递归走查单个节点：校验 + 选分支 + 记录路径 */
    private void walkNode(String nodeKey, Set<String> visitedNodes, int[] steps, int maxSteps, WalkCtx ctx) {
        if (nodeKey == null || steps[0] >= maxSteps) {
            return;
        }
        steps[0]++;
        WfProcessNode node = ctx.nodeMap.get(nodeKey);
        if (node == null) {
            return;
        }
        // 节点校验（仅校验一次）
        if (!ctx.nodeStatus.containsKey(nodeKey)) {
            String problem = validateNode(node, ctx.fieldPermNodeKeys);
            if (problem == null) {
                ctx.nodeStatus.put(nodeKey, 1);
            } else {
                ctx.nodeStatus.put(nodeKey, 2);
                ctx.nodeMsg.put(nodeKey, problem);
            }
        }
        // 归档节点：终点
        if (node.getNodeType() != null && node.getNodeType() == 3) {
            return;
        }
        List<WfNodeLink> outs = ctx.outLinks.getOrDefault(nodeKey, Collections.emptyList());
        // 全量走查（无模拟数据）：忽略条件，所有分支都走，用于验证流程图是否连通、有无死路。
        // 否则：按条件选分支（无条件分支 或 条件求值为真的分支）。
        List<WfNodeLink> taken;
        if (ctx.full) {
            taken = outs;
        } else {
            List<WfNodeLink> matched = new ArrayList<>();
            List<WfNodeLink> noCond = new ArrayList<>();
            for (WfNodeLink l : outs) {
                boolean blank = l.getConditionExpr() == null || l.getConditionExpr().isBlank();
                if (blank) {
                    noCond.add(l);
                } else if (evalCondition(l.getConditionExpr(), ctx.formData)) {
                    matched.add(l);
                }
            }
            taken = matched.isEmpty() ? noCond : matched;
        }
        if (taken.isEmpty()) {
            // 非归档节点却无后续出口 → 死路（线可能画错，或漏连出口）
            if (node.getNodeType() != null && node.getNodeType() != 3) {
                ctx.nodeStatus.put(nodeKey, 2);
                ctx.nodeMsg.put(nodeKey, "非归档节点却无后续出口，流程走不通（连线可能画错或漏连）");
            }
            return;
        }
        for (WfNodeLink l : taken) {
            SimulateResultVO.PathStep step = new SimulateResultVO.PathStep();
            step.setFromNodeKey(l.getFromNodeKey());
            step.setToNodeKey(l.getToNodeKey());
            step.setConditionCn(l.getConditionCn());
            ctx.path.add(step);
            String next = l.getToNodeKey();
            if (!visitedNodes.contains(next)) {
                visitedNodes.add(next);
                walkNode(next, visitedNodes, steps, maxSteps, ctx);
            }
        }
    }

    /**
     * 节点配置校验：返回问题描述（null = 通过）。
     *
     * <p>校验口径（与「流程测试」的真引擎校验保持一致）：
     * <ul>
     *   <li>网关(7)：系统节点，免校验；</li>
     *   <li>创建(0)：必须有<b>操作者</b>（否则无人可发起）+ 必须有<b>表单内容</b>（字段权限）；</li>
     *   <li>归档(3)：必须有<b>操作者</b> + 必须有<b>表单内容</b>（字段权限）；</li>
     *   <li>审批(1)/提交(2)：必须有操作者（原有）；</li>
     *   <li>其余（等待 5 / 自动处理 6 等）：免校验。</li>
     * </ul>
     * ⚠️ 此前只校验 1/2 的操作者，导致创建 / 归档节点即使「操作者与表单内容全空」也显示「走通」。</p>
     */
    private String validateNode(WfProcessNode node, Set<String> fieldPermNodeKeys) {
        Integer t = node.getNodeType();
        if (t == null || t == 7) {
            return null;
        }
        // ① 操作者
        if (t == 0) {
            if (!hasOperator(node)) {
                return "创建节点未设置操作者：无人可发起该流程（请在「节点信息-操作者」配置）";
            }
        } else if (t == 3) {
            if (!hasOperator(node)) {
                return "归档节点未设置操作者（请在「节点信息-操作者」配置）";
            }
        } else if (t == 1 || t == 2) {
            if (!hasOperator(node)) {
                return "人工节点未配置操作者，无法提交/审批（请在节点信息补充操作者）";
            }
        }
        // ② 表单内容：会渲染表单的节点（创建 0 / 审批 1 / 提交 2 / 归档 3）必须设置「节点布局」。
        //    本系统已屏蔽「普通模式」，故「是否设置了表单内容」= ext_json.settings.formContent.mode 是否为 custom；
        //    未设置（含存量的 normal）一律视为未配置，测试不通过。
        if (t >= 0 && t <= 3 && !isLayoutMode(node)) {
            return "节点未设置表单内容，请在「节点信息-表单内容」选择「节点布局」后再测试";
        }
        // 注：不再把「字段权限」当作「表单内容」的判据 —— 表单内容统一由上面的
        // ext_json.settings.formContent.mode 判定（节点布局），字段权限是独立维度，不在此拦截。
        return null;
    }

    /** 节点「表单内容」是否已设置为「节点布局」（ext_json.settings.formContent.mode = custom） */
    private boolean isLayoutMode(WfProcessNode node) {
        return "custom".equals(WfNodeSettingsUtil.str(node.getExtJson(), "formContent", "mode"));
    }

    /** 节点是否已配置操作者 */
    private boolean hasOperator(WfProcessNode node) {
        Long count = operatorMapper.selectCount(Wrappers.<WfNodeOperator>lambdaQuery()
            .eq(WfNodeOperator::getNodeId, node.getId()));
        return count != null && count > 0;
    }

    @Override
    public void saveNodeTestStatus(Long defId, String nodeKey, int status) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        if (node == null) {
            throw new ServiceException("节点不存在: " + nodeKey);
        }
        node.setTestStatus(status);
        nodeMapper.updateById(node);
    }

    /**
     * 求条件表达式（格式 {@code ${field op value && ...}}）在给定模拟表单数据下是否为真。
     * 字段 key 形如 {@code main.xxx} / {@code dt1.xxx}，查找时优先精确匹配，其次按字段名后缀匹配。
     */
    private boolean evalCondition(String expr, Map<String, Object> formData) {
        return WfConditionUtil.eval(expr, formData);
    }

    /** 出口解析 / 条件注入共用的有向边（目标 + 该边条件表达式） */
    private static class SeqEdge {
        final String to;
        final String cond;
        SeqEdge(String to, String cond) {
            this.to = to;
            this.cond = cond;
        }
    }

}
