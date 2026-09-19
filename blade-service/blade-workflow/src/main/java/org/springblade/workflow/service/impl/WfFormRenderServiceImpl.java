package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.dto.ValidateDTO;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.service.IWfFormRenderService;
import org.springblade.workflow.service.IWfPermService;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springblade.workflow.vo.FormRenderVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审批态渲染服务实现
 *
 * <p>数据来源：</p>
 * <ul>
 *   <li>布局：跨服务读取 formmode（{@code GET /form-layout/{formId}}）</li>
 *   <li>业务数据：取本实例最新的 {@code wf_form_snapshot}（决策 3：数据与布局解耦，
 *       历史实例可原样重现，且避免渲染路径再依赖业务动态表）</li>
 *   <li>权限：本地 {@code wf_node_field_perm} / {@code wf_node_detail_perm}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfFormRenderServiceImpl implements IWfFormRenderService {

    /** 必填 */
    private static final int PERM_REQUIRED = 3;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfProcessNodeMapper nodeMapper;
    private final IWfPermService permService;
    private final IFormmodeClient formmodeClient;

    @Override
    public FormRenderVO render(Long instanceId, Long taskId, String nodeKeyParam) {
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }

        String nodeKey = inst.getCurrentNodeKey();
        WfTask task = null;
        if (taskId != null) {
            task = taskMapper.selectById(taskId);
            if (task != null && task.getNodeKey() != null && !task.getNodeKey().isEmpty()) {
                nodeKey = task.getNodeKey();
            }
        }
        // 显式指定节点（测试页直显某节点布局）优先级最高
        if (nodeKeyParam != null && !nodeKeyParam.isEmpty()) {
            nodeKey = nodeKeyParam;
        }

        FormRenderVO vo = new FormRenderVO();
        vo.setInstanceId(inst.getId());
        vo.setTaskId(taskId);
        vo.setNodeKey(nodeKey);
        vo.setFormId(inst.getFormId());
        vo.setDataId(inst.getDataId());
        vo.setInstanceStatus(inst.getStatus());
        vo.setFieldPerms(permService.getFieldPerm(inst.getDefId(), nodeKey));
        vo.setDetailPerms(permService.getDetailPerm(inst.getDefId(), nodeKey));
        vo.setReadonly(!canOperate(task));

        // 节点信息 → 运行时消费：操作菜单（可用操作）与签字意见必填
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, inst.getDefId())
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        vo.setAllowMenus(WfNodeSettingsUtil.operateMenus(node));
        vo.setOpinionRequired(WfNodeSettingsUtil.opinionRequired(node));

        // 布局：跨服务读取（按节点取布局：优先节点级，回退表单级；布局类型默认 0=编辑）
        try {
            R<org.springblade.formmode.vo.FormLayoutVO> layoutResult =
                formmodeClient.getFormLayout(inst.getFormId(), 0, nodeKey);
            if (layoutResult != null && layoutResult.isSuccess() && layoutResult.getData() != null) {
                vo.setLayoutId(layoutResult.getData().getId());
                vo.setLayoutJson(layoutResult.getData().getLayoutJson());
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 读取表单布局失败，渲染包将不含布局. formId={}", inst.getFormId(), e);
        }

        // 业务数据：取本实例最新快照
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, inst.getId())
            .orderByDesc(WfFormSnapshot::getCreateTime)
            .last("LIMIT 1"));
        if (snap != null && snap.getDataJson() != null) {
            try {
                vo.setDataJson(OBJECT_MAPPER.readValue(snap.getDataJson(),
                    new TypeReference<Map<String, Object>>() {
                    }));
            } catch (Exception e) {
                log.warn("[blade-workflow] 解析表单数据快照失败. instId={}", inst.getId(), e);
            }
        }
        return vo;
    }

    @Override
    public boolean validate(ValidateDTO dto) {
        if (dto == null || dto.getFormData() == null) {
            throw new ServiceException("待校验表单数据不能为空");
        }
        Long defId = dto.getDefId();
        String nodeKey = dto.getNodeKey();
        Long formId = null;

        // 未指定定义/节点时，按实例推断
        if ((defId == null || nodeKey == null || nodeKey.isEmpty()) && dto.getInstanceId() != null) {
            WfInstance inst = instanceMapper.selectById(dto.getInstanceId());
            if (inst == null) {
                throw new ServiceException("流程实例不存在");
            }
            if (defId == null) {
                defId = inst.getDefId();
            }
            if (nodeKey == null || nodeKey.isEmpty()) {
                nodeKey = inst.getCurrentNodeKey();
            }
            if (formId == null) {
                formId = inst.getFormId();
            }
        }
        if (defId == null || nodeKey == null || nodeKey.isEmpty()) {
            throw new ServiceException("无法确定校验所用的流程定义与节点");
        }

        // 1. 节点必填字段校验
        List<String> missing = new ArrayList<>();
        List<FieldPermVO> perms = permService.getFieldPerm(defId, nodeKey);
        for (FieldPermVO perm : perms) {
            if (perm.getPerm() != null && perm.getPerm() == PERM_REQUIRED) {
                if (isEmpty(dto.getFormData().get(perm.getFieldName()))) {
                    missing.add(perm.getFieldName());
                }
            }
        }

        // 2. 明细表「必须新增」校验
        List<DetailPermVO> details = permService.getDetailPerm(defId, nodeKey);
        for (DetailPermVO d : details) {
            if (d.getRequired() != null && d.getRequired() == 1) {
                String prefix = "dt" + d.getDtIndex();
                boolean hasRow = dto.getFormData().keySet().stream()
                    .anyMatch(k -> k != null && k.startsWith(prefix));
                if (!hasRow) {
                    missing.add("明细表" + d.getDtIndex() + "（必须至少一条）");
                }
            }
        }

        // 3. 布局级必填（权限矩阵为空时，必填定义在布局 fieldMeta）
        if (formId != null) {
            collectLayoutRequired(formId, nodeKey, dto.getFormData(), missing);
        }

        if (!missing.isEmpty()) {
            throw new ServiceException("以下字段为必填： " + String.join("、", missing));
        }
        return true;
    }

    /**
     * 是否可操作（有待办任务且办理人为当前用户）
     */
    private static boolean canOperate(WfTask task) {
        if (task == null) {
            return false;
        }
        if (!Integer.valueOf(WfTask.STATUS_TODO).equals(task.getStatus())) {
            return false;
        }
        Long current = SecureUtil.getUserId();
        return current != null && current.equals(task.getAssignee());
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.trim().isEmpty();
        }
        return false;
    }

    /**
     * 布局级必填校验：读取当前节点布局，解析每个 sheet 的 cellData，
     * 凡 fieldMeta.fieldAttr==3（必填）或 fieldMeta.required==true 的字段，
     * 校验 formData 是否已有值。
     */
    private void collectLayoutRequired(Long formId, String nodeKey, Map<String, Object> formData, List<String> missing) {
        try {
            R<org.springblade.formmode.vo.FormLayoutVO> layoutResult =
                formmodeClient.getFormLayout(formId, 0, nodeKey);
            if (layoutResult == null || !layoutResult.isSuccess() || layoutResult.getData() == null) {
                return;
            }
            String layoutJson = layoutResult.getData().getLayoutJson();
            if (layoutJson == null || layoutJson.isEmpty()) {
                return;
            }
            JsonNode root = OBJECT_MAPPER.readTree(layoutJson);
            List<JsonNode> sheets = new ArrayList<>();
            JsonNode sheetsNode = root.get("sheets");
            if (sheetsNode != null && sheetsNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = sheetsNode.fields();
                while (it.hasNext()) {
                    sheets.add(it.next().getValue());
                }
            } else if (sheetsNode != null && sheetsNode.isArray()) {
                sheetsNode.forEach(sheets::add);
            } else if (root.get("cellData") != null) {
                sheets.add(root);
            }
            Set<String> checked = new HashSet<>();
            for (JsonNode sheet : sheets) {
                String sheetId = sheetIdOf(sheet, root);
                JsonNode cellData = sheet.get("cellData");
                if (cellData == null || !cellData.isObject()) {
                    continue;
                }
                Iterator<Map.Entry<String, JsonNode>> rowIt = cellData.fields();
                while (rowIt.hasNext()) {
                    Map.Entry<String, JsonNode> rowEntry = rowIt.next();
                    JsonNode rowObj = rowEntry.getValue();
                    if (!rowObj.isObject()) {
                        continue;
                    }
                    Iterator<Map.Entry<String, JsonNode>> colIt = rowObj.fields();
                    while (colIt.hasNext()) {
                        Map.Entry<String, JsonNode> colEntry = colIt.next();
                        JsonNode cell = colEntry.getValue();
                        if (!cell.isObject()) {
                            continue;
                        }
                        JsonNode fieldMeta = cell.get("fieldMeta");
                        if (fieldMeta == null || !fieldMeta.isObject()) {
                            continue;
                        }
                        JsonNode cellType = fieldMeta.get("cellType");
                        if (cellType != null && "detailTableMarker".equals(cellType.asText())) {
                            continue;
                        }
                        boolean required = false;
                        JsonNode fieldAttr = fieldMeta.get("fieldAttr");
                        if (fieldAttr != null && fieldAttr.asInt(0) == 3) {
                            required = true;
                        }
                        JsonNode requiredNode = fieldMeta.get("required");
                        if (requiredNode != null && requiredNode.asBoolean(false)) {
                            required = true;
                        }
                        if (!required) {
                            continue;
                        }
                        JsonNode fieldNameNode = fieldMeta.get("fieldName");
                        String fieldName = fieldNameNode != null ? fieldNameNode.asText() : null;
                        if (fieldName == null || fieldName.isEmpty()) {
                            continue;
                        }
                        if (checked.contains(fieldName)) {
                            continue;
                        }
                        checked.add(fieldName);
                        boolean filled = false;
                        String cellKeyStr = sheetId + "__" + rowEntry.getKey() + "__" + colEntry.getKey();
                        if (!isEmpty(formData.get(cellKeyStr))) {
                            filled = true;
                        }
                        if (!filled && !isEmpty(formData.get(fieldName))) {
                            filled = true;
                        }
                        if (!filled) {
                            missing.add(fieldName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 布局必填校验解析失败，已降级跳过. formId={}, nodeKey={}", formId, nodeKey, e);
        }
    }

    private static String sheetIdOf(JsonNode sheet, JsonNode root) {
        JsonNode id = sheet.get("id");
        if (id != null && !id.asText().isEmpty()) {
            return id.asText();
        }
        JsonNode sheetName = root.get("sheetName");
        if (sheetName != null && !sheetName.asText().isEmpty()) {
            return sheetName.asText();
        }
        return "sheet1";
    }
}
