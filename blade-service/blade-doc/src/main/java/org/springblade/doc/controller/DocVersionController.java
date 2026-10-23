package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容引擎 - 版本/文件列表（C-7）。
 *
 * <p>版本存于 {@link DocImageFile#getVersionId()}（对齐 E9「无物理 docversion 表」结论）；
 * 读取需记录级 canRead。</p>
 */
@RestController
@RequestMapping("/doc/version")
@RequiredArgsConstructor
@Tag(name = "内容引擎-版本管理", description = "文档版本/文件列表")
public class DocVersionController {

    private final DocImageFileMapper imageFileMapper;
    private final DocPermissionService docPermissionService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/{docId}")
    @Operation(summary = "版本/文件列表", description = "已登录 + 记录级 canRead")
    public R<List<DocImageFile>> versions(@PathVariable Long docId) {
        if (!docPermissionService.canRead(docId)) {
            throw new ServiceException("无权限查看该文档");
        }
        return R.data(imageFileMapper.selectList(Wrappers.<DocImageFile>lambdaQuery()
            .eq(DocImageFile::getDocId, docId)
            .orderByDesc(DocImageFile::getVersionId)));
    }
}
