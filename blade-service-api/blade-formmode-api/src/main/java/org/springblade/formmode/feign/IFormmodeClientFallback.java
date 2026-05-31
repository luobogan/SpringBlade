package org.springblade.formmode.feign;

import org.springblade.core.tool.api.R;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.vo.FormModeVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单建模 Feign 客户端降级实现
 */
public class IFormmodeClientFallback implements IFormmodeClient {

    @Override
    public R<FormModeVO> getModeById(Long id) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<Long> saveFormData(FormDataDTO formDataDTO) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<FormDataVO> getFormDataById(Long id) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<List<FieldDefinitionDTO>> getBillFields(Long billId) {
        return R.fail("表单建模服务不可用");
    }

}
