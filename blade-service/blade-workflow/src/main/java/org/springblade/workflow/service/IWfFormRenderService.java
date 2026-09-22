package org.springblade.workflow.service;

import org.springblade.workflow.dto.FormSaveDTO;
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
    default FormRenderVO render(Long instanceId, Long taskId, String nodeKey) {
        return render(instanceId, taskId, nodeKey, false);
    }

    /**
     * 同 {@link #render(Long, Long, String)}，额外声明「是否来自测试入口」。
     *
     * <p>用途（方案 §6.4 C8 + C12）：测试实例在<b>生产办理页必须拒绝渲染</b>，
     * 但<b>测试域（测试面板 / 真人模式）必须能渲染</b> —— 二者共用本接口，
     * 故由调用方显式声明：测试入口传 {@code true}，生产办理页不传。</p>
     *
     * @param testMode true = 测试入口（对 {@code is_test=1} 的实例放行渲染）
     */
    FormRenderVO render(Long instanceId, Long taskId, String nodeKey, boolean testMode);

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

    /**
     * 保存表单（<b>只存不流转</b>）：对齐 ecology「操作菜单 → 保存」。
     *
     * <p>发起态（无 instanceId）写/更新业务数据行并返回 dataId，之后发起带上该 dataId 即可复用；
     * 办理态（有 instanceId）写业务行 + 同步当前节点快照，不改任务状态、不推进引擎。</p>
     *
     * <p>⚠️ 不做必填校验：保存允许表单不完整（这正是「先存后提交」的意义）。</p>
     *
     * @param dto 保存入参
     * @return 业务数据ID（字符串形式，避免 19 位雪花 ID 在前端丢精度）
     */
    String save(FormSaveDTO dto);

}
