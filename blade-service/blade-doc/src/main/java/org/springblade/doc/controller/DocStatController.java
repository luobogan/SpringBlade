package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.service.IDocStatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - 文档统计（统一维度抽象）。
 *
 * <p>维度→分组列走白名单映射，不拼接用户输入（守 G2）；只读 GET + @PreAuth。</p>
 */
@RestController
@RequestMapping("/doc/stat")
@RequiredArgsConstructor
@Tag(name = "内容引擎-统计", description = "统一维度统计（创建人/目录/分部/日期）+ 总览")
public class DocStatController {

    private final IDocStatService statService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/count")
    @Operation(summary = "按维度统计", description = "dim=author|category|division|time，可选日期区间")
    public R<List<Map<String, Object>>> count(@RequestParam String dim,
                                              @RequestParam(required = false) String fromDate,
                                              @RequestParam(required = false) String toDate) {
        return R.data(statService.countByDim(dim, fromDate, toDate));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/overview")
    @Operation(summary = "总览", description = "total/draft/effective/recycled")
    public R<Map<String, Object>> overview() {
        return R.data(statService.overview());
    }
}
