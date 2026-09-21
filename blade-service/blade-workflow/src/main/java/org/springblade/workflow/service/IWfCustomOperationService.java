package org.springblade.workflow.service;

import org.springblade.core.tool.api.R;
import org.springblade.workflow.dto.WfCustomOperationFull;
import org.springblade.workflow.entity.WfCustomOperation;

import java.util.List;

/**
 * 节点「自定义操作」服务（按钮 + 动作明细 + 权限矩阵 + 运行时执行）。
 */
public interface IWfCustomOperationService {

    /** 运行时可见的启用按钮（按 btnOrder 升序），用于审批页渲染自定义操作菜单 */
    List<WfCustomOperation> listEnabled(Long defId, String nodeKey);

    /** 配置用：节点全部按钮（含停用）及其动作明细与权限矩阵，按 btnOrder 升序 */
    List<WfCustomOperationFull> listFull(Long defId, String nodeKey);

    /** 单条按钮完整配置 */
    WfCustomOperationFull detail(Long opId);

    /** 保存节点全部自定义操作（覆盖式：清旧写新，保证 op→action/right 外键一致） */
    void saveBatch(Long defId, String nodeKey, List<WfCustomOperationFull> payload);

    /** 运行时执行某自定义操作（URL/流程操作/接口 + $field$ 占位符替换 + 权限校验 + 留痕） */
    R<Boolean> execute(Long opId, Long instId, Long operator);

    /** 取节点某操作类型的默认签字意见（wf_node_default_sign） */
    String defaultSign(Long defId, String nodeKey, String menuType);

    /** 保存节点「按操作类型默认签字意见」（覆盖式） */
    void saveDefaultSigns(Long defId, String nodeKey, java.util.List<org.springblade.workflow.entity.WfNodeDefaultSign> signs);
}
