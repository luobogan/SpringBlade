package org.springblade.workflow.odoc.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.service.IWfOdocFontService;
import org.springblade.workflow.odoc.vo.OdocFontStatusVO;
import org.springframework.stereotype.Service;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 国标字体检测实现。
 *
 * <p>通过 AWT 枚举本机已安装字体族；无头服务器亦可枚举（读 fontconfig）。</p>
 */
@Slf4j
@Service
public class WfOdocFontServiceImpl implements IWfOdocFontService {

	/** GB/T 9704-2012 公文字体：字体名 → 用途；required=标题/正文必需 */
	private static final List<Object[]> SPEC = Arrays.asList(
		new Object[]{"方正小标宋简体", "标题（2号）", true},
		new Object[]{"仿宋_GB2312", "正文（3号）", true},
		new Object[]{"黑体", "一级标题", false},
		new Object[]{"楷体_GB2312", "二级标题", false},
		new Object[]{"宋体", "通用/页码", false}
	);

	@Override
	public List<OdocFontStatusVO> checkFonts() {
		Set<String> installed = installedFonts();
		List<OdocFontStatusVO> result = new java.util.ArrayList<>();
		for (Object[] s : SPEC) {
			String font = (String) s[0];
			String usage = (String) s[1];
			boolean required = (Boolean) s[2];
			boolean available = contains(installed, font);
			OdocFontStatusVO vo = new OdocFontStatusVO();
			vo.setFont(font);
			vo.setUsage(usage);
			vo.setRequired(required);
			vo.setAvailable(available);
			vo.setNote(available ? "已安装"
				: (required ? "缺失（必需）：转版式后该要素将字形回退、不符国标，请安装" : "缺失（建议）：影响对应要素字形"));
			result.add(vo);
		}
		return result;
	}

	@Override
	public boolean allRequiredAvailable() {
		Set<String> installed = installedFonts();
		for (Object[] s : SPEC) {
			if ((Boolean) s[2] && !contains(installed, (String) s[0])) {
				return false;
			}
		}
		return true;
	}

	private Set<String> installedFonts() {
		Set<String> names = new LinkedHashSet<>();
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			names.addAll(Arrays.asList(ge.getAvailableFontFamilyNames()));
			names.addAll(Arrays.asList(ge.getAllFonts() == null ? new String[0]
				: Arrays.stream(ge.getAllFonts()).map(java.awt.Font::getFontName).toArray(String[]::new)));
		} catch (Throwable t) {
			log.warn("枚举本机字体失败（无头/受限环境）：{}", t.getMessage());
		}
		return names;
	}

	/** 兼容字体名带后缀/别名的包含匹配 */
	private boolean contains(Set<String> installed, String font) {
		if (installed.contains(font)) {
			return true;
		}
		String key = font.replace("_GB2312", "").replace(" ", "");
		for (String n : installed) {
			if (n == null) {
				continue;
			}
			String nn = n.replace("_GB2312", "").replace(" ", "");
			if (nn.equalsIgnoreCase(key)) {
				return true;
			}
		}
		return false;
	}
}
