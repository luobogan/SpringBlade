package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.vo.FieldPermVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单权限控制器
 *
 * 对应 ecology 的 FormmodeRightController
 */
@RestController
@RequestMapping({"/api/blade-formmode/right", "/right"})
@Tag(name = "表单权限", description = "表单建模权限管理")
@RequiredArgsConstructor
public class FormRightController extends BladeController {

    private final IFormRightService formRightService;
    private final IDataScopeService dataScopeService;

    /**
     * 获取模块权限
     */
    @GetMapping("/mode/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeRightInfo>> getModeRights(@PathVariable Long modeId) {
        return R.data(formRightService.getModeRights(modeId));
    }

    /**
     * 保存模块权限
     */
    @PostMapping("/mode/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveModeRights(@PathVariable Long modeId, @RequestBody List<ModeRightInfo> rights) {
        return R.data(formRightService.saveModeRights(modeId, rights), "权限保存成功");
    }

    /**
     * 获取字段权限
     */
    @GetMapping("/field/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeFieldAuthorize>> getFieldAuthorizes(@PathVariable Long modeId) {
        return R.data(formRightService.getFieldAuthorizes(modeId));
    }

    /**
     * 保存字段权限
     */
    @PostMapping("/field/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveFieldAuthorizes(@PathVariable Long modeId, @RequestBody List<ModeFieldAuthorize> authorizes) {
        return R.data(formRightService.saveFieldAuthorizes(modeId, authorizes), "字段权限保存成功");
    }

    /**
     * 校验权限
     *
     * <p>修正：原先硬编码 0L 用户（任何人都按匿名用户鉴权，审批态权限形同虚设），
     * 改为从 {@link org.springblade.core.secure.utils.SecureUtil} 取真实当前用户。</p>
     */
    @GetMapping("/check/{modeId}/{rightType}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> checkRight(@PathVariable Long modeId, @PathVariable Integer rightType) {
        return R.data(formRightService.checkRight(modeId, rightType, SecureUtil.getUserId()));
    }

    /**
     * 查询「当前用户」在某模块下的<b>字段级权限</b>（运行期消费，非流程场景）。
     *
     * <p>与 {@code /field/{modeId}}（管理端查看授权配置）的区别：本端点按当前用户
     * <b>聚合计算</b>实际生效的字段三态，供渲染端直接消费。</p>
     *
     * <p>返回空列表 = 该模块未配置字段授权 → 调用方按字段定义默认渲染
     * （fail-open，与流程侧「无节点权限配置」的处理一致）。</p>
     *
     * <p>鉴权：仅要求已登录（{@link WorkflowConstant#HAS_AUTH}）——与
     * {@code checkRight} 的「未配置放行」语义配套，真正的字段三态由返回值驱动。</p>
     */
    @GetMapping("/field-perm/{modeId}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<List<FieldPermVO>> fieldPerm(@PathVariable Long modeId) {
        return R.data(formRightService.listFieldPerm(modeId));
    }

    /**
     * 按「表单ID」查询字段级权限（页面只有 formId 时使用）。
     *
     * <p>内部经 {@link IFormRightService#resolveModeIdByFormId} 反查模块ID；
     * 反查不到（表单未建模块 / billid 超 int 范围）时返回<b>空列表</b>，
     * 即「无授权配置」→ 调用方按字段定义默认渲染（fail-open），不阻断页面。</p>
     */
    @GetMapping("/field-perm/by-form/{formId}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<List<FieldPermVO>> fieldPermByForm(@PathVariable Long formId) {
        Long modeId = formRightService.resolveModeIdByFormId(formId);
        if (modeId == null) {
            return R.data(new ArrayList<>());
        }
        return R.data(formRightService.listFieldPerm(modeId));
    }

    /**
     * 获取数据范围规则（行级数据权限配置查看，对应 modedatasharerule）。
     */
    @GetMapping("/scope/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeDataShareRule>> getModeDataScopes(@PathVariable Long modeId) {
        return R.data(dataScopeService.listRules(modeId));
    }

    /**
     * 保存数据范围规则（全量替换）。
     */
    @PostMapping("/scope/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveModeDataScopes(@PathVariable Long modeId, @RequestBody List<ModeDataShareRule> rules) {
        return R.data(dataScopeService.saveRules(modeId, rules), "数据范围保存成功");
    }

    /**
     * 由「表单ID」反查「模块ID」（modeinfo.id）。
     *
     * <p>权限配置以 modeId 为键，但管理端多停留在表单维度，故提供桥接；
     * 反查不到返回 null（调用方据此提示「请先为该表单创建模块」）。</p>
     */
    @GetMapping("/resolve-mode/{formId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> resolveModeId(@PathVariable Long formId) {
        return R.data(formRightService.resolveModeIdByFormId(formId));
    }

}
