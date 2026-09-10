package org.springblade.formmode.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.springblade.formmode.entity.FormLayout;

import java.util.List;

/**
 * 表单布局服务接口
 */
public interface IFormLayoutService extends IService<FormLayout> {

    /**
     * 根据表单ID获取布局（兼容旧调用：返回该表单最新一条布局）
     * @param formId 表单ID
     * @return 表单布局
     */
    FormLayout getByFormId(Long formId);

    /**
     * 按 formId + 布局类型 + 节点Key 解析布局（带回退）。
     *
     * <p>回退优先级：
     * ① 节点级(指定类型) → ② 节点级(默认类型0) → ③ 表单级(指定类型)
     * → ④ 表单级(默认类型0) → ⑤ 该表单任意最新一条（兼容旧数据）。</p>
     *
     * @param formId     表单ID
     * @param layoutType 布局类型（null 视为默认 0）
     * @param nodeKey    流程节点Key（空=表单级通用）
     */
    FormLayout getByFormId(Long formId, Integer layoutType, String nodeKey);

    /**
     * 列出表单下的布局（可按类型/节点过滤）
     * @param formId     表单ID
     * @param layoutType 布局类型（可空）
     * @param nodeKey    节点Key（可空）
     */
    List<FormLayout> listByFormId(Long formId, Integer layoutType, String nodeKey);

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
