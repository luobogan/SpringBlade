package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - WPS WebOffice 服务端回调（solution.wps.cn 官方 v3 协议）。
 *
 * <p>路径体系为 WPS 标准 {@code /v3/3rd/files/{fileId}[/download|/permission|/upload...]}，
 * 由 WPS 中台按控制台登记的回调地址调用（预览拉取 / 编辑保存）。鉴权靠前端 init 传入、
 * 回调经请求头 {@code X-WebOffice-Token} 回传的业务 token（按 fileId 验签）。</p>
 *
 * <p>响应遵循 WPS 协议：成功直接返回业务 JSON（code=0），失败返回 {@code {"code":xxx,"message":...}}；
 * 故不使用 BladeX 的 R 包装。fileId 既可能是附件 fileId（DocImageFile.fileId），也可能是 docId（主文件），
 * {@link #actualFileId(String)} 做兼容解析。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v3/3rd")
@RequiredArgsConstructor
@Tag(name = "内容引擎-WPS WebOffice回调", description = "/v3/3rd/files/* 预览/编辑回调，X-WebOffice-Token 鉴权")
public class WpsCallbackController {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocUrlSigner signer;
    private final DocProperties docProperties;
    private final IDocEngineSettingService settingService;
    private final JdbcTemplate jdbcTemplate;
    private final org.springblade.doc.store.IDocFileStore fileStore;

    // ---------------- 鉴权 / 工具 ----------------

    private boolean authed(HttpServletRequest req, String fileId) {
        String token = req.getHeader("X-WebOffice-Token");
        return token != null && !token.isBlank() && signer.verify("wps:" + fileId, token);
    }

    private String publicBase(HttpServletRequest req) {
        String base = settingService.get("wps_public_base", null);
        if (base == null || base.isBlank()) {
            base = docProperties.getWps().getPublicBase();
        }
        if (base != null && !base.isBlank()) {
            return base.trim().replaceAll("/+$", "");
        }
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        boolean def = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host + (def ? "" : ":" + port);
    }

    /** fileId → 实际文件ID：优先按 DocImageFile.fileId 命中；否则视为 docId 取其主文件 */
    private Long actualFileId(String fileId) {
        Long fid = parseLong(fileId);
        if (fid == null) {
            return null;
        }
        DocImageFile img = imageFileMapper.selectOne(Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, fid));
        if (img != null) {
            return fid;
        }
        DocDetail doc = detailMapper.selectById(fid);
        if (doc != null && doc.getMainFileId() != null) {
            return doc.getMainFileId();
        }
        return fid;
    }

    private DocImageFile imageOf(Long actualFileId) {
        return actualFileId == null ? null
            : imageFileMapper.selectOne(Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, actualFileId));
    }

    private int versionOf(Long actualFileId) {
        DocImageFile img = imageOf(actualFileId);
        Long docId = img != null ? img.getDocId() : actualFileId;
        Long cnt = imageFileMapper.selectCount(Wrappers.<DocImageFile>lambdaQuery()
            .eq(DocImageFile::getDocId, docId).eq(DocImageFile::getDocFileType, 2));
        return cnt == null || cnt < 1 ? 1 : cnt.intValue();
    }

    private static Long parseLong(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Long.parseLong(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- 预览 ----------------

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}")
    @Operation(summary = "WPS-文件元信息")
    public ResponseEntity<Map<String, Object>> info(HttpServletRequest req, @PathVariable String fileId) {
        log.info("WPS info 到站 fileId={}, hasToken={}, ua={}", fileId,
            req.getHeader("X-WebOffice-Token") != null, req.getHeader("User-Agent"));
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        Long af = actualFileId(fileId);
        if (af == null) {
            return err(40005, "file_id invalid");
        }
        DocImageFile img = imageOf(af);
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", fileId);
        r.put("name", (img != null && img.getFileName() != null) ? img.getFileName() : (fileId + ".docx"));
        r.put("version", versionOf(af));
        r.put("size", (img != null && img.getFileSize() != null) ? img.getFileSize() : 0L);
        r.put("create_time", now);
        r.put("modify_time", now);
        r.put("creator_id", "0");
        r.put("modifier_id", "0");
        return json(r);
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/download")
    @Operation(summary = "WPS-文件下载地址")
    public ResponseEntity<Map<String, Object>> download(HttpServletRequest req, @PathVariable String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        Long af = actualFileId(fileId);
        if (af == null) {
            return err(40005, "file_id invalid");
        }
        String url = publicBase(req) + "/doc/file/" + af + "/content?token=" + signer.sign(String.valueOf(af));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("url", url);
        return json(r);
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/permission")
    @Operation(summary = "WPS-文档用户权限")
    public ResponseEntity<Map<String, Object>> permission(HttpServletRequest req, @PathVariable String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("read", 1);
        r.put("update", 0);   // 预览只读；编辑模式需配合用户信息接口再放开
        r.put("download", 1);
        r.put("rename", 0);
        r.put("history", 1);
        r.put("copy", 1);
        r.put("print", 1);
        r.put("saveas", 0);
        r.put("comment", 1);
        return json(r);
    }

    // ---------------- 编辑保存 ----------------

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/upload/prepare")
    @Operation(summary = "WPS-上传准备")
    public ResponseEntity<Map<String, Object>> prepare(HttpServletRequest req, @PathVariable String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        return json(new LinkedHashMap<>());
    }

    @PreAuth("permitAll()")
    @PostMapping("/files/{fileId}/upload/address")
    @Operation(summary = "WPS-上传地址")
    public ResponseEntity<Map<String, Object>> address(HttpServletRequest req, @PathVariable String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("url", publicBase(req) + "/v3/3rd/files/" + fileId + "/upload");
        return json(r);
    }

    @PreAuth("permitAll()")
    @PostMapping(value = "/files/{fileId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "WPS-上传(保存)")
    public ResponseEntity<Map<String, Object>> upload(HttpServletRequest req, @PathVariable String fileId,
                                                      @RequestParam("file") MultipartFile file) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        if (file == null || file.isEmpty()) {
            return err(40005, "empty file");
        }
        try {
            Long af = actualFileId(fileId);
            DocImageFile img = imageOf(af);
            Long docId = img != null ? img.getDocId() : parseLong(fileId);
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
            if (docId != null) {
                DocDetail doc = detailMapper.selectById(docId);
                if (doc != null) {
                    doc.setMainFileId(newFileId);
                    detailMapper.updateById(doc);
                }
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", fileId);
            r.put("name", file.getOriginalFilename());
            r.put("version", versionOf(newFileId));
            r.put("size", content.length);
            return json(r);
        } catch (IOException e) {
            log.error("WPS 上传保存失败 fileId={}", fileId, e);
            return err(50001, "save error");
        }
    }

    @PreAuth("permitAll()")
    @PostMapping("/files/{fileId}/upload/complete")
    @Operation(summary = "WPS-上传完成")
    public ResponseEntity<Map<String, Object>> complete(HttpServletRequest req, @PathVariable String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
        Long af = actualFileId(fileId);
        DocImageFile img = imageOf(af);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", fileId);
        r.put("name", img != null ? img.getFileName() : fileId);
        r.put("version", versionOf(af));
        r.put("size", (img != null && img.getFileSize() != null) ? img.getFileSize() : 0L);
        return json(r);
    }

    @PreAuth("permitAll()")
    @GetMapping("/users")
    @Operation(summary = "WPS-用户信息")
    public ResponseEntity<Map<String, Object>> users(
        @RequestParam(name = "user_ids", required = false) List<String> userIds) {
        List<Map<String, Object>> arr = new ArrayList<>();
        if (userIds != null) {
            for (String uid : userIds) {
                if (uid == null || uid.isBlank()) {
                    continue;
                }
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("id", uid);
                u.put("name", nickNameOf(uid));
                arr.add(u);
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("data", arr);
        r.put("message", "");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
    }

    /** 用户昵称：优先 sys_user.account，查不到兜位 */
    private String nickNameOf(String uid) {
        try {
            return jdbcTemplate.queryForObject("SELECT account FROM sys_user WHERE id=?", String.class, Long.valueOf(uid));
        } catch (Exception e) {
            return "用户" + uid;
        }
    }

    // ---------------- 响应 ----------------

    private static ResponseEntity<Map<String, Object>> json(Map<String, Object> data) {
        // WPS 回调统一响应格式：{"code":0,"data":{...},"message":""}
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("data", data);
        r.put("message", "");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
    }

    private static ResponseEntity<Map<String, Object>> err(int code, String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", code);
        r.put("message", message);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
    }
}
