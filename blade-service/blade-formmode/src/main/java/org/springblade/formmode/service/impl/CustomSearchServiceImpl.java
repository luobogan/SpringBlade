package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeCustomSearch;
import org.springblade.formmode.mapper.ModeCustomSearchMapper;
import org.springblade.formmode.service.ICustomSearchService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义搜索服务实现
 */
@Service
@RequiredArgsConstructor
public class CustomSearchServiceImpl implements ICustomSearchService {

    private final ModeCustomSearchMapper customSearchMapper;

    @Override
    public List<ModeCustomSearch> getCustomSearches(Long modeId) {
        return customSearchMapper.selectList(
            new LambdaQueryWrapper<ModeCustomSearch>()
                .eq(ModeCustomSearch::getModeid, modeId.intValue())
                .orderByAsc(ModeCustomSearch::getDsporder)
        );
    }

    @Override
    public boolean saveCustomSearch(ModeCustomSearch customSearch) {
        return customSearchMapper.insert(customSearch) > 0;
    }

    @Override
    public boolean deleteCustomSearch(Long searchId) {
        return customSearchMapper.deleteById(searchId) > 0;
    }

}
