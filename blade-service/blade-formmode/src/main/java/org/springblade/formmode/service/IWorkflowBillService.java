package org.springblade.formmode.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.springblade.formmode.entity.WorkflowBill;

import java.util.Map;

/**
 * 表单定义服务接口
 */
public interface IWorkflowBillService extends IService<WorkflowBill> {

    /**
     * 表单删除前检查：流程绑定情况 + 关联数据统计。
     *
     * <p>返回结构：</p>
     * <ul>
     *   <li>formId / formName / tableName：表单基础信息</li>
     *   <li>fieldCount / layoutCount / extendCount / optionCount：字段定义、布局、扩展属性、选项数量</li>
     *   <li>tableExists / dataCount：动态数据表是否存在、已沉淀业务数据行数</li>
     *   <li>bound / definitionCount / instanceCount / definitionNames：流程设计绑定与流程实例占用情况</li>
     *   <li>checkFailed / failReason：绑定校验是否失败（流程服务不可用）</li>
     *   <li>message：可直接展示给用户的提示语</li>
     * </ul>
     *
     * @param id 表单ID
     * @return 检查结果
     */
    Map<String, Object> deleteCheck(Long id);

    /**
     * 删除表单并清理全部关联数据。
     *
     * <p>清理范围：动态主表/明细表/历史表（物理 DROP）、字段定义（workflow_billfield）、
     * 表单布局（form_layout）、字段扩展属性（mode_form_field_extend）、
     * 字段选项（mode_form_field_option）、表单定义本身。</p>
     *
     * <p><b>拦截</b>：表单已被流程设计（wf_process_definition）绑定或已产生流程实例时拒绝删除，
     * 抛出 {@code ServiceException} 提示被哪个流程占用。</p>
     *
     * @param id 表单ID
     * @return 是否删除成功
     */
    boolean deleteForm(Long id);
}
