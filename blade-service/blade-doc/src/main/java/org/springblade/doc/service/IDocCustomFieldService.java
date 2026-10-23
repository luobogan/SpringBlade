package org.springblade.doc.service;

import org.springblade.doc.entity.DocCustomFieldValue;

/**
 * 自定义字段值服务（读写同一加密口径，修 E9 §18.4 加密静默失效）。
 */
public interface IDocCustomFieldService {

    /** 保存字段值：字段 isEncrypt=1 时落密文，否则明文 */
    DocCustomFieldValue saveValue(Long docId, Long fieldId, String plainValue);

    /** 读取字段值：密文自动还原 */
    String readValue(Long docId, Long fieldId);
}
