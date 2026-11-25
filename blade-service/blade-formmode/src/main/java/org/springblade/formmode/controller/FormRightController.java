package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.dto.FieldSubjectSaveDTO;
import org.springblade.formmode.dto.ModeAuthSchemeDTO;
import org.springblade.formmode.dto.ModeRightBatchDTO;
import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.service.IDataScopeService;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.vo.AuthSchemeVO;
import org.springblade.formmode.vo.FieldPermVO;
import org.springblade.formmode.vo.RuntimeFieldPermVO;
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
    private final org.springblade.formmode.service.IAuthSchemeService authSchemeService;

    @GetMapping("/schemes/{formId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<AuthSchemeVO>> listSchemes(@PathVariable Long formId) {
        return R.data(authSchemeService.listByForm(formId));
    }

    @GetMapping("/scheme/{schemeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<AuthSchemeVO> getScheme(@PathVariable Long schemeId) {
        return R.data(authSchemeService.get(schemeId));
    }

    @PostMapping("/scheme/save")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> saveScheme(@RequestBody ModeAuthSchemeDTO dto) {
        return R.data(authSchemeService.save(dto), "授权方案保存成功");
    }

    @PostMapping("/scheme/save-batch")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<Long>> saveSchemes(@RequestBody List<ModeAuthSchemeDTO> dtos) {
        return R.data(authSchemeService.saveBatch(dtos), "授权方案批量保存成功");
    }

    @PostMapping("/scheme/copy/{schemeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> copyScheme(@PathVariable Long schemeId) {
        return R.data(authSchemeService.copy(schemeId), "授权方案复制成功");
    }

    @DeleteMapping("/scheme/{schemeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> deleteScheme(@PathVariable Long schemeId) {
        return R.data(authSchemeService.delete(schemeId), "授权方案删除成功");
    }

    @PostMapping("/scheme/delete-batch")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Integer> deleteSchemes(@RequestBody List<Long> schemeIds) {
        return R.data(authSchemeService.deleteBatch(schemeIds), "授权方案批量删除成功");
    }

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

    /** 按授权主体查询跨模块字段权限（对象优先配置视图） */
    @GetMapping("/field/subject")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<List<ModeFieldAuthorize>> getFieldAuthorizesBySubject(@RequestParam Integer objtype,
                                                                   @RequestParam String objid) {
        long id;
        try { id = Long.parseLong(String.valueOf(objid).trim()); }
        catch (Exception e) { return R.fail("授权对象ID非法"); }
        return R.data(formRightService.getFieldAuthorizesBySubject(objtype, id));
    }

    /** 按授权主体批量保存字段权限（只影响该主体提交的模块/字段） */
    @PutMapping("/field/subject/batch")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveFieldAuthorizesBySubject(@RequestBody FieldSubjectSaveDTO batch) {
        return R.data(formRightService.saveFieldAuthorizesBySubject(batch), "字段权限按主体保存成功");
    }

    /** 查询表单数据菜单可见范围（管理端） */
    @GetMapping("/menu-shares/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<?> getFormMenuShares(@PathVariable Long modeId) {
        return R.data(formRightService.getFormMenuShares(modeId));
    }

    /** 查询字段权限授权对象组（管理端） */
    @GetMapping("/field/groups/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<?> getFieldAuthGroups(@PathVariable Long modeId) {
        return R.data(formRightService.getFieldAuthGroups(modeId));
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
        // 口径对齐：表单未建正式模块时，前端 FormRightDrawer 会以 formId 本身作为 modeId 保存
        // （resolve-mode 返回 null 的兜底）；读取侧必须同样回退，否则写进去的权限读不出来。
        if (modeId == null) {
            modeId = formId;
        }
        return R.data(formRightService.listFieldPerm(modeId));
    }

    /**
     * 运行期字段权限 + 当前用户命中组的 Univer 布局标识。
     *
     * <p>layoutType=0 编辑/新增，layoutType=1 显示/查看。</p>
     */
    @GetMapping("/runtime/field-perm/by-form/{formId}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<RuntimeFieldPermVO> runtimeFieldPermByForm(@PathVariable Long formId,
                                                        @RequestParam(required = false, defaultValue = "0") Integer layoutType) {
        return R.data(formRightService.getRuntimeFieldPermByForm(formId, layoutType));
    }

    /**
     * 查询当前用户对某表单拥有的模块操作权限码（1可见 2新建 3编辑 4删除 5详情）。
     *
     * <p>供通用列表（ScopeGrid）按建模权限驱动操作按钮；formId 反查不到模块时回退 formId 本身，
     * 与字段权限 by-form 端点同口径。失败返回空列表（前端按菜单按钮体系工作）。</p>
     */
    @GetMapping("/user-right-types/{formId}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<?> userRightTypes(@PathVariable Long formId) {
        Long modeId = formRightService.resolveModeIdByFormId(formId);
        if (modeId == null) {
            modeId = formId;
        }
        Long uid = SecureUtil.getUserId();
        try {
            return R.data(formRightService.listUserRightTypes(modeId, uid));
        } catch (Exception e) {
            return R.data(new java.util.ArrayList<Integer>());
        }
    }

    /**
     * 获取数据范围规则（行级数据权限配置查看，对应 modedatasharerule）。
     */
    @GetMapping("/form-manage-permission")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    public R<?> formManagePermission() {
        return R.data(formRightService.getFormManagePermission(SecureUtil.getUserId()));
    }

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
     * 一次性保存模块权限 + 字段权限 + 数据范围（三表同一事务）。
     *
     * <p>替代前端此前 Promise.all 并发调三个独立接口的做法，避免跨表不一致中间态。</p>
     */
    @PostMapping("/all/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> saveAll(@PathVariable Long modeId, @RequestBody ModeRightBatchDTO batch) {
        return R.data(formRightService.saveAll(modeId, batch), "权限配置已保存");
    }

    /** 读取权限配置当前版本号（乐观锁；前端加载时取得，保存时带回） */
    @GetMapping("/version/{modeId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Long> rightVersion(@PathVariable Long modeId) {
        return R.data(formRightService.getRightVersion(modeId));
    }

    /** 把某条业务数据行共享给主体（对标 E9 单条分享） */
    @PostMapping("/share/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> shareDataRow(@PathVariable Long modeId, @PathVariable Long dataId,
                                   @RequestParam Integer objtype, @RequestParam Long objid,
                                   @RequestParam(defaultValue = "1") Integer shareLevel,
                                   @RequestParam(required = false) String conditionField,
                                   @RequestParam(required = false) String matrixValueField) {
        return R.data(formRightService.shareDataRow(modeId, dataId, objtype, objid, shareLevel,
            conditionField, matrixValueField));
    }

    /** 取消某条数据行的主体共享 */
    @PostMapping("/unshare/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<Boolean> unshareDataRow(@PathVariable Long modeId, @PathVariable Long dataId,
                                     @RequestParam Integer objtype, @RequestParam Long objid) {
        return R.data(formRightService.unshareDataRow(modeId, dataId, objtype, objid));
    }

    /** 查询某条数据行的共享授权列表 */
    @GetMapping("/share/{modeId}/{dataId}")
    @PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
    public R<?> listDataShares(@PathVariable Long modeId, @PathVariable Long dataId) {
        return R.data(formRightService.listDataShares(modeId, dataId));
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
        Long modeId = formRightService.resolveModeIdByFormId(formId);
        // 口径统一：运行期各端点（field-perm/by-form、user-right-types、pageFormDataByScope）
        // 在反查不到 modeinfo 时均以 formId 兜底；管理端抽屉也必须同口径，
        // 否则「表单没建模块 → 无法配置权限 → 运行期却按 formId 读权限」造成死锁。
        return R.data(modeId == null ? formId : modeId);
    }

}
