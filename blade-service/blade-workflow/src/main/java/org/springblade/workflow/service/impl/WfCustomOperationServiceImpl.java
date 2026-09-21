package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.dto.WfCustomOperationFull;
import org.springblade.workflow.entity.WfCustomOperation;
import org.springblade.workflow.entity.WfCustomOperationAction;
import org.springblade.workflow.entity.WfCustomOperationRight;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeDefaultSign;
import org.springblade.workflow.mapper.WfCustomOperationActionMapper;
import org.springblade.workflow.mapper.WfCustomOperationMapper;
import org.springblade.workflow.mapper.WfCustomOperationRightMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeDefaultSignMapper;
import org.springblade.workflow.service.IWfCustomOperationService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 节点「自定义操作」服务实现（对齐泛微节点信息「自定义操作」）。
 *
 * <p>按钮 + 动作明细（URL / 流程操作 / 接口，支持 {@code $field$} 占位符）+ 权限矩阵
 * （角色 / 部门 / 人员）。运行时 {@link #execute} 做字段占位符替换、权限校验与执行留痕。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfCustomOperationServiceImpl implements IWfCustomOperationService {

    private static final Pattern FIELD_PLACEHOLDER = Pattern.compile("\\$(\\w+)\\$");

    private final WfCustomOperationMapper opMapper;
    private final WfCustomOperationActionMapper actionMapper;
    private final WfCustomOperationRightMapper rightMapper;
    private final WfNodeDefaultSignMapper defaultSignMapper;
    private final IWfInstanceService instanceService;
    private final WfInstanceMapper instanceMapper;
    private final IFormmodeClient formmodeClient;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<WfCustomOperation> listEnabled(Long defId, String nodeKey) {
        return opMapper.selectList(Wrappers.<WfCustomOperation>lambdaQuery()
            .eq(WfCustomOperation::getDefId, defId)
            .eq(WfCustomOperation::getNodeKey, nodeKey)
            .eq(WfCustomOperation::getEnabled, 1)
            .orderByAsc(WfCustomOperation::getBtnOrder));
    }

    @Override
    public List<WfCustomOperationFull> listFull(Long defId, String nodeKey) {
        List<WfCustomOperation> ops = opMapper.selectList(Wrappers.<WfCustomOperation>lambdaQuery()
            .eq(WfCustomOperation::getDefId, defId)
            .eq(WfCustomOperation::getNodeKey, nodeKey)
            .orderByAsc(WfCustomOperation::getBtnOrder));
        List<WfCustomOperationFull> result = new ArrayList<>();
        for (WfCustomOperation op : ops) {
            result.add(toFull(op.getId()));
        }
        return result;
    }

    @Override
    public WfCustomOperationFull detail(Long opId) {
        return toFull(opId);
    }

    private WfCustomOperationFull toFull(Long opId) {
        WfCustomOperationFull full = new WfCustomOperationFull();
        full.setOp(opMapper.selectById(opId));
        full.setAction(actionMapper.selectOne(Wrappers.<WfCustomOperationAction>lambdaQuery()
            .eq(WfCustomOperationAction::getOpId, opId)));
        full.setRights(rightMapper.selectList(Wrappers.<WfCustomOperationRight>lambdaQuery()
            .eq(WfCustomOperationRight::getOpId, opId)));
        return full;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long defId, String nodeKey, List<WfCustomOperationFull> payload) {
        // 清旧：先删本节点全部按钮关联的动作 / 权限 / 按钮本体
        List<WfCustomOperation> oldOps = opMapper.selectList(Wrappers.<WfCustomOperation>lambdaQuery()
            .eq(WfCustomOperation::getDefId, defId)
            .eq(WfCustomOperation::getNodeKey, nodeKey));
        for (WfCustomOperation old : oldOps) {
            actionMapper.delete(Wrappers.<WfCustomOperationAction>lambdaQuery()
                .eq(WfCustomOperationAction::getOpId, old.getId()));
            rightMapper.delete(Wrappers.<WfCustomOperationRight>lambdaQuery()
                .eq(WfCustomOperationRight::getOpId, old.getId()));
        }
        opMapper.delete(Wrappers.<WfCustomOperation>lambdaQuery()
            .eq(WfCustomOperation::getDefId, defId)
            .eq(WfCustomOperation::getNodeKey, nodeKey));

        if (payload == null) {
            return;
        }
        int order = 0;
        for (WfCustomOperationFull one : payload) {
            WfCustomOperation op = one.getOp();
            if (op == null || op.getBtnName() == null || op.getBtnName().isBlank()) {
                continue;
            }
            op.setId(null);
            op.setDefId(defId);
            op.setNodeKey(nodeKey);
            if (op.getBtnOrder() == null) {
                op.setBtnOrder(order++);
            } else {
                order = op.getBtnOrder() + 1;
            }
            if (op.getEnabled() == null) {
                op.setEnabled(1);
            }
            if (op.getActionType() == null) {
                op.setActionType(1);
            }
            opMapper.insert(op);
            Long opId = op.getId();

            WfCustomOperationAction action = one.getAction();
            if (action != null) {
                action.setId(null);
                action.setOpId(opId);
                actionMapper.insert(action);
            }
            List<WfCustomOperationRight> rights = one.getRights();
            if (rights != null) {
                for (WfCustomOperationRight r : rights) {
                    if (r == null || r.getRightType() == null || r.getRightType().isBlank()
                        || r.getRightValue() == null || r.getRightValue().isBlank()) {
                        continue;
                    }
                    r.setId(null);
                    r.setOpId(opId);
                    rightMapper.insert(r);
                }
            }
        }
    }

    @Override
    public R<Boolean> execute(Long opId, Long instId, Long operator) {
        if (operator == null) {
            operator = SecureUtil.getUserId();
        }
        WfCustomOperation op = opMapper.selectById(opId);
        if (op == null) {
            return R.fail("自定义操作不存在或已删除");
        }
        WfCustomOperationAction action = actionMapper.selectOne(Wrappers.<WfCustomOperationAction>lambdaQuery()
            .eq(WfCustomOperationAction::getOpId, opId));
        List<WfCustomOperationRight> rights = rightMapper.selectList(Wrappers.<WfCustomOperationRight>lambdaQuery()
            .eq(WfCustomOperationRight::getOpId, opId));

        // 权限校验（人员精确 / 角色按当前登录角色；部门 / 接口类后续扩展）
        if (!rights.isEmpty() && !hasRight(rights, operator)) {
            return R.fail("无权限执行该自定义操作");
        }

        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return R.fail("流程实例不存在");
        }

        // 表单字段占位符上下文
        Map<String, Object> fieldValues = loadFieldValues(inst.getDataId());

        String opinion = action != null && action.getOpinion() != null ? action.getOpinion() : op.getBtnName();
        try {
            if (op.getActionType() == 1 && action != null && action.getUrl() != null) {
                String url = substitute(action.getUrl(), fieldValues);
                String body = action.getParamExpr() != null ? substitute(action.getParamExpr(), fieldValues) : null;
                String method = action.getHttpMethod() != null ? action.getHttpMethod() : "POST";
                executeHttp(method, url, body);
            } else if (op.getActionType() == 2) {
                // 流程操作：当前以留痕形式记录（具体动作映射为后续扩展点）
                log.info("[blade-workflow] 自定义操作[流程操作] 执行. opId={}, flowOperation={}",
                    opId, action != null ? action.getFlowOperation() : null);
            } else if (op.getActionType() == 3) {
                // 接口：当前以留痕形式记录（具体接口调用为后续扩展点）
                log.info("[blade-workflow] 自定义操作[接口] 执行. opId={}, interfaceName={}",
                    opId, action != null ? action.getInterfaceName() : null);
            }
            instanceService.recordLog(instId, op.getNodeKey(), operator, "9", opinion);
            return R.data(true);
        } catch (Exception e) {
            log.warn("[blade-workflow] 自定义操作执行异常. opId={}", opId, e);
            return R.fail("执行失败：" + e.getMessage());
        }
    }

    private boolean hasRight(List<WfCustomOperationRight> rights, Long operator) {
        String roleName = SecureUtil.getUserRole();
        for (WfCustomOperationRight r : rights) {
            if ("person".equals(r.getRightType()) && operator != null) {
                for (String id : r.getRightValue().split(",")) {
                    if (id.trim().equals(String.valueOf(operator))) {
                        return true;
                    }
                }
            } else if ("role".equals(r.getRightType()) && roleName != null) {
                for (String name : r.getRightValue().split(",")) {
                    if (name.trim().equalsIgnoreCase(roleName.trim())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Map<String, Object> loadFieldValues(Long dataId) {
        if (dataId == null) {
            return Map.of();
        }
        try {
            R<FormDataVO> r = formmodeClient.getFormDataById(dataId);
            if (r != null && r.isSuccess() && r.getData() != null) {
                return r.getData().getFieldValues();
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 自定义操作读取表单字段失败 dataId={}", dataId, e);
        }
        return Map.of();
    }

    private String substitute(String expr, Map<String, Object> fieldValues) {
        if (expr == null) {
            return null;
        }
        Matcher m = FIELD_PLACEHOLDER.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object val = fieldValues.get(m.group(1));
            m.appendReplacement(sb, val == null ? "" : Matcher.quoteReplacement(String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void executeHttp(String method, String url, String body) {
        if ("GET".equalsIgnoreCase(method)) {
            restTemplate.getForObject(url, String.class);
        } else {
            restTemplate.postForObject(url, body, String.class);
        }
    }

    @Override
    public String defaultSign(Long defId, String nodeKey, String menuType) {
        WfNodeDefaultSign sign = defaultSignMapper.selectOne(Wrappers.<WfNodeDefaultSign>lambdaQuery()
            .eq(WfNodeDefaultSign::getDefId, defId)
            .eq(WfNodeDefaultSign::getNodeKey, nodeKey)
            .eq(WfNodeDefaultSign::getMenuType, menuType));
        return sign != null ? sign.getDefaultOpinion() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDefaultSigns(Long defId, String nodeKey, List<WfNodeDefaultSign> signs) {
        defaultSignMapper.delete(Wrappers.<WfNodeDefaultSign>lambdaQuery()
            .eq(WfNodeDefaultSign::getDefId, defId)
            .eq(WfNodeDefaultSign::getNodeKey, nodeKey));
        if (signs == null) {
            return;
        }
        for (WfNodeDefaultSign s : signs) {
            if (s == null || s.getMenuType() == null || s.getMenuType().isBlank()) {
                continue;
            }
            s.setId(null);
            s.setDefId(defId);
            s.setNodeKey(nodeKey);
            defaultSignMapper.insert(s);
        }
    }
}
