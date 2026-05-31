package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.ModeBrowser;
import org.springblade.formmode.mapper.ModeBrowserMapper;
import org.springblade.formmode.service.IBrowserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 浏览框控制器
 *
 * 对应 ecology 的 FormModeDialogController / FormModeCptController
 */
@RestController
@RequestMapping("/api/blade-formmode/browser")
@Tag(name = "浏览框", description = "浏览框数据查询")
@RequiredArgsConstructor
public class FormBrowserController extends BladeController {

    private final IBrowserService browserService;
    private final ModeBrowserMapper modeBrowserMapper;

    /**
     * 获取浏览框配置列表
     */
    @GetMapping("/configs")
    public R<List<ModeBrowser>> getConfigs() {
        return R.data(modeBrowserMapper.selectList(null));
    }

    /**
     * 查询浏览框数据
     */
    @PostMapping("/data/{browserId}")
    public R<List<Map<String, Object>>> queryData(@PathVariable Long browserId,
                                                   @RequestBody(required = false) Map<String, Object> params) {
        return R.data(browserService.queryBrowserData(browserId, params));
    }

    /**
     * 根据ID获取浏览框单条数据
     */
    @GetMapping("/data/{browserId}/{value}")
    public R<Map<String, Object>> getDataById(@PathVariable Long browserId, @PathVariable String value) {
        return R.data(browserService.getBrowserDataById(browserId, value));
    }

    /**
     * 按名称搜索浏览框数据
     */
    @GetMapping("/search/{browserId}")
    public R<List<Map<String, Object>>> searchByName(@PathVariable Long browserId,
                                                      @RequestParam String keyword,
                                                      @RequestParam(required = false) Map<String, Object> params) {
        return R.data(browserService.searchBrowserDataByName(browserId, keyword, params));
    }

}
