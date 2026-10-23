package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.crypto.DocFieldCrypto;
import org.springblade.doc.entity.DocCustomField;
import org.springblade.doc.entity.DocCustomFieldValue;
import org.springblade.doc.mapper.DocCustomFieldMapper;
import org.springblade.doc.mapper.DocCustomFieldValueMapper;
import org.springblade.doc.service.IDocCustomFieldService;
import org.springframework.stereotype.Service;

/**
 * 自定义字段值服务实现。
 *
 * <p>加密字段的写入与读取共用同一 {@link DocFieldCrypto}（同一 key/口径），
 * 从根上杜绝 E9 §18.4「写读 scope 不一致 → 加密被静默跳过 → 明文入库」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCustomFieldServiceImpl implements IDocCustomFieldService {

    private final DocCustomFieldMapper fieldMapper;
    private final DocCustomFieldValueMapper valueMapper;
    private final DocFieldCrypto crypto;

    @Override
    public DocCustomFieldValue saveValue(Long docId, Long fieldId, String plainValue) {
        if (docId == null || fieldId == null) {
            throw new ServiceException("docId/fieldId 不能为空");
        }
        DocCustomField field = fieldMapper.selectById(fieldId);
        if (field == null) {
            throw new ServiceException("字段不存在");
        }
        boolean encrypt = Integer.valueOf(1).equals(field.getIsEncrypt());

        DocCustomFieldValue val = valueMapper.selectOne(Wrappers.<DocCustomFieldValue>lambdaQuery()
            .eq(DocCustomFieldValue::getDocId, docId)
            .eq(DocCustomFieldValue::getFieldId, fieldId));
        if (val == null) {
            val = new DocCustomFieldValue();
            val.setDocId(docId);
            val.setFieldId(fieldId);
        }
        if (encrypt) {
            val.setFieldValue(crypto.encrypt(plainValue));
            val.setIsEncrypted(1);
        } else {
            val.setFieldValue(plainValue);
            val.setIsEncrypted(0);
        }
        if (val.getId() == null) {
            valueMapper.insert(val);
        } else {
            valueMapper.updateById(val);
        }
        return val;
    }

    @Override
    public String readValue(Long docId, Long fieldId) {
        DocCustomFieldValue val = valueMapper.selectOne(Wrappers.<DocCustomFieldValue>lambdaQuery()
            .eq(DocCustomFieldValue::getDocId, docId)
            .eq(DocCustomFieldValue::getFieldId, fieldId));
        if (val == null) {
            return null;
        }
        if (Integer.valueOf(1).equals(val.getIsEncrypted())) {
            return crypto.decrypt(val.getFieldValue());
        }
        return val.getFieldValue();
    }
}
