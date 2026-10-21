package org.springblade.formmode.service;

import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.vo.DataScope;
import org.springblade.formmode.vo.FieldPermVO;

import java.util.List;

/**
 * 表单权限服务接口
 *
 * 对应 ecology 的 ModeRightService
 */
public interface IFormRightService {

    /**
     * 检查用户是否有指定模块的指定权限
     */
    boolean checkRight(Long modeId, Integer rightType, Long userId);

    /**
     * 获取模块的权限配置
     */
    List<ModeRightInfo> getModeRights(Long modeId);

    /**
     * 保存模块权限
     */
    boolean saveModeRights(Long modeId, List<ModeRightInfo> rights);

    /**
     * 获取字段权限
     */
    List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId);

    /**
     * 保存字段权限
     */
    boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes);

    /**
     * 获取用户有权限的模块ID列表
     */
    List<Long> getUserVisibleModeIds(Long userId);

    /**
     * 由「表单ID」（{@code workflow_bill.id}）反查「模块ID」（{@code modeinfo.id}）。
     *
     * <p><b>必要性</b>：非流程端点（{@code /form-data/{formId}} 系列）入参是 formId，
     * 而 {@code moderightinfo} / {@code modefieldauthorize} 的 {@code modeid} 指向
     * {@code modeinfo.id}，二者不同源；不转换则模块权限查不到行。</p>
     *
     * <p><b>类型对齐（2026-10-20）</b>：{@code modeinfo.billid} 已由 V2026.10.20_003
     * 改 bigint、实体改 Long，雪花 formId 可直接反查（此前因 int 列「超范围即跳过」的
     * 兜底已移除）。</p>
     *
     * @return 模块ID；找不到返回 null
     */
    Long resolveModeIdByFormId(Long formId);

    /**
     * 查询「当前用户」在该模块下的<b>字段级权限</b>（非流程场景，对应 E9 {@code modefieldauthorize}）。
     *
     * <p>聚合规则：命中当前用户（objid=0 全部 / 角色 / 部门 / 人员；岗位暂不支持，见
     * {@code FormRightServiceImpl} 已知残缺）的多条授权，按字段取<b>最宽松</b>值
     * （可编辑 &gt; 可见）。</p>
     *
     * <p>维度映射：{@code rightvalue 1→可见}、{@code 2→可编辑}；<b>无必填维度</b>
     * （required 恒 null，前端据此回退字段定义的必填配置）。scope 固定 {@code main}。</p>
     *
     * <p><b>空列表语义</b>：该模块未配置任何字段授权 → 返回空列表，调用方按字段定义
     * （{@code workflow_billfield}）默认渲染，与流程侧「无节点权限配置」的处理一致。</p>
     *
     * @param modeId 模块ID（modeinfo.id）
     * @return 字段权限列表；无配置 / 查询异常返回空列表（不返回 null）
     */
    List<FieldPermVO> listFieldPerm(Long modeId);

    /**
     * 解析「当前用户」对某模块的<b>数据范围</b>（行级数据权限，对应 modedatasharerule）。
     *
     * @param modeId 模块ID（modeinfo.id）
     * @return 数据范围结果（canViewAll / creatorId）
     */
    DataScope resolveDataScope(Long modeId);

    /**
     * 判断某行数据（创建人）是否在当前用户数据范围内。
     *
     * @param modeId         模块ID
     * @param creatorIdOfRow 该数据行的创建人ID
     */
    boolean inDataScope(Long modeId, Long creatorIdOfRow);

}
