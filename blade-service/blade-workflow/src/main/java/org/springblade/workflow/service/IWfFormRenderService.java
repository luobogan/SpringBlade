package org.springblade.workflow.service;

import org.springblade.workflow.dto.ValidateDTO;
import org.springblade.workflow.vo.FormRenderVO;

/**
 * 审批态渲染服务
 *
 * <p>对应文档 §6.4：{@code GET /form/render} 一次返回「布局 + 业务数据 + 节点字段权限 + 明细权限」，
 * 把表单侧（布局/字段/业务数据）与流程侧（权限）的耦合收敛在此接口内，
 * 避免前端多次往返；{@code POST /form/validate} 按节点必填矩阵做服务端复核。</p>
 */
public interface IWfFormRenderService {

    /**
     * 组装审批态渲染包
     *
     * @param instanceId 流程实例ID
     * @param taskId     任务ID（待办渲染时传入，用于判定读写态；为空则只读）
     * @param nodeKey    指定渲染的节点Key（测试页用于直显某节点布局；为空时取当前节点/任务节点）
     */
    FormRenderVO render(Long instanceId, Long taskId, String nodeKey);

    /**
     * 表单预览（无需实例）：按流程定义/表单/节点返回布局 + 字段权限 + 操作菜单，
     * 用于测试页/设计页直接打开流程表单查看，不创建任何测试实例。
     */
    FormRenderVO preview(Long defId, Long formId, String nodeKey);

    /**
     * 按节点必填矩阵做服务端校验（前端校验不可信）
     *
     * @return 校验通过返回 true；失败则抛出携带缺失字段信息的异常
     */
    boolean validate(ValidateDTO dto);

}
