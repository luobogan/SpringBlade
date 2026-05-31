package org.springblade.formmode.service;

import org.springblade.formmode.entity.ModeCustomSearch;

import java.util.List;

/**
 * 自定义搜索服务接口
 */
public interface ICustomSearchService {

    /**
     * 获取模块的自定义搜索列表
     */
    List<ModeCustomSearch> getCustomSearches(Long modeId);

    /**
     * 保存自定义搜索
     */
    boolean saveCustomSearch(ModeCustomSearch customSearch);

    /**
     * 删除自定义搜索
     */
    boolean deleteCustomSearch(Long searchId);

}
