package org.springblade.doc.vo;

import lombok.Data;

/**
 * 在线编辑器配置（多格式在线编辑 / 预览调度）。
 *
 * <p>三种编辑/预览方案清晰分工、可共存：
 * <ul>
 *   <li><b>Univer</b>（开源，{@code editorType=univer}）：Office 文档（xlsx/xls/csv/docx/doc/pptx/ppt）的在线编辑；</li>
 *   <li><b>WPS SDK</b>（kdocs jssdk，{@code editorType=wps}）：同为 Office 文档的可选在线编辑方案（需 WPS 开放平台 appId/secret）；</li>
 *   <li><b>PDF.js</b>（开源，{@code editorType=pdfjs}）：PDF 文件的只读渲染与预览（{@code preview=true}）。</li>
 * </ul>
 * 加载/保存 URL 均经 {@code DocUrlSigner} 签名（防枚举、带时效）。</p>
 */
@Data
public class DocEditorConfig {

    private Long docId;

    /** edit/view（pdfjs 强制 view） */
    private String mode;

    /** 主文件扩展名（小写，不含点），如 xlsx / pdf —— 前端据此判定方案 */
    private String fileExt;

    /** 文件名（可选） */
    private String fileName;

    // ───────────── 统一字段 ─────────────
    /** 编辑器方案：univer / wps / pdfjs */
    private String editorType;

    /** 是否只读（view 模式或 pdfjs 预览时为 true） */
    private Boolean readOnly;

    /** 签名后的正文内容 URL（编辑器/预览器拉取） */
    private String documentUrl;

    /** 当前用户ID/名称（协作显示用） */
    private String userId;

    private String userName;

    // ───────────── Univer 专属 ─────────────
    /** Univer 文档类型：sheet(表格) / doc(文档) / slide(演示) */
    private String docType;

    /** 签名保护的保存回调 URL（仅可写方案签发） */
    private String saveUrl;

    // ───────────── WPS SDK(kdocs jssdk) 专属 ─────────────
    /** WPS 应用 ID */
    private String wpsAppId;

    /** WPS 文件ID（我方文件标识，作为 WPS 中台 fileId） */
    private String wpsFileId;

    /** 文件名（透传 WPS 中台 fname） */
    private String wpsFileName;

    /** 权限：write / read */
    private String wpsPermission;

    /** 用户ID（透传 WPS 中台 userid） */
    private String wpsUserId;

    /** 待编辑文件地址（签名 content URL，前端透传给 jssdk） */
    private String wpsFileUrl;

    /** 服务端为本次编辑签发的签名（前端透传给 jssdk；需与 WPS 开放平台签名规则一致） */
    private String wpsSignature;

    /** 签名时间戳（秒） */
    private Long wpsTimestamp;

    /** 保存回调 URL（签名） */
    private String wpsSaveUrl;

    /** kdocs jssdk 脚本地址 */
    private String wpsJsSdkUrl;

    /** 业务 token（透传 WebOfficeSDK.init，回调经 X-WebOffice-Token 带回鉴权） */
    private String wpsToken;

    // ───────────── PDF.js 专属 ─────────────
    /** 是否为只读预览（pdfjs 为 true） */
    private Boolean preview;

    /** 是否外部文件(OSS 直链)，前端据此走原生预览 */
    private Integer isExtFile;
}
