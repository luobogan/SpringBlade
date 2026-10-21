package org.springblade.formmode.feign;

import org.springblade.core.tool.api.R;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.vo.FieldPermVO;
import org.springblade.formmode.vo.FormLayoutVO;
import org.springblade.formmode.vo.FormModeVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单建模 Feign 客户端降级实现
 */
@Component
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

    @Override
    public R<FormLayoutVO> getFormLayout(Long formId, Integer layoutType, String nodeKey) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<Long> saveBusinessData(FormDataSaveDTO dto) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<Boolean> deleteFormLayoutByNode(Long formId, String nodeKey) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<Boolean> deleteBusinessData(Long formId, Long dataId) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<FormDataVO> getBusinessData(Long formId, Long dataId) {
        return R.fail("表单建模服务不可用");
    }

    @Override
    public R<Map<String, List<Map<String, Object>>>> getDetailData(Long formId, Long dataId) {
        // 降级：服务不可用时返回空明细结构，避免调用方 NPE（调用方应判空而非直接取值）
        return R.data(new java.util.LinkedHashMap<>());
    }

    @Override
    public R<List<FieldPermVO>> listFieldPerm(Long modeId) {
        // 降级：返回空字段权限列表 = 「未配置」，调用方按字段定义默认渲染（fail-open）
        return R.data(new java.util.ArrayList<>());
    }

}
