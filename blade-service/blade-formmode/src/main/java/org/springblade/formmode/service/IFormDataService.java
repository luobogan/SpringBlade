package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.vo.FormDataVO;

import java.util.List;
import java.util.Map;

/**
 * 表单数据管理服务接口
 *
 * 对应 ecology 的 ModeDataManager（核心数据管理器）
 */
public interface IFormDataService {

    /**
     * 保存表单数据（新建/编辑/删除）
     * 对应 ecology ModeDataManager.saveModeData()
     */
    Long saveFormData(FormDataDTO formDataDTO);

    /**
     * 按「表单ID」写入业务数据（不依赖 modeinfo 模块）。
     *
     * <p>表名取 {@code workflow_bill.table_name}（迁移表单的表名与表单ID不同，如
     * 表单 2064530495200337922 → {@code formtable_main_5}），列名用 fieldValues 的 key
     * （字段名），只写目标表真实存在的列。新增时返回新行的 id（即 {@code wf_instance.data_id}）。</p>
     */
    Long saveBusinessData(FormDataSaveDTO dto);

    /**
     * 获取单条数据详情
     */
    FormDataVO getFormDataById(Long modeId, Long dataId);

    /**
     * 分页查询数据
     */
    Page<Map<String, Object>> pageFormData(Long modeId, Page<?> page, Map<String, Object> params);

    /**
     * 按「数据权限规则」分页查询业务数据（数据权限 · 列表预览专用）。
     *
     * <p>与 {@link #pageFormData} 的差异：额外叠加 {@code blade_scope_data} 的
     * 列清单（scope_field）与行级规则（scope_column / scope_type / scope_value），
     * 语义与 {@code BladeDataScopeHandler#sqlCondition} 一致
     * （{@code select {scopeField} from (原SQL) scope where scope.{scopeColumn} in (...)}）。</p>
     *
     * <p>背景：{@code blade_scope_data} 的行级规则只在 MyBatis Mapper SQL 链路生效，
     * formmode 的动态 JDBC 查询走不到它；而「列表预览」必须展示
     * <b>当前数据权限配置下的真实数据</b>，故在此显式应用规则。</p>
     *
     * @param formId       表单ID（workflow_bill.id）
     * @param page         分页参数
     * @param params       过滤条件（字段名 → 值，LIKE 模糊）
     * @param resourceCode 资源编号（= 菜单 code），用于定位数据权限规则；为空则不套规则
     * @return 分页数据（records / total）
     */
    Page<Map<String, Object>> pageFormDataByScope(Long formId, Page<?> page, Map<String, Object> params, String resourceCode);

    /**
     * 按「单元格」更新单条业务数据（通用列表行内编辑专用）。
     *
     * <p>与 {@link #saveBusinessData}（整体覆盖）的差异：<b>只更新指定的一个字段</b>，
     * 其余字段一律保留库中原值 —— 通用列表的行内编辑只能提交「这一格改了什么」，
     * 若走整体覆盖会把未加载/未渲染的字段清空。</p>
     *
     * <p>安全约束：字段名必须是合法标识符且真实存在于该表单主表（否则拒绝）；
     * 值按目标列类型转换（复用 {@code BizValueCoercer}）；同时刷新修改人/修改时间审计列。</p>
     *
     * @param formId   表单ID（workflow_bill.id）
     * @param dataId   数据行主键
     * @param fieldName 字段名（数据库列名）
     * @param value    新值（按列类型自动转换）
     * @return 是否更新成功
     */
    Boolean updateFormDataCell(Long formId, Long dataId, String fieldName, Object value);

    /** 单元格更新（带资源码，写链路套通用数据权限行规则） */
    Boolean updateFormDataCell(Long formId, Long dataId, String fieldName, Object value, String resourceCode);

    /**
     * 删除数据
     */
    boolean deleteFormData(Long modeId, Long dataId);

    /**
     * 按「表单ID」删除业务数据行（与 {@link #saveBusinessData} 同口径，不依赖 modeinfo）。
     *
     * <p>表名取 {@code workflow_bill.table_name}，按 id 删除一行。供 blade-workflow 删除草稿时
     * 清理其独占的业务数据行（草稿未发起，业务行不纳入任何正式单据），避免孤儿行。</p>
     *
     * @param formId 表单定义ID（workflow_bill.id）
     * @param dataId 业务数据ID（formtable_main_{id}.id）
     * @return 是否删除成功（行不存在返回 false）
     */
    boolean deleteBusinessData(Long formId, Long dataId);

    /**
     * 批量删除数据
     */
    boolean batchDeleteFormData(Long modeId, List<Long> dataIds);

    /**
     * 按「表单ID + 数据ID」读取<b>明细表数据</b>（与 {@link #saveBusinessData} 同口径）。
     *
     * <p>补齐此前最大缺口：明细数据只存在于流程快照 JSON，物理明细表
     * ({@code {主表名}_dtN}) 无读取入口，导致明细无法查询 / 统计 / 关联。</p>
     *
     * <p>实现：由 {@code workflow_bill.table_name} 得主表名 → 从 information_schema 找出
     * 该主表的所有明细物理表 → 逐表按关联列（优先 {@code main_id}，兼容 {@code mainid}）取行。</p>
     *
     * @param formId 表单定义ID（workflow_bill.id）
     * @param dataId 主表数据ID
     * @return 明细表名 → 行列表；无明细或无表时返回空 Map（不返回 null）
     */
    Map<String, List<Map<String, Object>>> getDetailData(Long formId, Long dataId);

}
