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
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.dto.FormSaveDTO;
import org.springblade.workflow.dto.ValidateDTO;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.exception.WfAccessDeniedException;
import org.springblade.workflow.service.IWfFormRenderService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfPermService;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springblade.workflow.vo.FormRenderVO;
import org.springblade.workflow.vo.InstanceFreshVO;
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
    /** 记录级鉴权复用实例服务的口径（发起人 / 参与人 / 管理员） */
    private final IWfInstanceService instanceService;
    /** 保存时按 defId 回退表单ID */
    private final WfProcessDefinitionMapper defMapper;

    @Override
    public FormRenderVO render(Long instanceId, Long taskId, String nodeKeyParam) {
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：渲染包会带出单据全部字段值与布局，只有发起人、参与人
        // （办理人/被抄送/被传阅）或流程管理员能取。控制器已放开为「登录即可」
        // （发起页/办理页/已办查看都要用），这一层是防「猜 instanceId 读单据」的关键。
        if (!instanceService.canView(instanceId)) {
            log.warn("[blade-workflow] 越权拦截：渲染包非参与人. instId={}, starter={}, current={}",
                instanceId, inst.getStarter(), WfAuthUtil.userId());
            throw new WfAccessDeniedException("无权查看该流程表单：只有流程发起人、参与人（办理人/抄送人）或流程管理员可以查看");
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
        vo.setDefId(inst.getDefId());
        vo.setTaskId(taskId);
        vo.setNodeKey(nodeKey);
        vo.setFormId(inst.getFormId());
        vo.setDataId(inst.getDataId());
        vo.setInstanceStatus(inst.getStatus());
        vo.setFieldPerms(permService.getFieldPerm(inst.getDefId(), nodeKey));
        vo.setDetailPerms(permService.getDetailPerm(inst.getDefId(), nodeKey));
        vo.setDetailFilters(permService.getDetailFilter(inst.getDefId(), nodeKey, 1));
        vo.setReadonly(!canOperate(task));

        // 节点信息 → 运行时消费：操作菜单（可用操作）与签字意见必填
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, inst.getDefId())
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        vo.setAllowMenus(WfNodeSettingsUtil.operateMenus(node));
        fillSignOpinion(vo, node);
        // 节点信息 → 运行时消费：打印内容设置（打印模板页签的「打印内容设置」）
        vo.setPrintSet(WfNodeSettingsUtil.printSet(node));
        // 节点信息 → 运行时消费：签字意见显示设置（节点意见页签的「意见显示设置」）
        vo.setOpinionDisplay(WfNodeSettingsUtil.opinionDisplay(node));

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

    /** 把节点「签字意见设置」三组（输入/显示/反馈）填充进渲染包 */
    private void fillSignOpinion(FormRenderVO vo, WfProcessNode node) {
        vo.setOpinionRequired(WfNodeSettingsUtil.opinionRequired(node));
        vo.setOpinionMustInput(WfNodeSettingsUtil.signOpinionMustInput(node));
        vo.setOpinionMustInputOperations(WfNodeSettingsUtil.signOpinionMustInputOperations(node));
        vo.setOpinionHideInput(WfNodeSettingsUtil.signOpinionHideInput(node));
        vo.setOpinionHideArea(WfNodeSettingsUtil.signOpinionHideArea(node));
        vo.setOpinionViewMode(WfNodeSettingsUtil.signOpinionViewMode(node));
        vo.setOpinionViewNodeKeys(WfNodeSettingsUtil.signOpinionViewNodeKeys(node));
        vo.setOpinionNotSeeEachOther(WfNodeSettingsUtil.signOpinionNotSeeEachOther(node));
        vo.setOpinionFeedback(WfNodeSettingsUtil.signOpinionFeedback(node));
        vo.setOpinionNullNotFeedback(WfNodeSettingsUtil.signOpinionNullNotFeedback(node));
    }

    @Override
    public FormRenderVO preview(Long defId, Long formId, String nodeKey) {
        if (defId == null && formId == null) {
            throw new ServiceException("无法确定预览所用的流程定义或表单");
        }
        FormRenderVO vo = new FormRenderVO();
        vo.setDefId(defId);
        vo.setFormId(formId);
        vo.setNodeKey(nodeKey);
        vo.setReadonly(true);

        // 节点信息（操作菜单 / 签字意见必填）：依赖定义 + 节点
        if (defId != null && nodeKey != null && !nodeKey.isEmpty()) {
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
            vo.setAllowMenus(WfNodeSettingsUtil.operateMenus(node));
            fillSignOpinion(vo, node);
            vo.setPrintSet(WfNodeSettingsUtil.printSet(node));
            vo.setOpinionDisplay(WfNodeSettingsUtil.opinionDisplay(node));
            vo.setFieldPerms(permService.getFieldPerm(defId, nodeKey));
            vo.setDetailPerms(permService.getDetailPerm(defId, nodeKey));
            vo.setDetailFilters(permService.getDetailFilter(defId, nodeKey, 1));
        }

        // 布局：跨服务读取（与 render 同口径，按节点取布局：优先节点级，回退表单级）
        if (formId != null && nodeKey != null && !nodeKey.isEmpty()) {
            try {
                R<org.springblade.formmode.vo.FormLayoutVO> layoutResult =
                    formmodeClient.getFormLayout(formId, 0, nodeKey);
                if (layoutResult != null && layoutResult.isSuccess() && layoutResult.getData() != null) {
                    vo.setLayoutId(layoutResult.getData().getId());
                    vo.setLayoutJson(layoutResult.getData().getLayoutJson());
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 预览读取表单布局失败. formId={}, nodeKey={}", formId, nodeKey, e);
            }
        }
        return vo;
    }

    @Override
    public String save(FormSaveDTO dto) {
        if (dto == null) {
            throw new ServiceException("保存参数不能为空");
        }
        Long formId = dto.getFormId();
        if (formId == null && dto.getDefId() != null) {
            WfProcessDefinition def = defMapper.selectById(dto.getDefId());
            formId = def == null ? null : def.getFormId();
        }
        if (formId == null) {
            throw new ServiceException("无法确定保存所用的表单（formId 与 defId 都拿不到表单）");
        }
        // 办理态：记录级鉴权（与 render / validate 同口径），防止保存别人的单据
        if (dto.getInstanceId() != null && !instanceService.canView(dto.getInstanceId())) {
            throw new WfAccessDeniedException("无权保存该流程表单：只有流程发起人、参与人（办理人/抄送人）或流程管理员可以操作");
        }

        // ① 过期页面守卫（与「页面新鲜度复检」同一套规则，单一权威实现见 IWfInstanceService#fresh）：
        //    流程被退回、被他人流转、已归档/撤回后，浏览器里未刷新的页面仍显示原节点，
        //    若继续保存会把过期数据写回业务行，并污染「原节点」的表单快照。
        //    故先复检：stale=true 直接拒绝，并把过期原因（已成人话）透出给前端展示。
        if (dto.getInstanceId() != null) {
            InstanceFreshVO fresh = instanceService.fresh(dto.getInstanceId(), dto.getNodeKey(), null);
            if (Boolean.TRUE.equals(fresh.getStale())) {
                log.warn("[blade-workflow] 拒绝保存：页面已过期. instId={}, uiNodeKey={}, curNodeKey={}, reason={}",
                    dto.getInstanceId(), dto.getNodeKey(), fresh.getCurrentNodeKey(), fresh.getStaleReason());
                throw new ServiceException("本页已过期：" + fresh.getStaleReason());
            }
        }

        // ① 写业务数据行（跨服务 formmode：dataId 为空=新建，非空=更新同一行）
        FormDataSaveDTO saveDto = new FormDataSaveDTO();
        saveDto.setFormId(formId);
        saveDto.setDataId(dto.getDataId());
        saveDto.setFieldValues(dto.getFieldValues() == null ? Map.of() : dto.getFieldValues());
        R<Long> r = formmodeClient.saveBusinessData(saveDto);
        if (r == null || !r.isSuccess() || r.getData() == null) {
            throw new ServiceException("保存业务数据失败：" + (r == null ? "无响应" : r.getMsg()));
        }
        Long dataId = r.getData();

        // ② 办理态：同步当前节点快照（不改任务状态、不推进引擎）
        if (dto.getInstanceId() != null) {
            String nodeKey = (dto.getNodeKey() != null && !dto.getNodeKey().isEmpty())
                ? dto.getNodeKey() : currentNodeKey(dto.getInstanceId());
            upsertSnapshot(dto.getInstanceId(), nodeKey, dto.getFieldValues());
        }
        log.info("[blade-workflow] 表单已保存（未流转）. instId={}, formId={}, dataId={}",
            dto.getInstanceId(), formId, dataId);
        return String.valueOf(dataId);
    }

    /** 实例的当前节点Key（保存时未显式指定节点时用它定位快照） */
    private String currentNodeKey(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null || inst.getCurrentNodeKey() == null) {
            return "";
        }
        return inst.getCurrentNodeKey();
    }

    /** 快照 upsert：与 {@code snapshot(instId,nodeKey)} 的取数口径一致（同节点取最新一条） */
    private void upsertSnapshot(Long instId, String nodeKey, Map<String, Object> values) {
        if (nodeKey == null || nodeKey.isEmpty()) {
            return;
        }
        String json = JsonUtil.toJson(values == null ? Map.of() : values);
        WfFormSnapshot latest = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .eq(WfFormSnapshot::getNodeKey, nodeKey)
            .orderByDesc(WfFormSnapshot::getCreateTime)
            .last("LIMIT 1"));
        if (latest != null) {
            WfFormSnapshot patch = new WfFormSnapshot();
            patch.setId(latest.getId());
            patch.setDataJson(json);
            snapshotMapper.updateById(patch);
            return;
        }
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(instId);
        snap.setNodeKey(nodeKey);
        snap.setDataJson(json);
        snapshotMapper.insert(snap);
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
            // 记录级鉴权：按实例推断节点、必填矩阵时也要求能看到该实例
            if (!instanceService.canView(dto.getInstanceId())) {
                log.warn("[blade-workflow] 越权拦截：表单校验非参与人. instId={}, current={}",
                    dto.getInstanceId(), WfAuthUtil.userId());
                throw new WfAccessDeniedException("无权校验该流程表单：只有流程发起人、参与人（办理人/抄送人）或流程管理员可以操作");
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
            // 必填判定：优先用三维度的 required（权威值）；仅当它缺省时才回退到 perm 兼容列
            boolean required = perm.getRequired() != null
                ? perm.getRequired()
                : (perm.getPerm() != null && perm.getPerm() == PERM_REQUIRED);
            if (required) {
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
