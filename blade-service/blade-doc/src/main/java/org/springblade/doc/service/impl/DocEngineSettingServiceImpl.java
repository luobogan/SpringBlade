package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.doc.entity.DocEngineSetting;
import org.springblade.doc.mapper.DocEngineSettingMapper;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springframework.stereotype.Service;

/**
 * 内容引擎 - 编辑器设置服务实现。
 */
@Service
@RequiredArgsConstructor
public class DocEngineSettingServiceImpl implements IDocEngineSettingService {

    private final DocEngineSettingMapper mapper;

    @Override
    public String get(String key, String defaultValue) {
        DocEngineSetting one = mapper.selectOne(
            Wrappers.<DocEngineSetting>lambdaQuery().eq(DocEngineSetting::getSettingKey, key));
        return (one != null && one.getSettingValue() != null) ? one.getSettingValue() : defaultValue;
    }

    @Override
    public void set(String key, String value) {
        DocEngineSetting one = mapper.selectOne(
            Wrappers.<DocEngineSetting>lambdaQuery().eq(DocEngineSetting::getSettingKey, key));
        if (one == null) {
            one = new DocEngineSetting();
            one.setSettingKey(key);
            one.setSettingValue(value);
            mapper.insert(one);
        } else {
            one.setSettingValue(value);
            mapper.updateById(one);
        }
    }
}
