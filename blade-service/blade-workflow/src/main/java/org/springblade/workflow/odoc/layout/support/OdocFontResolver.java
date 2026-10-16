package org.springblade.workflow.odoc.layout.support;

import org.springblade.workflow.odoc.config.OdocProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 中文字体定位与读取（PDF / OFD 共用）。
 *
 * <p>内置渲染器需嵌入 CJK TTF：默认自动探测 {@code C:/Windows/Fonts} 与常见 Linux 字体目录，
 * 也可经 {@code odoc.pdf-font-path} 显式指定。提供「按文件名取路径」与「读字节流」两种能力，
 * PDF 走 iText 需路径、OFD 需把字体字节写进包体。</p>
 */
public final class OdocFontResolver {

	private OdocFontResolver() {
	}

	/** 定位字体文件绝对路径；找不到返回 null。 */
	public static String resolveFontFile(OdocProperties props, String fileName) {
		List<String> roots = new ArrayList<>();
		String cfg = props != null ? props.getPdfFontPath() : null;
		if (cfg != null && !cfg.isBlank()) {
			roots.add(cfg);
		}
		roots.add("C:/Windows/Fonts");
		roots.add("C:\\Windows\\Fonts");
		roots.add("/usr/share/fonts");
		roots.add("/usr/share/fonts/chinese");
		roots.add("/usr/share/fonts/truetype");
		roots.add("/usr/share/fonts/opentype");
		roots.add("/usr/share/fonts/wqy");
		roots.add("/System/Library/Fonts");
		roots.add("/Library/Fonts");
		for (String r : roots) {
			File direct = new File(r, fileName);
			if (direct.exists()) {
				return direct.getAbsolutePath();
			}
			File found = deepFind(new File(r), fileName, 5);
			if (found != null) {
				return found.getAbsolutePath();
			}
		}
		return null;
	}

	/** 读取字体文件字节（用于 OFD 嵌入）；找不到抛 {@link IllegalStateException}。 */
	public static byte[] readFontBytes(OdocProperties props, String fileName) throws IOException {
		String path = resolveFontFile(props, fileName);
		if (path == null) {
			throw new IllegalStateException("字体文件未找到: " + fileName
				+ "，请配置 odoc.pdf-font-path 指向含 CJK 字体的目录");
		}
		return Files.readAllBytes(new File(path).toPath());
	}

	private static File deepFind(File dir, String fileName, int depth) {
		if (dir == null || !dir.isDirectory() || depth <= 0) {
			return null;
		}
		File[] children = dir.listFiles();
		if (children == null) {
			return null;
		}
		for (File c : children) {
			if (c.isFile() && c.getName().equalsIgnoreCase(fileName)) {
				return c;
			}
		}
		for (File c : children) {
			if (c.isDirectory()) {
				File f = deepFind(c, fileName, depth - 1);
				if (f != null) {
					return f;
				}
			}
		}
		return null;
	}
}
