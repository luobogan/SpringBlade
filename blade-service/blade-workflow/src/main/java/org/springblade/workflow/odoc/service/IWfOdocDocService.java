package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.vo.OdocDocInitVO;
import org.springblade.workflow.odoc.vo.OdocEditorConfigVO;

import java.util.List;

/**
 * 公文正文文档服务。
 *
 * <p>一份流程实例对应一份 {@link WfOdocDoc}，每次在线编辑保存/套红/回滚都产生新
 * {@link WfOdocDocVersion}；本服务负责文档的「初始化 - 版本管理 - 在线编辑配置 -
 * 回调落库 - 内容代理」。</p>
 */
public interface IWfOdocDocService {

	/**
	 * 获取或创建正文文档（按流程实例），返回文档 + 版本 + 在线编辑配置。
	 *
	 * @param instanceId 流程实例ID
	 * @param defId      流程定义ID（首次创建时落库）
	 * @param title      标题（首次创建时落库）
	 * @param edit       是否请求可编辑配置（false 仅预览）
	 */
	OdocDocInitVO initDoc(Long instanceId, Long defId, String title, boolean edit);

	/** 文档主记录 */
	WfOdocDoc getDoc(Long docId);

	/** 版本历史（按版本号倒序） */
	List<WfOdocDocVersion> listVersions(Long docId);

	/**
	 * 落库一个新版本（手动上传 / 回调保存共用）。
	 *
	 * @return 新建的版本
	 */
	WfOdocDocVersion saveVersion(Long docId, Long fileId, String fileName, Integer docFileType, String versionDetail);

	/** 回滚到指定版本（克隆该版本文件产生新版本） */
	WfOdocDocVersion rollback(Long docId, Long toVersionId);

	/** 构建 OnlyOffice 编辑/预览配置（未部署 DOCS 时返回 null） */
	OdocEditorConfigVO buildEditorConfig(Long docId, Long versionId, boolean edit);

	/** 取某版本文件字节（内容代理） */
	byte[] content(Long versionId);

	/** 取某版本落库时的文件名（含扩展名），用于下载端点 Content-Disposition */
	String contentFileName(Long versionId);

	/**
	 * 处理 OnlyOffice 回调。
	 *
	 * @param docId    正文ID（回调URL带入）
	 * @param versionId 被编辑的版本ID（回调URL带入）
	 * @param status   OnlyOffice status 字段（2=需保存）
	 * @param url      OnlyOffice 给出的编辑后文件临时下载地址
	 */
	void handleCallback(Long docId, Long versionId, int status, String url);

	/** 按流程实例查正文（提交拦截 / 套红入口用） */
	WfOdocDoc getDocByInstanceId(Long instanceId);

	/** 当前版本（可能为 null） */
	WfOdocDocVersion getCurrentVersion(Long docId);

	/**
	 * 落库套红产物为新版本，并置 {@code hasUsedTemplet=1}（含文档级标记）。
	 *
	 * @param bytes 套红版 docx 字节
	 */
	WfOdocDocVersion saveTaohongVersion(Long docId, byte[] bytes, String fileName,
										Integer docFileType, String versionDetail);

	/**
	 * 取消套红：以最近一个「未套红」版本克隆出新版本并回退，清文档级套红标记。
	 *
	 * @return 回退后的新版本
	 */
	WfOdocDocVersion clearTaohong(Long docId);
}
