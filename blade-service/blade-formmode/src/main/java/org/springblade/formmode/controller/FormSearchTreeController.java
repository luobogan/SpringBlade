package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.ModeCustomSearch;
import org.springblade.formmode.entity.ModeCustomTree;
import org.springblade.formmode.service.ICustomSearchService;
import org.springblade.formmode.service.ICustomTreeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自定义搜索/树控制器
 */
@RestController
@RequestMapping("/api/blade-formmode")
@Tag(name = "自定义搜索/树", description = "自定义搜索和树配置")
@RequiredArgsConstructor
public class FormSearchTreeController extends BladeController {

    private final ICustomSearchService customSearchService;
    private final ICustomTreeService customTreeService;

    // ==================== 自定义搜索 ====================

    @GetMapping("/search/{modeId}")
    public R<List<ModeCustomSearch>> getSearches(@PathVariable Long modeId) {
        return R.data(customSearchService.getCustomSearches(modeId));
    }

    @PostMapping("/search")
    public R<Boolean> saveSearch(@RequestBody ModeCustomSearch customSearch) {
        return R.data(customSearchService.saveCustomSearch(customSearch), "搜索保存成功");
    }

    @DeleteMapping("/search/{id}")
    public R<Boolean> deleteSearch(@PathVariable Long id) {
        return R.data(customSearchService.deleteCustomSearch(id), "搜索删除成功");
    }

    // ==================== 自定义树 ====================

    @GetMapping("/tree/{modeId}")
    public R<List<ModeCustomTree>> getTrees(@PathVariable Long modeId) {
        return R.data(customTreeService.getCustomTrees(modeId));
    }

    @PostMapping("/tree")
    public R<Boolean> saveTree(@RequestBody ModeCustomTree customTree) {
        return R.data(customTreeService.saveCustomTree(customTree), "树配置保存成功");
    }

    @DeleteMapping("/tree/{id}")
    public R<Boolean> deleteTree(@PathVariable Long id) {
        return R.data(customTreeService.deleteCustomTree(id), "树配置删除成功");
    }

    @PostMapping("/tree/data/{treeId}")
    public R<List<Map<String, Object>>> getTreeData(@PathVariable Long treeId) {
        return R.data(customTreeService.getTreeData(treeId));
    }

}
