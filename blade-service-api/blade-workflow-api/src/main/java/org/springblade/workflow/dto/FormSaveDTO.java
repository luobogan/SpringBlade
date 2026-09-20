package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 表单保存（只存不流转）入参。
 *
 * <p>对齐 ecology 节点「操作菜单 → 保存」：允许处理人先把表单内容存下来，
 * <b>不提交、不推进流程</b>。两种场景：</p>
 * <ul>
 *   <li><b>发起态</b>（{@code instanceId} 为空）：写/更新业务数据行，返回 dataId；
 *       之后真正发起时把该 dataId 回传（{@code StartProcessDTO.dataId}），
 *       流程即复用这条已保存的业务行，不会新建。</li>
 *   <li><b>办理态</b>（{@code instanceId} 非空）：写业务行 + 同步当前节点表单快照，
 *       不改任务状态、不推进引擎。</li>
 * </ul>
 *
 * <p>与 {@code POST /form/validate}（只校验不写）和
 * {@code POST /instance/start}（发起并推进）区分开。</p>
 */
@Data
@Schema(description = "表单保存（只存不流转）")
public class FormSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID（formId 为空时按其取表单）")
    private Long defId;

    @Schema(description = "表单ID（workflow_bill.id）")
    private Long formId;

    @Schema(description = "业务数据ID：为空=新建业务行；非空=更新该行")
    private Long dataId;

    @Schema(description = "流程实例ID：发起态为空；办理态必填")
    private Long instanceId;

    @Schema(description = "节点Key：为空取实例当前节点（用于定位快照）")
    private String nodeKey;

    @Schema(description = "表单字段值（主表 + 明细表，key 沿用布局坐标/字段命名）")
    private Map<String, Object> fieldValues;

}
