package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.vo.FormModeVO;

import java.util.List;

/**
 * 表单模块管理服务接口
 *
 * 对应 ecology 的 ModeInfoService + FormInfoService
 */
public interface IFormModeService extends IService<ModeInfo> {

    /**
     * 创建表单模块（含表单定义、字段定义、动态表）
     */
    FormModeVO createFormMode(FormModeDTO formModeDTO);

    /**
     * 更新表单模块
     */
    FormModeVO updateFormMode(Long modeId, FormModeDTO formModeDTO);

    /**
     * 删除表单模块（含配置表数据）
     */
    boolean deleteFormMode(Long modeId);

    /**
     * 获取表单模块完整信息
     */
    FormModeVO getFormModeDetail(Long modeId);

    /**
     * 根据模块ID查询字段定义
     */
    List<FieldDefinitionDTO> getFieldDefinitions(Long modeId);

    /**
     * 动态创建数据表（formtable_main_{billid}）
     */
    boolean createDynamicTable(Integer billId, List<WorkflowBillField> fields);

    /**
     * 动态删除数据表
     */
    boolean dropDynamicTable(Integer billId);

    /**
     * 生成新的表单ID（负数自减）
     */
    Integer generateBillId();

}
