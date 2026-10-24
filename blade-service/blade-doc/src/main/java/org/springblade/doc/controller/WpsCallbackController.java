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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - WPS WebOffice 服务端回调（solution.wps.cn 官方 v3 协议）。
 *
 * <p>路径体系为 WPS 标准 {@code /v3/3rd/files/{fileId}[/download|/permission|/upload...]}，
 * 由 WPS 中台按控制台登记的回调地址调用（预览拉取 / 在线编辑保存）。鉴权靠前端 init 传入、
 * 回调经请求头 {@code X-WebOffice-Token} 回传的业务 token（按 fileId 验签）。</p>
 *
 * <p>响应遵循 WPS 协议：成功 {@code {"code":0,"data":{...},"message":""}}，失败 {@code {"code":xxx,"message":...}}；
 * 故不使用 BladeX 的 R 包装。fileId 既可能是附件 fileId（DocImageFile.fileId），也可能是 docId（主文件），
 * {@link #actualFileId(String)} 做兼容解析。在线编辑保存为三阶段：prepare → address(PUT) → upload(PUT) → complete。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v3/3rd")
@RequiredArgsConstructor
@Tag(name = "内容引擎-WPS WebOffice回调", description = "/v3/3rd/files/* 预览/在线编辑回调，X-WebOffice-Token 鉴权")
public class WpsCallbackController {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocUrlSigner signer;
    private final DocProperties docProperties;
    private final IDocEngineSettingService settingService;
    private final JdbcTemplate jdbcTemplate;
    private final org.springblade.doc.store.IDocFileStore fileStore;

    // ---------------- 鉴权 / 工具 ----------------

    private void softCheck(HttpServletRequest req, String fileId) {
        if (!authed(req, fileId)) {
            log.warn("WPS 回调 token 校验未通过（临时放行联调），fileId={}, hasToken={}", fileId,
                req.getHeader("X-WebOffice-Token") != null);
        }
    }

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

    /** 解析 WPS 回调头 X-User-Query（URL query + 前端 init 的 customArgs 平铺） */
    private Map<String, String> userQuery(HttpServletRequest req) {
        Map<String, String> m = new HashMap<>();
        String q = req.getHeader("X-User-Query");
        if (q != null && !q.isBlank()) {
            for (String kv : q.split("&")) {
                int i = kv.indexOf('=');
                if (i > 0) {
                    String k = kv.substring(0, i);
                    String v = kv.substring(i + 1);
                    try {
                        v = URLDecoder.decode(v, StandardCharsets.UTF_8.name());
                    } catch (Exception ignore) {
                        // 保留原值
                    }
                    m.put(k, v);
                }
            }
        }
        return m;
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
        List<DocImageFile> list = versionChain(docIdOfActual(actualFileId));
        for (int i = 0; i < list.size(); i++) {
            if (actualFileId != null && actualFileId.equals(list.get(i).getFileId())) {
                return i + 1;
            }
        }
        return list.isEmpty() ? 1 : list.size();
    }

    /** actualFileId → 所属 docId */
    private Long docIdOfActual(Long actualFileId) {
        DocImageFile img = imageOf(actualFileId);
        return img != null ? img.getDocId() : actualFileId;
    }

    /** fileId(可能是附件 fileId 或 docId) → 所属 docId */
    private Long docIdOf(String fileId) {
        return docIdOfActual(actualFileId(fileId));
    }

    /** 版本链：该 doc 的主文件正文/编辑版本(versionId 非空)按 versionId 正序(v1 最早)；上传附件 versionId 为空，不计入 */
    private List<DocImageFile> versionChain(Long docId) {
        return imageFileMapper.selectList(Wrappers.<DocImageFile>lambdaQuery()
            .eq(DocImageFile::getDocId, docId).eq(DocImageFile::getDocFileType, 2)
            .isNotNull(DocImageFile::getVersionId)
            .orderByAsc(DocImageFile::getVersionId));
    }

    /** 版本 8 字段（versions/version 用，version 为流水序号） */
    private Map<String, Object> versionItem(String fileId, DocImageFile img, int ver) {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", fileId);
        r.put("name", (img != null && img.getFileName() != null) ? img.getFileName() : (fileId + ".docx"));
        r.put("version", ver);
        r.put("size", (img != null && img.getFileSize() != null) ? img.getFileSize() : 0L);
        r.put("create_time", now);
        r.put("modify_time", now);
        r.put("creator_id", "0");
        r.put("modifier_id", "0");
        return r;
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
        softCheck(req, fileId);
        Long af = actualFileId(fileId);
        if (af == null) {
            return err(40005, "file_id invalid");
        }
        return json(versionData(fileId, af));
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/download")
    @Operation(summary = "WPS-文件下载地址")
    public ResponseEntity<Map<String, Object>> download(HttpServletRequest req, @PathVariable String fileId) {
        softCheck(req, fileId);
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
        softCheck(req, fileId);
        Map<String, String> uq = userQuery(req);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("read", 1);
        r.put("update", 1);   // 放开在线编辑
        r.put("download", 1);
        r.put("rename", 1);   // 已接入重命名接口
        r.put("history", 1);  // 已接入历史版本接口
        r.put("copy", 1);
        r.put("print", 1);
        r.put("saveas", 0);
        r.put("comment", 1);
        // update=1 必须返回 user_id，且该用户可经 /v3/3rd/users 查到
        r.put("user_id", uq.getOrDefault("userId", "0"));
        return json(r);
    }

    // ---------------- 编辑保存（三阶段） ----------------

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/upload/prepare")
    @Operation(summary = "WPS-上传准备")
    public ResponseEntity<Map<String, Object>> prepare(HttpServletRequest req, @PathVariable String fileId) {
        softCheck(req, fileId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("digest_types", List.of("sha1"));
        return json(r);
    }

    @PreAuth("permitAll()")
    @PostMapping("/files/{fileId}/upload/address")
    @Operation(summary = "WPS-获取上传地址")
    public ResponseEntity<Map<String, Object>> address(HttpServletRequest req, @PathVariable String fileId,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        softCheck(req, fileId);
        // 规范：address 请求体带 name/size/digest/is_manual；把 name 透传到 PUT 上传地址，保证新版本文件名准确
        String name = (body != null && body.get("name") != null) ? String.valueOf(body.get("name")) : null;
        String url = publicBase(req) + "/v3/3rd/files/" + fileId + "/upload";
        if (name != null && !name.isBlank()) {
            try {
                url += "?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            } catch (Exception ignore) {
                url += "?name=" + name;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("method", "PUT");
        r.put("url", url);
        return json(r);
    }

    /** 三阶段保存第二步：WPS 以 PUT 将文件实体上传到 address 返回的 url（octet-stream body） */
    @PreAuth("permitAll()")
    @PutMapping(value = "/files/{fileId}/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "WPS-上传文件实体(PUT)")
    public ResponseEntity<Map<String, Object>> uploadPut(HttpServletRequest req, @PathVariable String fileId,
                                                         @RequestParam(name = "name", required = false) String name,
                                                         @RequestBody byte[] body) {
        softCheck(req, fileId);
        return saveNewVersion(fileId, body, name);
    }

    /** 单阶段保存（兼容保留，WPS 已推荐三阶段） */
    @PreAuth("permitAll()")
    @PostMapping(value = "/files/{fileId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "WPS-上传(单阶段保存)")
    public ResponseEntity<Map<String, Object>> upload(HttpServletRequest req, @PathVariable String fileId,
                                                      @RequestParam("file") MultipartFile file) {
        softCheck(req, fileId);
        if (file == null || file.isEmpty()) {
            return err(40005, "empty file");
        }
        try {
            return saveNewVersion(fileId, file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            log.error("WPS 上传读取失败 fileId={}", fileId, e);
            return err(50001, "read error");
        }
    }

    @PreAuth("permitAll()")
    @PostMapping("/files/{fileId}/upload/complete")
    @Operation(summary = "WPS-上传完成回调")
    public ResponseEntity<Map<String, Object>> complete(HttpServletRequest req, @PathVariable String fileId) {
        softCheck(req, fileId);
        Long af = actualFileId(fileId);
        if (af == null) {
            return err(40005, "file_id invalid");
        }
        return json(versionData(fileId, af));
    }

    /** 保存为新版本：写文件存储 + 新 DocImageFile 记录 + 回写文档主文件ID */
    private ResponseEntity<Map<String, Object>> saveNewVersion(String fileId, byte[] content, String name) {
        if (content == null || content.length == 0) {
            return err(40005, "empty file");
        }
        try {
            Long af = actualFileId(fileId);
            DocImageFile img = imageOf(af);
            Long docId = img != null ? img.getDocId() : parseLong(fileId);
            Long newFileId = fileStore.save(content);
            DocImageFile rec = new DocImageFile();
            rec.setDocId(docId);
            rec.setVersionId(newFileId);
            rec.setFileId(newFileId);
            rec.setFileName(name != null && !name.isBlank() ? name
                : (img != null && img.getFileName() != null ? img.getFileName() : (fileId + ".docx")));
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
            log.info("WPS 保存新版本 fileId={} -> newFileId={}, size={}", fileId, newFileId, content.length);
            return json(versionData(fileId, newFileId));
        } catch (Exception e) {
            log.error("WPS 保存失败 fileId={}", fileId, e);
            return err(50001, "save error");
        }
    }

    // ---------------- 重命名 / 历史版本 ----------------

    @PreAuth("permitAll()")
    @PutMapping("/files/{fileId}/name")
    @Operation(summary = "WPS-重命名")
    public ResponseEntity<Map<String, Object>> rename(HttpServletRequest req, @PathVariable String fileId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        softCheck(req, fileId);
        String name = (body != null && body.get("name") != null) ? String.valueOf(body.get("name")) : null;
        if (name == null || name.isBlank()) {
            return err(40005, "name required");
        }
        Long af = actualFileId(fileId);
        DocImageFile img = imageOf(af);
        if (img != null) {
            img.setFileName(name);
            imageFileMapper.updateById(img);
        }
        return json(new LinkedHashMap<>());
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/versions")
    @Operation(summary = "WPS-历史版本列表")
    public ResponseEntity<Map<String, Object>> versions(HttpServletRequest req, @PathVariable String fileId,
                                                        @RequestParam(required = false) Integer offset,
                                                        @RequestParam(required = false) Integer limit) {
        softCheck(req, fileId);
        List<DocImageFile> list = versionChain(docIdOf(fileId));
        int from = (offset != null && offset > 0) ? offset : 0;
        int to = list.size();
        if (limit != null && limit > 0) {
            to = Math.min(to, from + limit);
        }
        List<Map<String, Object>> arr = new ArrayList<>();
        for (int i = from; i < to; i++) {
            arr.add(versionItem(fileId, list.get(i), i + 1));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("data", arr);
        r.put("message", "");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r);
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/versions/{version}")
    @Operation(summary = "WPS-指定历史版本信息")
    public ResponseEntity<Map<String, Object>> versionInfo(HttpServletRequest req, @PathVariable String fileId,
                                                           @PathVariable int version) {
        softCheck(req, fileId);
        List<DocImageFile> list = versionChain(docIdOf(fileId));
        int idx = version - 1;
        if (idx < 0 || idx >= list.size()) {
            return err(40005, "version not found");
        }
        return json(versionItem(fileId, list.get(idx), version));
    }

    @PreAuth("permitAll()")
    @GetMapping("/files/{fileId}/versions/{version}/download")
    @Operation(summary = "WPS-指定历史版本下载地址")
    public ResponseEntity<Map<String, Object>> versionDownload(HttpServletRequest req, @PathVariable String fileId,
                                                               @PathVariable int version) {
        softCheck(req, fileId);
        List<DocImageFile> list = versionChain(docIdOf(fileId));
        int idx = version - 1;
        if (idx < 0 || idx >= list.size()) {
            return err(40005, "version not found");
        }
        Long vf = list.get(idx).getFileId();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("url", publicBase(req) + "/doc/file/" + vf + "/content?token=" + signer.sign(String.valueOf(vf)));
        return json(r);
    }

    // ---------------- 附件对象（智能文档/多维表格 插图） ----------------

    /** 上传附件对象：WPS 以 PUT 将附件实体上传，key 为对象ID，name 为附件名 */
    @PreAuth("permitAll()")
    @PutMapping(value = "/object/{key}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "WPS-上传附件对象")
    public ResponseEntity<Map<String, Object>> objectPut(@PathVariable String key,
                                                         @RequestParam(name = "name", required = false) String name,
                                                         @RequestBody byte[] body) {
        if (body == null || body.length == 0) {
            return err(40005, "empty body");
        }
        try {
            Long fileId = fileStore.save(body);
            DocImageFile rec = new DocImageFile();
            rec.setDocId(0L);
            rec.setFileId(fileId);
            rec.setOssFileId(key);
            rec.setFileName(name != null && !name.isBlank() ? name : key);
            rec.setFileSize((long) body.length);
            rec.setDocFileType(4);   // 4 = WPS 附件对象（区别于正文/版本/普通附件）
            imageFileMapper.insert(rec);
            return json(new LinkedHashMap<>());
        } catch (Exception e) {
            log.error("WPS 附件对象上传失败 key={}", key, e);
            return err(50001, "save error");
        }
    }

    /** 附件对象下载地址（外网可达） */
    @PreAuth("permitAll()")
    @GetMapping("/object/{key}/url")
    @Operation(summary = "WPS-附件对象下载地址")
    public ResponseEntity<Map<String, Object>> objectUrl(HttpServletRequest req, @PathVariable String key) {
        DocImageFile rec = objectOf(key);
        if (rec == null || rec.getFileId() == null) {
            return err(40005, "object not found");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("url", publicBase(req) + "/doc/file/" + rec.getFileId() + "/content?token="
            + signer.sign(String.valueOf(rec.getFileId())));
        return json(r);
    }

    /** 拷贝附件对象：key_dict 为 <源key:目标key> */
    @PreAuth("permitAll()")
    @PostMapping("/object/copy")
    @Operation(summary = "WPS-拷贝附件对象")
    public ResponseEntity<Map<String, Object>> objectCopy(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null || body.get("key_dict") == null) {
            return err(40005, "key_dict required");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> kd = (Map<String, Object>) body.get("key_dict");
            for (Map.Entry<String, Object> e : kd.entrySet()) {
                String src = String.valueOf(e.getKey());
                String dst = String.valueOf(e.getValue());
                DocImageFile s = objectOf(src);
                if (s == null || s.getFileId() == null) {
                    continue;
                }
                byte[] content = fileStore.download(s.getFileId()).orElse(null);
                if (content == null) {
                    continue;
                }
                Long nf = fileStore.save(content);
                DocImageFile rec = new DocImageFile();
                rec.setDocId(0L);
                rec.setFileId(nf);
                rec.setOssFileId(dst);
                rec.setFileName(s.getFileName());
                rec.setFileSize((long) content.length);
                rec.setDocFileType(4);
                imageFileMapper.insert(rec);
            }
            return json(new LinkedHashMap<>());
        } catch (Exception ex) {
            log.error("WPS 附件对象拷贝失败", ex);
            return err(50001, "copy error");
        }
    }

    /** 按 key 查附件对象（ossFileId=key, docFileType=4，取最新） */
    private DocImageFile objectOf(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return imageFileMapper.selectOne(Wrappers.<DocImageFile>lambdaQuery()
            .eq(DocImageFile::getOssFileId, key).eq(DocImageFile::getDocFileType, 4)
            .orderByDesc(DocImageFile::getFileId).last("limit 1"));
    }

    // ---------------- 用户信息 ----------------

    @PreAuth("permitAll()")
    @GetMapping("/users")
    @Operation(summary = "WPS-用户信息(批量)")
    public ResponseEntity<Map<String, Object>> users(HttpServletRequest req,
        @RequestParam(name = "user_ids", required = false) List<String> userIds) {
        Map<String, String> uq = userQuery(req);
        String curId = uq.get("userId");
        String curName = uq.get("userName");
        List<Map<String, Object>> arr = new ArrayList<>();
        if (userIds != null) {
            for (String uid : userIds) {
                if (uid == null || uid.isBlank()) {
                    continue;
                }
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("id", uid);
                String name = (uid.equals(curId) && curName != null && !curName.isBlank()) ? curName : nickNameOf(uid);
                u.put("name", name);
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

    /** 文件 8 字段（info/complete/version 共用）：id/name/version/size/create_time/modify_time/creator_id/modifier_id */
    private Map<String, Object> versionData(String fileId, Long af) {
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
        return r;
    }

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
