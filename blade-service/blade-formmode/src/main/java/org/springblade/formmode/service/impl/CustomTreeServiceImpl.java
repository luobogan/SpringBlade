package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeCustomTree;
import org.springblade.formmode.mapper.ModeCustomTreeMapper;
import org.springblade.formmode.service.ICustomTreeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 自定义树服务实现
 */
@Service
@RequiredArgsConstructor
public class CustomTreeServiceImpl implements ICustomTreeService {

    private final ModeCustomTreeMapper customTreeMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ModeCustomTree> getCustomTrees(Long modeId) {
        return customTreeMapper.selectList(
            new LambdaQueryWrapper<ModeCustomTree>()
                .eq(ModeCustomTree::getModeid, modeId.intValue())
                .orderByAsc(ModeCustomTree::getDsporder)
        );
    }

    @Override
    public boolean saveCustomTree(ModeCustomTree customTree) {
        return customTreeMapper.insert(customTree) > 0;
    }

    @Override
    public boolean deleteCustomTree(Long treeId) {
        return customTreeMapper.deleteById(treeId) > 0;
    }

    @Override
    public List<Map<String, Object>> getTreeData(Long treeId) {
        ModeCustomTree tree = customTreeMapper.selectById(treeId);
        if (tree == null || tree.getDatasql() == null) {
            return List.of();
        }
        // 执行自定义SQL查询树数据
        return jdbcTemplate.queryForList(tree.getDatasql());
    }

}
