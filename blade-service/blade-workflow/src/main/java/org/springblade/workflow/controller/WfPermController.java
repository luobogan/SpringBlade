package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.DetailPermSaveDTO;
import org.springblade.workflow.dto.FieldPermSaveDTO;
import org.springblade.workflow.service.IWfPermService;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点权限控制器
 *
 * <p>对应文档 §6.2：节点字段权限矩阵与明细表权限的读写。
 * perm 语义：0隐藏 1只读 2可编辑 3必填（对齐 ecology fieldattr）。</p>
 */
@RestController
@RequestMapping("/definition")
// 角色门禁：流程管理员（workflow），与 formmode 侧保持同一角色
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "节点权限", description = "节点级字段权限矩阵与明细表权限")
public class WfPermController {

    private final IWfPermService permService;

    @GetMapping("/{id}/node/{nodeKey}/field-perm")
    @Operation(summary = "读取节点字段权限矩阵", description = "0隐藏 1只读 2可编辑 3必填")
    public R<List<FieldPermVO>> getFieldPerm(@PathVariable("id") Long id,
                                             @PathVariable("nodeKey") String nodeKey) {
        return R.data(permService.getFieldPerm(id, nodeKey));
    }

    @PutMapping("/{id}/node/{nodeKey}/field-perm")
    @Operation(summary = "保存节点字段权限矩阵", description = "整体覆盖")
    public R<Boolean> saveFieldPerm(@PathVariable("id") Long id,
                                    @PathVariable("nodeKey") String nodeKey,
                                    @RequestBody FieldPermSaveDTO dto) {
        dto.setNodeKey(nodeKey);
        return R.data(permService.saveFieldPerm(id, dto), "保存成功");
    }

    @GetMapping("/{id}/node/{nodeKey}/detail-perm")
    @Operation(summary = "读取节点明细表权限")
    public R<List<DetailPermVO>> getDetailPerm(@PathVariable("id") Long id,
                                               @PathVariable("nodeKey") String nodeKey) {
        return R.data(permService.getDetailPerm(id, nodeKey));
    }

    @PutMapping("/{id}/node/{nodeKey}/detail-perm")
    @Operation(summary = "保存节点明细表权限", description = "整体覆盖")
    public R<Boolean> saveDetailPerm(@PathVariable("id") Long id,
                                     @PathVariable("nodeKey") String nodeKey,
                                     @RequestBody DetailPermSaveDTO dto) {
        dto.setNodeKey(nodeKey);
        return R.data(permService.saveDetailPerm(id, dto), "保存成功");
    }

}
