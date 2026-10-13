package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.service.IWfDefinitionPermNormalizeService;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.util.BpmnExtensionUtil.WfDetailPermExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfFieldPermExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfNodeExt;
import org.springblade.workflow.vo.PermNormalizeReportVO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 创建节点字段权限「归一」实现。
 *
 * <p><b>为什么单独成类</b>：与 {@code WfDefinitionRedeployServiceImpl} 同理——逐条调
 * {@link IWfDefinitionService#deploy}/{@link #testDeploy} 必须经 Spring 代理，其
 * {@code @Transactional} 才能逐条生效；本类自身不加事务，避免「一条失败回滚全部」。</p>
 *
 * <p><b>只读判定</b>：三维度（visible/editable/required）齐备时以它为准——「可见、不可编辑、
 * 非必填」即只读；缺三维度时回退 perm 兼容列 perm=1。必填（required=1）与隐藏（visible=0）
 * 一律不动，避免把「必填/隐藏」误归一成「可编辑」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfDefinitionPermNormalizeServiceImpl implements IWfDefinitionPermNormalizeService {

    /** 创建节点 nodeType（对齐 ecology NodeType：0=创建） */
    private static final String NODE_TYPE_CREATE = "0";
    /** 已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 测试态 */
    private static final int STATUS_TEST = 3;

    private static final BpmnXMLConverter CONVERTER = new BpmnXMLConverter();

    private final WfProcessDefinitionMapper defMapper;
    /** 经 Spring 代理调用，保证 deploy/testDeploy 的 @Transactional 逐条生效 */
    private final IWfDefinitionService definitionService;
    /** 归一旧表 wf_node_field_perm（双轨对账一致性；运行期已不读表，仅为数据卫生） */
    private final WfNodeFieldPermMapper fieldPermMapper;

    @Override
    public PermNormalizeReportVO normalizeCreateNodeFieldPerm(Long targetDefId) {
        PermNormalizeReportVO report = new PermNormalizeReportVO();

        List<WfProcessDefinition> defs = targetDefId != null
            ? defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
                .eq(WfProcessDefinition::getId, targetDefId)
                .isNotNull(WfProcessDefinition::getBpmnXml)
                .ne(WfProcessDefinition::getBpmnXml, ""))
            : defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
                .isNotNull(WfProcessDefinition::getBpmnXml)
                .ne(WfProcessDefinition::getBpmnXml, ""));
        report.setTotal(defs.size());

        for (WfProcessDefinition def : defs) {
            try {
                int changed = normalizeOne(def);
                if (changed <= 0) {
                    report.setUnchanged(report.getUnchanged() + 1);
                    continue;
                }
                report.setNormalized(report.getNormalized() + 1);
                report.setFieldsNormalized(report.getFieldsNormalized() + changed);

                // 按状态安全重部署：已发布→deploy（激活新版本）；测试态→testDeploy（不改状态）；
                // 草稿/其它→只归一源 BPMN，不部署（避免误发布），待其发布后自然生效。
                Integer status = def.getStatus();
                if (status != null && status == STATUS_PUBLISHED) {
                    definitionService.deploy(def.getId());
                    report.setDeployed(report.getDeployed() + 1);
                } else if (status != null && status == STATUS_TEST) {
                    definitionService.testDeploy(def.getId());
                    report.setDeployed(report.getDeployed() + 1);
                } else {
                    report.setSourceOnly(report.getSourceOnly() + 1);
                }
            } catch (Exception e) {
                report.setFailed(report.getFailed() + 1);
                report.getFailures().add("defId=" + def.getId() + ", procKey=" + def.getProcKey()
                    + " : " + e.getMessage());
                log.warn("[blade-workflow] 创建节点字段权限归一-单条失败. defId={}, procKey={}, {}",
                    def.getId(), def.getProcKey(), e.getMessage());
            }
        }

        report.setMessage("创建节点字段权限归一完成：扫描 " + report.getTotal()
            + "，归一 " + report.getNormalized() + "（字段 " + report.getFieldsNormalized() + " 条）"
            + "，无需处理 " + report.getUnchanged()
            + "，已部署 " + report.getDeployed()
            + "，仅清源未部署 " + report.getSourceOnly()
            + "，失败 " + report.getFailed());
        log.info("[blade-workflow] {}", report.getMessage());
        return report;
    }

    /**
     * 归一单条定义的创建节点字段权限，返回归一条目数（0=无需处理）。
     *
     * <p>有改动时：写回 {@code bpmn_xml} 源、同步旧表 {@code wf_node_field_perm}，由调用方按状态重部署。</p>
     */
    private int normalizeOne(WfProcessDefinition def) {
        BpmnModel model = parse(def.getBpmnXml(), def.getId());
        Process process = model.getMainProcess();
        if (process == null) {
            return 0;
        }

        int changed = 0;
        Set<String> createNodeKeys = new HashSet<>();
        // 创建节点既可能是 UserTask（新模型「申请人填单」），也可能是 StartEvent（旧模型「申请人填写」），
        // 故遍历全部 flow 元素、按 wf:node.nodeType=0 识别，而非只扫 UserTask。
        for (FlowElement fe : process.getFlowElements()) {
            WfNodeExt ext = BpmnExtensionUtil.readNode(fe);
            if (ext == null || !NODE_TYPE_CREATE.equals(ext.nodeType)) {
                continue;
            }
            boolean nodeChanged = false;
            for (WfFieldPermExt f : ext.fieldPerms) {
                if (isReadonly(f.perm, f.visible, f.editable, f.required)) {
                    // 只读 → 可编辑：perm=2 / visible=1 / editable=1 / required=0
                    f.perm = "2";
                    f.visible = "1";
                    f.editable = "1";
                    f.required = "0";
                    nodeChanged = true;
                    changed++;
                }
            }
            for (WfDetailPermExt d : ext.detailPerms) {
                if (isReadonly(d.perm, d.visible, d.editable, d.required)) {
                    d.perm = "2";
                    d.visible = "1";
                    d.editable = "1";
                    d.required = "0";
                    nodeChanged = true;
                    changed++;
                }
            }
            if (nodeChanged) {
                // writeNode 内部 ensureFirstExt 先清空旧 node 子元素再整体重写（读全了所有类别，无丢数据）
                BpmnExtensionUtil.writeNode(fe, ext);
                createNodeKeys.add(fe.getId());
            }
        }

        if (changed > 0) {
            String merged = new String(CONVERTER.convertToXML(model), StandardCharsets.UTF_8);
            def.setBpmnXml(merged);
            defMapper.updateById(def);
            // 旧表同步（双轨对账一致性）：运行期已不读表，仅为避免 reconcile 长期告警
            normalizeTableRows(def.getId(), createNodeKeys);
            log.info("[blade-workflow] 创建节点字段权限归一. defId={}, procKey={}, 归一 {} 条",
                def.getId(), def.getProcKey(), changed);
        }
        return changed;
    }

    /** 归一旧表 wf_node_field_perm 中创建节点的只读行 → 可编辑 */
    private void normalizeTableRows(Long defId, Set<String> createNodeKeys) {
        if (createNodeKeys.isEmpty()) {
            return;
        }
        List<WfNodeFieldPerm> rows = fieldPermMapper.selectList(Wrappers.<WfNodeFieldPerm>lambdaQuery()
            .eq(WfNodeFieldPerm::getDefId, defId)
            .in(WfNodeFieldPerm::getNodeKey, createNodeKeys));
        for (WfNodeFieldPerm row : rows) {
            Integer editable = row.getIsEditable();
            Integer visible = row.getIsVisible();
            Integer required = row.getIsRequired();
            Integer perm = row.getPerm();
            boolean readonly = (editable != null)
                ? (editable == 0 && (visible == null || visible == 1) && !Integer.valueOf(1).equals(required))
                : Integer.valueOf(1).equals(perm);
            if (readonly) {
                row.setPerm(2);
                row.setIsVisible(1);
                row.setIsEditable(1);
                row.setIsRequired(0);
                fieldPermMapper.updateById(row);
            }
        }
    }

    /** 只读判定：三维度齐备以它为准（可见+不可编辑+非必填），否则回退 perm=1 */
    private static boolean isReadonly(String perm, String visible, String editable, String required) {
        if (visible != null && editable != null) {
            return "1".equals(visible) && "0".equals(editable) && !"1".equals(required);
        }
        return "1".equals(perm);
    }

    private BpmnModel parse(String xml, Long defId) {
        try {
            return CONVERTER.convertToBpmnModel(
                () -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
        } catch (Exception e) {
            throw new RuntimeException("BPMN 解析失败 defId=" + defId + " : " + e.getMessage(), e);
        }
    }
}
