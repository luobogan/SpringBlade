package org.springblade.workflow.odoc.secret;

import java.util.Map;

/**
 * 秘密等级序数（用于密级收发限制的范围比较）。
 *
 * <p>与字典 {@code wf_odoc_dict(dict_type=secret_level)} 的种子名称对齐：
 * 公开 &lt; 秘密 &lt; 机密 &lt; 绝密。无法识别的名称返回 -1（调用方据此跳过校验，避免误伤）。</p>
 */
public final class OdocSecretLevel {

	private static final Map<String, Integer> ORDER = Map.of(
		"公开", 0, "秘密", 1, "机密", 2, "绝密", 3);

	private OdocSecretLevel() {
	}

	/** 密级名 → 序数(0..3)；无法识别返回 -1 */
	public static int levelOf(String name) {
		if (name == null) {
			return -1;
		}
		Integer v = ORDER.get(name.trim());
		return v == null ? -1 : v;
	}
}
