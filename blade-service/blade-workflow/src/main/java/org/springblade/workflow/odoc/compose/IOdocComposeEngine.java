package org.springblade.workflow.odoc.compose;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公文套红合成引擎（接口收敛，便于平替 docx4j / 厂商 SDK）。
 *
 * <p>套红 = 用「套红模板（.docx 含红头 + 书签）」承载正文：把表单字段值填入字段书签、
 * 在正文占位书签处插入正文内容，产出套红版 docx。</p>
 */
public interface IOdocComposeEngine {

	/** 提取 docx 内所有书签名（上传模板时扫描用） */
	List<String> extractBookmarks(byte[] docxBytes);

	/**
	 * 合成套红版 docx。
	 *
	 * @param templateBytes    套红模板 docx 字节
	 * @param bodyBytes        正文 docx 字节（可为空，空则仅填字段）
	 * @param bookmarkValues   书签名 → 字段值
	 * @param contentBookmarks 正文占位书签名集合（这些书签处插入正文，而非填字段）
	 * @return 套红版 docx 字节
	 */
	byte[] compose(byte[] templateBytes, byte[] bodyBytes,
				   Map<String, String> bookmarkValues, Set<String> contentBookmarks);
}
