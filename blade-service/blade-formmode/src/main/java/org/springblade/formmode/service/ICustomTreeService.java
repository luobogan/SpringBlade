package org.springblade.formmode.service;

import org.springblade.formmode.entity.ModeCustomTree;

import java.util.List;
import java.util.Map;

/**
 * 自定义树服务接口
 */
public interface ICustomTreeService {

    /**
     * 获取模块的树配置列表
     */
    List<ModeCustomTree> getCustomTrees(Long modeId);

    /**
     * 保存树配置
     */
    boolean saveCustomTree(ModeCustomTree customTree);

    /**
     * 删除树配置
     */
    boolean deleteCustomTree(Long treeId);

    /**
     * 获取树数据
     */
    List<Map<String, Object>> getTreeData(Long treeId);

}
