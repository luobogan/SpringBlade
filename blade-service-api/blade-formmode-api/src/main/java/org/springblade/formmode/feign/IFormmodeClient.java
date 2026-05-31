package org.springblade.formmode.feign;

import org.springblade.core.tool.api.R;
import org.springblade.formmode.constant.FormmodeConstant;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.vo.FormModeVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单建模 Feign 客户端接口
 */
@FeignClient(
    value = FormmodeConstant.APPLICATION_FORMMODE_NAME,
    fallback = IFormmodeClientFallback.class
)
public interface IFormmodeClient {

    /**
     * 获取模块信息
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/mode/{id}")
    R<FormModeVO> getModeById(@PathVariable("id") Long id);

    /**
     * 保存表单数据
     */
    @PostMapping(FormmodeConstant.API_PREFIX + "/form/data/save")
    R<Long> saveFormData(@RequestBody FormDataDTO formDataDTO);

    /**
     * 查询表单数据
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/form/data/{id}")
    R<FormDataVO> getFormDataById(@PathVariable("id") Long id);

    /**
     * 获取字段定义列表
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/bill/{billId}/fields")
    R<List<FieldDefinitionDTO>> getBillFields(@PathVariable("billId") Long billId);

}
