package org.springblade.doc.online;

import org.springblade.doc.vo.DocEditorConfig;

/**
 * 在线编辑器抽象（多格式调度）：把「正文内容 URL + 回调」组装成编辑器配置。
 *
 * <p>实现：{@link UniverDocOnlineEditor}（Univer，Office 编辑）、
 * {@link WpsDocOnlineEditor}（WPS kdocs jssdk，Office 编辑可选方案）。
 * PDF 预览由控制器直接组装（pdfjs，无实现类）。</p>
 *
 * @param editorType 期望方案：univer / wps（控制器据文件扩展名 + 入参决定；实现内部可忽略或断言）
 */
public interface IDocOnlineEditor {

    DocEditorConfig buildConfig(Long docId, String mode, String editorType);

    /** 预览指定附件文件（而非文档主文件）：fileId 即附件 fileId，通常为只读预览 */
    DocEditorConfig buildConfigFromFile(Long docId, String mode, String editorType, Long fileId);
}
