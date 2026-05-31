package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.formmode.dto.FormDataDTO;
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
     * 批量删除数据
     */
    boolean batchDeleteFormData(Long modeId, List<Long> dataIds);

}
