package org.springblade.formmode.service;

import org.springblade.formmode.entity.ModeDataShareRule;
import org.springblade.formmode.vo.DataScope;

import java.util.List;

/**
 * 行级数据范围（共享规则）解析服务。
 *
 * <p><b>模型（精简够用子集）</b>：把模块的「查看」权限共享给 角色/部门/人员/岗位，
 * 命中者可见该模块<b>全部</b>数据；未命中者默认仅可见<b>本人创建</b>的数据
 * （creator 自阅，E9 默认行为）。矩阵/复杂条件表达式为后续增强。</p>
 *
 * <p><b>fail-open</b>：规则加载失败 → 按「仅 creator 自阅」兜底，不阻断读取。</p>
 */
public interface IDataScopeService {

    /**
     * 解析用户对某模块的数据范围。
     *
     * @param modeId  模块ID
     * @param userId  当前用户ID
     * @param deptId  当前用户主部门ID（可null）
     * @param roleIds 当前用户角色ID集合（可空）
     * @param jobIds  当前用户岗位ID集合（可空）
     */
    DataScope resolve(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds);

    /**
     * 判断某行数据是否在用户数据范围内（供详情/单条读取用）。
     *
     * @param creatorIdOfRow 该数据行的创建人ID（取 modedatacreator/modedatacreater）
     */
    boolean inScope(Long modeId, Long userId, Long deptId, List<Long> roleIds, List<Long> jobIds,
                    Long creatorIdOfRow);

    /**
     * 查询模块的数据范围规则（管理端配置查看，对应 modedatasharerule）。
     *
     * @param modeId 模块ID
     */
    List<ModeDataShareRule> listRules(Long modeId);

    /**
     * 保存模块的数据范围规则（全量替换：先按 modeid 删除，再逐条插入）。
     *
     * @param modeId 模块ID（规则行的 modeid 由服务端统一写入，忽略入参自带）
     * @param rules  规则列表
     */
    boolean saveRules(Long modeId, List<ModeDataShareRule> rules);
}
