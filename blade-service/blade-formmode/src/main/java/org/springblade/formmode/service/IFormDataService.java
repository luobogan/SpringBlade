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
