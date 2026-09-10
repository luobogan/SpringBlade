package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.DefinitionSaveDTO;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfWorkflowType;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfWorkflowTypeMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormConditionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfDefinitionServiceImpl implements IWfDefinitionService {

    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final WfNodeLinkMapper linkMapper;
    private final WfNodeOperatorMapper operatorMapper;
    private final WfNodeFieldPermMapper fieldPermMapper;
    private final WfWorkflowTypeMapper wfWorkflowTypeMapper;
    private final IWfInstanceService instanceService;
    private final IProcessService processService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DefinitionSaveDTO dto) {
        if (dto == null || dto.getDefinition() == null) {
            throw new ServiceException("流程定义不能为空");
        }
        WfProcessDefinition def = dto.getDefinition();
        if (def.getId() == null) {
            def.setVersion(def.getVersion() == null ? 1 : def.getVersion());
            defMapper.insert(def);
        } else {
            defMapper.updateById(def);
        }

        Long defId = def.getId();
        // 节点 / 出口 / 操作者整体覆盖
        nodeMapper.delete(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
        linkMapper.delete(Wrappers.<WfNodeLink>lambdaQuery().eq(WfNodeLink::getDefId, defId));
        operatorMapper.delete(Wrappers.<WfNodeOperator>lambdaQuery()
            .eq(WfNodeOperator::getNodeId, defId));

        if (dto.getNodes() != null) {
            for (WfProcessNode node : dto.getNodes()) {
                node.setId(null);
                node.setDefId(defId);
                nodeMapper.insert(node);
            }
        }
        if (dto.getLinks() != null) {
            for (WfNodeLink link : dto.getLinks()) {
                link.setId(null);
                link.setDefId(defId);
                linkMapper.insert(link);
            }
        }
        if (dto.getOperators() != null) {
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
        return defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getFormId, formId)
            .orderByDesc(WfProcessDefinition::getVersion));
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
            processService.deployProcess(def.getProcKey(), def.getBpmnXml());
        } else {
            throw new ServiceException("尚无 BPMN 定义，请先在「流程画布」中设计并保存");
        }
        def.setStatus(1);
        defMapper.updateById(def);
        log.info("[blade-workflow] 流程定义已发布并部署到引擎. defId={}, procKey={}", defId, def.getProcKey());
        return true;
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
        Process process;
        try {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            var model = converter.convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, false);
            process = model.getMainProcess();
        } catch (Exception e) {
            throw new ServiceException("BPMN 解析失败：" + e.getMessage());
        }
        String procId = process.getId();
        if (procId != null && !procId.isBlank()) {
            // 以画布 process id 为准，保证引擎部署一致
            def.setProcKey(procId);
        }
        def.setBpmnXml(bpmnXml);
        defMapper.updateById(def);

        // 重建节点（整体覆盖）
        nodeMapper.delete(Wrappers.<WfProcessNode>lambdaQuery().eq(WfProcessNode::getDefId, defId));
        int sort = 1;
        for (FlowElement fe : new ArrayList<>(process.getFlowElements())) {
            if (fe instanceof UserTask) {
                UserTask ut = (UserTask) fe;
                WfProcessNode node = new WfProcessNode();
                node.setDefId(defId);
                node.setNodeKey(ut.getId());
                node.setNodeName(ut.getName() == null || ut.getName().isBlank() ? ut.getId() : ut.getName());
                node.setNodeType(1);      // 审批节点
                node.setSignOrder(0);     // 或签
                node.setSortOrder(sort++);
                nodeMapper.insert(node);
            }
        }
        log.info("[blade-workflow] BPMN 已保存并解析节点. defId={}, procKey={}, nodeCount={}",
            defId, def.getProcKey(), sort - 1);
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
        int newVersion = (old.getVersion() == null ? 1 : old.getVersion()) + 1;

        WfProcessDefinition neo = new WfProcessDefinition();
        neo.setProcKey(old.getProcKey());
        neo.setFormId(old.getFormId());
        neo.setName(old.getName());
        neo.setVersion(newVersion);
        neo.setIsFree(old.getIsFree());
        neo.setType(old.getType());
        neo.setFormType(old.getFormType());
        neo.setDescription(old.getDescription());
        neo.setSortOrder(old.getSortOrder());
        neo.setStatus(0);
        defMapper.insert(neo);

        // 复制节点（节点ID重新生成，与 ecology 版本模型一致）
        List<WfProcessNode> nodes = nodes(defId);
        for (WfProcessNode n : nodes) {
            n.setId(null);
            n.setDefId(neo.getId());
            nodeMapper.insert(n);
        }
        // 复制出口
        List<WfNodeLink> links = links(defId);
        for (WfNodeLink l : links) {
            l.setId(null);
            l.setDefId(neo.getId());
            linkMapper.insert(l);
        }
        // 复制节点字段权限
        List<WfNodeFieldPerm> perms = fieldPermMapper.selectList(
            Wrappers.<WfNodeFieldPerm>lambdaQuery().eq(WfNodeFieldPerm::getDefId, defId));
        for (WfNodeFieldPerm p : perms) {
            p.setId(null);
            p.setDefId(neo.getId());
            fieldPermMapper.insert(p);
        }
        return neo.getId();
    }

    @Override
    public boolean enable(Long defId, boolean enabled) {
        WfProcessDefinition def = defMapper.selectById(defId);
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        def.setStatus(enabled ? 1 : 2);
        defMapper.updateById(def);
        return true;
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

        // 5. 流程状态 —— 对齐 ecology getWorkflowStatusItem（0无效/1有效/2测试）
        FormFieldVO status = field("status", "流程状态", "select", false);
        status.setDefaultValue(0);
        status.setOptions(Arrays.asList(
            option(0, "草稿"),
            option(1, "已发布"),
            option(2, "停用")
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

}
