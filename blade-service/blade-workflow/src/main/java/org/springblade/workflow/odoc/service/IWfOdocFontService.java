package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.vo.OdocFontStatusVO;

import java.util.List;

/**
 * 国标字体检测（信创适配）。
 *
 * <p>GB/T 9704-2012 强制使用特定公文字体（方正小标宋简体作标题、仿宋_GB2312 作正文等）。
 * 信创/内网环境常缺失这些字体，是版式渲染「最大坑」；本服务在服务器侧自检字体是否安装，
 * 缺字体时前置暴露，避免转版式后字形回退、不符国标。</p>
 */
public interface IWfOdocFontService {

	/** 检测 GB/T 9704 必需/常用字体在本机是否可用 */
	List<OdocFontStatusVO> checkFonts();

	/** 是否所有「必需」字体都可用 */
	boolean allRequiredAvailable();
}
