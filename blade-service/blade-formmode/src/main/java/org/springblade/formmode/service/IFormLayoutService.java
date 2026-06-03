package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FormLayout;

/**
 * 表单布局服务接口
 */
public interface IFormLayoutService extends IService<FormLayout> {

    /**
     * 根据表单ID获取布局
     * @param formId 表单ID
     * @return 表单布局
     */
    FormLayout getByFormId(Long formId);

    /**
     * 保存表单布局
     * @param formLayout 表单布局
     * @return 是否成功
     */
    boolean saveFormLayout(FormLayout formLayout);

    /**
     * 解析布局JSON
     * @param layoutJson 布局JSON
     * @return 解析结果
     */
    String parseLayoutJson(String layoutJson);
}
