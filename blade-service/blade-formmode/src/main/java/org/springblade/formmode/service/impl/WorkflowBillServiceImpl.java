package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.mapper.FieldDefinitionMapper;
import org.springblade.formmode.mapper.FieldExtendMapper;
import org.springblade.formmode.mapper.FieldOptionMapper;
import org.springblade.formmode.mapper.FormLayoutMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IWorkflowBillService;
import org.springblade.workflow.feign.IWorkflowClient;
import org.springblade.workflow.vo.FormBindingVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 表单定义服务实现
 *
 * <p>除基础 CRUD 外，承载「表单删除」的级联清理与流程绑定校验：</p>
 * <ul>
 *   <li>删除前经 Feign 调 blade-workflow 校验表单是否被流程设计绑定（wf_process_definition.form_id）
 *       或已产生流程实例（wf_instance.form_id），被占用则拒绝删除并给出流程名称；
 *       流程服务不可用时同样拒绝（宁可拦截，不产生脏数据）。</li>
 *   <li>删除时清理：动态主表/明细表/历史表（物理 DROP）、字段定义、表单布局、
 *       字段扩展属性、字段选项，最后物理删除表单定义本身。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowBillServiceImpl extends ServiceImpl<WorkflowBillMapper, WorkflowBill> implements IWorkflowBillService {

    /**
     * 动态表名白名单：仅允许字母/数字/下划线，避免拼接 SQL 时被注入。
     */
    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    private final FieldDefinitionMapper fieldDefinitionMapper;
    private final FormLayoutMapper formLayoutMapper;
    private final FieldExtendMapper fieldExtendMapper;
    private final FieldOptionMapper fieldOptionMapper;
    private final IDynamicTableService dynamicTableService;
    private final IWorkflowClient workflowClient;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> deleteCheck(Long id) {
        WorkflowBill bill = getById(id);
        if (bill == null) {
            throw new ServiceException("表单不存在或已被删除");
        }

        String formId = String.valueOf(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formId", formId);
        result.put("formName", bill.getFormName());
        result.put("tableName", bill.getTableName());

        // ---- 关联配置数据统计 ----
        result.put("fieldCount", fieldDefinitionMapper.selectCount(
            new LambdaQueryWrapper<FieldDefinition>().eq(FieldDefinition::getBillId, formId)));
        result.put("layoutCount", formLayoutMapper.selectCount(
            new LambdaQueryWrapper<FormLayout>().eq(FormLayout::getFormId, id)));
        result.put("extendCount", fieldExtendMapper.selectCount(
            new LambdaQueryWrapper<FieldExtend>().eq(FieldExtend::getFormId, formId)));
        result.put("optionCount", fieldOptionMapper.selectCount(
            new LambdaQueryWrapper<FieldOption>().eq(FieldOption::getFormId, formId)));

        // ---- 动态数据表统计 ----
        String tableName = bill.getTableName();
        boolean tableExists = StringUtil.isNotBlank(tableName)
            && SAFE_TABLE_NAME.matcher(tableName).matches()
            && dynamicTableService.tableExists(tableName);
        result.put("tableExists", tableExists);
        result.put("dataCount", tableExists ? countRows(tableName) : 0L);

        // ---- 流程绑定校验（跨服务） ----
        FormBindingVO binding = fetchBinding(id);
        boolean checkFailed = Boolean.TRUE.equals(binding.getCheckFailed());
        boolean bound = Boolean.TRUE.equals(binding.getBound());
        result.put("checkFailed", checkFailed);
        result.put("bound", bound);
        result.put("definitionCount", binding.getDefinitionCount());
        result.put("instanceCount", binding.getInstanceCount());
        result.put("definitionNames", binding.getDefinitionNames());
        result.put("failReason", binding.getFailReason());
        result.put("message", buildCheckMessage(bill, binding));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteForm(Long id) {
        WorkflowBill bill = getById(id);
        if (bill == null) {
            throw new ServiceException("表单不存在或已被删除");
        }

        // 1. 绑定校验：被流程设计绑定 / 已产生流程实例 / 校验失败，一律拒绝删除
        FormBindingVO binding = fetchBinding(id);
        if (Boolean.TRUE.equals(binding.getCheckFailed())) {
            throw new ServiceException("无法校验表单绑定关系："
                + (StringUtil.isBlank(binding.getFailReason()) ? "流程服务不可用" : binding.getFailReason()));
        }
        if (Boolean.TRUE.equals(binding.getBound())) {
            throw new ServiceException(buildCheckMessage(bill, binding));
        }

        String formId = String.valueOf(id);

        // 2. 删除动态数据表（主表 + 明细表 + 历史表，含索引残留）
        dropDynamicTables(bill.getTableName());

        // 3. 清理关联配置数据
        int fieldCount = fieldDefinitionMapper.delete(
            new LambdaQueryWrapper<FieldDefinition>().eq(FieldDefinition::getBillId, formId));
        int layoutCount = formLayoutMapper.delete(
            new LambdaQueryWrapper<FormLayout>().eq(FormLayout::getFormId, id));
        int extendCount = fieldExtendMapper.delete(
            new LambdaQueryWrapper<FieldExtend>().eq(FieldExtend::getFormId, formId));
        int optionCount = fieldOptionMapper.delete(
            new LambdaQueryWrapper<FieldOption>().eq(FieldOption::getFormId, formId));

        // 4. 物理删除表单定义（实体带 @TableLogic，removeById 只做逻辑删除，故走原生 SQL）
        jdbcTemplate.update("DELETE FROM workflow_bill WHERE id = ?", id);

        log.info("[formmode] 表单已删除: id={}, name={}, table={}, 字段={}, 布局={}, 扩展={}, 选项={}",
            id, bill.getFormName(), bill.getTableName(), fieldCount, layoutCount, extendCount, optionCount);
        return true;
    }

    // ==================== 内部方法 ====================

    /**
     * 调用流程服务查询绑定情况；Feign 降级或异常时返回「校验失败」占位对象。
     */
    private FormBindingVO fetchBinding(Long formId) {
        try {
            R<FormBindingVO> result = workflowClient.getFormBinding(formId);
            if (result == null) {
                return bindingCheckFailed("流程服务无响应");
            }
            if (!result.isSuccess() || result.getData() == null) {
                return bindingCheckFailed(result.getMsg());
            }
            return result.getData();
        } catch (Exception e) {
            log.error("[formmode] 校验表单流程绑定失败: formId={}", formId, e);
            return bindingCheckFailed("调用流程服务异常：" + e.getMessage());
        }
    }

    private FormBindingVO bindingCheckFailed(String reason) {
        FormBindingVO vo = new FormBindingVO();
        vo.setCheckFailed(Boolean.TRUE);
        vo.setBound(Boolean.TRUE);
        vo.setFailReason(StringUtil.isBlank(reason) ? "流程服务不可用" : reason);
        return vo;
    }

    /**
     * 组装可直接展示的提示语。
     */
    private String buildCheckMessage(WorkflowBill bill, FormBindingVO binding) {
        String formName = StringUtil.isBlank(bill.getFormName()) ? String.valueOf(bill.getId()) : bill.getFormName();
        if (Boolean.TRUE.equals(binding.getCheckFailed())) {
            return "表单「" + formName + "」的流程绑定关系校验失败（"
                + binding.getFailReason() + "），为避免数据不一致已拒绝删除，请稍后重试。";
        }

        int defCount = binding.getDefinitionCount() == null ? 0 : binding.getDefinitionCount();
        int instCount = binding.getInstanceCount() == null ? 0 : binding.getInstanceCount();
        if (defCount == 0 && instCount == 0) {
            return "表单「" + formName + "」未被流程绑定，可以删除。";
        }

        StringBuilder sb = new StringBuilder("表单「").append(formName).append("」已被占用，无法删除：");
        if (defCount > 0) {
            List<String> names = binding.getDefinitionNames() == null
                ? new ArrayList<>() : binding.getDefinitionNames();
            sb.append("流程设计已绑定 ").append(defCount).append(" 个流程定义");
            if (!names.isEmpty()) {
                sb.append("（").append(String.join("、", names));
                if (defCount > names.size()) {
                    sb.append(" 等");
                }
                sb.append("）");
            }
        }
        if (instCount > 0) {
            if (defCount > 0) {
                sb.append("；");
            }
            sb.append("已产生 ").append(instCount).append(" 个流程实例");
        }
        sb.append("。请先在「流程设计」中对相关流程改绑其他表单或删除流程，再删除本表单。");
        return sb.toString();
    }

    /**
     * 删除表单对应的动态表：主表 + 以「主表名_」为前缀的明细表/历史表。
     *
     * <p>先按 detailtablecount 无法覆盖历史残留（手工删过明细表索引等），
     * 故统一按 information_schema 前缀匹配扫描后逐个 DROP。</p>
     */
    private void dropDynamicTables(String tableName) {
        if (StringUtil.isBlank(tableName)) {
            return;
        }
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            log.warn("[formmode] 表单表名不合法，跳过物理表清理: {}", tableName);
            return;
        }

        // 主表
        dynamicTableService.dropTable(tableName);

        // 明细表 / 历史表：formtable_main_1_dt1、formtable_main_1_history、formtable_main_1_dt1_history
        String escaped = tableName.replace("_", "\\_");
        List<String> related = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE ?",
            String.class, escaped + "\\_%");
        for (String table : related) {
            if (SAFE_TABLE_NAME.matcher(table).matches()) {
                dynamicTableService.dropTable(table);
            }
        }
        log.info("[formmode] 动态表已清理: 主表={}, 关联表={}", tableName, related);
    }

    private long countRows(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Long.class);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("[formmode] 统计动态表数据量失败: table={}", tableName, e);
            return 0L;
        }
    }
}
