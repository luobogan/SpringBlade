package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.WpsSign;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - WPS 文档中台服务端回调（无会话，靠 {@code _w_signature} 鉴权）。
 *
 * <p>WPS 中台按在 WPS 控制台登记的回调地址调用本组端点（下载/上传/元信息/版本）。
 * 本组端点与「前端 jssdk」共用同一套 SHA1 签名规则（见 {@link WpsSign}），彼此解耦：
 * 前端负责在浏览器拉起编辑，WPS 中台负责经本组端点拉/存文件。</p>
 *
 * <p>需在 WPS 控制台登记的回调地址（按实际部署域名替换）：</p>
 * <ul>
 *   <li>文件信息：{base}/doc/wps/file/info</li>
 *   <li>文件下载：{base}/doc/wps/file/download</li>
 *   <li>文件上传(保存)：{base}/doc/wps/file/upload</li>
 *   <li>版本列表：{base}/doc/wps/file/version</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/doc/wps/file")
@RequiredArgsConstructor
@Tag(name = "内容引擎-WPS中台回调", description = "download/upload/info/version，靠 _w_signature 鉴权")
public class WpsCallbackController {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final IDocFileStore fileStore;
    private final DocProperties docProperties;

    private DocProperties.Wps wps() {
        return docProperties.getWps();
    }

    private boolean verify(HttpServletRequest req) {
        Map<String, String> params = new HashMap<>();
        req.getParameterMap().forEach((k, v) -> params.put(k, v != null && v.length > 0 ? v[0] : ""));
        return WpsSign.serverVerify(params, wps().getAppSecret());
    }

    private Long docIdOf(HttpServletRequest req) {
        String fid = req.getParameter("fileid");
        if (fid == null) {
            fid = req.getParameter("_w_fileid");
        }
        return Func.isBlank(fid) ? null : Long.parseLong(fid);
    }

    @PreAuth("permitAll()")
    @Operation(summary = "WPS 中台-文件元信息")
    @GetMapping("/info")
    public R<?> info(HttpServletRequest req) {
        if (!verify(req)) {
            throw new ServiceException("WPS 回调签名无效");
        }
        Long docId = docIdOf(req);
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        long size = 0;
        String name = doc.getDocSubject();
        Long mainFileId = doc.getMainFileId();
        if (mainFileId != null) {
            DocImageFile f = imageFileMapper.selectOne(
                Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, mainFileId));
            if (f != null) {
                size = f.getFileSize() == null ? 0 : f.getFileSize();
                if (f.getFileName() != null) {
                    name = f.getFileName();
                }
            }
        }
        long version = imageFileMapper.selectCount(
            Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getDocId, docId).eq(DocImageFile::getDocFileType, 2));

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("fileid", String.valueOf(docId));
        info.put("name", name);
        info.put("size", size);
        info.put("version", version);
        Map<String, Integer> acl = new LinkedHashMap<>();
        acl.put("rename", 0);
        acl.put("history", 1);
        acl.put("copy", 1);
        acl.put("export_pdf", 1);
        acl.put("print", 1);
        acl.put("save_as", 0);
        info.put("user_acl", acl);
        info.put("watermark", "");
        info.put("creator", "");
        return R.data(info);
    }

    @PreAuth("permitAll()")
    @Operation(summary = "WPS 中台-文件下载（流式返回正文）")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(HttpServletRequest req) {
        if (!verify(req)) {
            throw new ServiceException("WPS 回调签名无效");
        }
        Long docId = docIdOf(req);
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null || doc.getMainFileId() == null) {
            throw new ServiceException("文档或正文文件不存在");
        }
        byte[] data = fileStore.download(doc.getMainFileId())
            .orElseThrow(() -> new ServiceException("文件不存在"));
        String name = doc.getDocSubject();
        DocImageFile f = imageFileMapper.selectOne(
            Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, doc.getMainFileId()));
        if (f != null && f.getFileName() != null) {
            name = f.getFileName();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encode(name) + "\"")
            .body(data);
    }

    @PreAuth("permitAll()")
    @Operation(summary = "WPS 中台-文件上传(保存)：接收编辑后文件存为新版本")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<?> upload(HttpServletRequest req, @RequestParam("file") MultipartFile file) {
        if (!verify(req)) {
            throw new ServiceException("WPS 回调签名无效");
        }
        Long docId = docIdOf(req);
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件为空");
        }
        try {
            byte[] content = file.getBytes();
            Long newFileId = fileStore.save(content);
            DocImageFile rec = new DocImageFile();
            rec.setDocId(docId);
            rec.setVersionId(newFileId);
            rec.setFileId(newFileId);
            rec.setFileName(file.getOriginalFilename());
            rec.setFileSize((long) content.length);
            rec.setDocFileType(2);
            imageFileMapper.insert(rec);
            // 回写主文件ID，使下次编辑/下载加载最新版本
            doc.setMainFileId(newFileId);
            detailMapper.updateById(doc);
            long version = imageFileMapper.selectCount(
                Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getDocId, docId).eq(DocImageFile::getDocFileType, 2));
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("fileid", String.valueOf(docId));
            r.put("version", version);
            return R.data(r);
        } catch (IOException e) {
            throw new ServiceException("保存文件读取失败");
        }
    }

    @PreAuth("permitAll()")
    @Operation(summary = "WPS 中台-版本列表")
    @GetMapping("/version")
    public R<?> version(HttpServletRequest req) {
        if (!verify(req)) {
            throw new ServiceException("WPS 回调签名无效");
        }
        Long docId = docIdOf(req);
        List<DocImageFile> list = imageFileMapper.selectList(
            Wrappers.<DocImageFile>lambdaQuery()
                .eq(DocImageFile::getDocId, docId).eq(DocImageFile::getDocFileType, 2)
                .orderByDesc(DocImageFile::getCreateTime));
        List<Object> versions = new ArrayList<>();
        long v = list.size();
        for (DocImageFile f : list) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("version", v--);
            o.put("size", f.getFileSize() == null ? 0 : f.getFileSize());
            o.put("name", f.getFileName());
            o.put("created_at", f.getCreateTime() == null ? "" : f.getCreateTime().toString());
            versions.add(o);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("versions", versions);
        return R.data(r);
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
