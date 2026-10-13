package org.springblade.workflow.odoc.service.impl;

import org.springblade.workflow.odoc.gbt.Gbt9704BuiltinProfiles;
import org.springblade.workflow.odoc.gbt.Gbt9704DocType;
import org.springblade.workflow.odoc.gbt.Gbt9704Element;
import org.springblade.workflow.odoc.gbt.Gbt9704ElementType;
import org.springblade.workflow.odoc.gbt.Gbt9704Profile;
import org.springblade.workflow.odoc.service.IWfOdocAiService;
import org.springblade.workflow.odoc.vo.OdocLayoutRuleVO;
import org.springblade.workflow.odoc.vo.OdocSampleVO;
import org.springblade.workflow.odoc.vo.OdocSensitiveHitVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 公文 AI 增强（L1）实现：规则库 + 内置范文 + 敏感词，无外部模型依赖。
 */
@Service
public class WfOdocAiServiceImpl implements IWfOdocAiService {

	/**
	 * 敏感词内置中性词库（起步版，仅作机制演示与常见安全泄露提示）。
	 * 正式环境应替换为各单位保密/合规词库（可后续改为配置或 DB 加载）。
	 */
	private static final Map<String, String> SENSITIVE_WORDS = new LinkedHashMap<>();

	static {
		SENSITIVE_WORDS.put("密码", "安全凭证");
		SENSITIVE_WORDS.put("口令", "安全凭证");
		SENSITIVE_WORDS.put("账号", "安全凭证");
		SENSITIVE_WORDS.put("token", "安全凭证");
		SENSITIVE_WORDS.put("内网地址", "网络信息");
		SENSITIVE_WORDS.put("IP地址", "网络信息");
	}

	@Override
	public Gbt9704Profile suggestLayout(String docTypeCode) {
		return Gbt9704BuiltinProfiles.forDocType(docTypeCode);
	}

	@Override
	public List<OdocLayoutRuleVO> layoutRules(String docTypeCode) {
		Gbt9704Profile profile = Gbt9704BuiltinProfiles.forDocType(docTypeCode);
		List<OdocLayoutRuleVO> rules = new ArrayList<>();
		for (Gbt9704Element e : profile.getElements()) {
			OdocLayoutRuleVO vo = new OdocLayoutRuleVO();
			Gbt9704ElementType type = e.getType();
			vo.setElementType(type == null ? null : type.getCode());
			vo.setLabel(type == null ? null : type.getLabel());
			vo.setFontFamily(e.getFontFamily());
			vo.setFontSizePt(e.getFontSizePt());
			vo.setAlign(e.getAlign());
			vo.setXMm(e.getXMm());
			vo.setYMm(e.getYMm());
			vo.setWidthMm(e.getWidthMm());
			vo.setHeightMm(e.getHeightMm());
			vo.setRequired(e.isRequired());
			vo.setDataKey(e.getDataKey());
			rules.add(vo);
		}
		return rules;
	}

	@Override
	public List<OdocSampleVO> samples(String docTypeCode) {
		List<OdocSampleVO> list = new ArrayList<>();
		OdocSampleVO universal = universal();
		if (docTypeCode == null || docTypeCode.isBlank()) {
			list.add(universal);
			return list;
		}
		OdocSampleVO specific = sampleFor(docTypeCode);
		if (specific != null) {
			list.add(specific);
		}
		list.add(universal);
		return list;
	}

	@Override
	public List<OdocSensitiveHitVO> checkSensitive(String text) {
		List<OdocSensitiveHitVO> hits = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return hits;
		}
		String lower = text.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> e : SENSITIVE_WORDS.entrySet()) {
			String word = e.getKey();
			String needle = word.toLowerCase(Locale.ROOT);
			int from = 0;
			int idx;
			while ((idx = lower.indexOf(needle, from)) >= 0) {
				hits.add(new OdocSensitiveHitVO(word, e.getValue(), idx));
				from = idx + needle.length();
			}
		}
		return hits;
	}

	private OdocSampleVO universal() {
		OdocSampleVO vo = new OdocSampleVO();
		vo.setDocType("UNIVERSAL");
		vo.setDocTypeName("通用公文结构");
		vo.setTitle("关于{事由}的{文种}");
		vo.setBody("{主送机关}：\n\n"
			+ "{开头：依据/背景/目的}。\n\n"
			+ "{主体：分条列项阐述，第一层用「一、」，第二层用「（一）」，第三层用「1.」}。\n\n"
			+ "{结尾：惯用语，如「特此通知」「妥否，请批示」}。\n\n"
			+ "{发文机关署名}\n{成文日期}");
		return vo;
	}

	private OdocSampleVO sampleFor(String docTypeCode) {
		String code = docTypeCode.toUpperCase(Locale.ROOT);
		String name = nameOf(code);
		switch (code) {
			case "TONGZHI":
				return build(code, name, "关于印发《{制度名称}》的通知",
					"{主送机关}：\n\n《{制度名称}》已经{会议/审批}通过，现印发给你们，请认真贯彻执行。\n\n{发文机关署名}\n{成文日期}");
			case "QINGSHI":
				return build(code, name, "关于{事项}的请示",
					"{主送机关}：\n\n{缘由}。为解决{问题}，拟{措施}。\n\n妥否，请批示。\n\n{发文机关署名}\n{成文日期}");
			case "BAOGAO":
				return build(code, name, "关于{工作}情况的报告",
					"{主送机关}：\n\n现将{工作}情况报告如下：\n\n一、{基本情况}\n二、{主要做法}\n三、{存在问题}\n四、{下一步打算}\n\n{发文机关署名}\n{成文日期}");
			case "HAN":
				return build(code, name, "关于{事项}的函",
					"{主送机关}：\n\n{事由}。现商请{事项}，请予函复。\n\n{发文机关署名}\n{成文日期}");
			case "PIFU":
				return build(code, name, "关于{事项}的批复",
					"{主送机关}：\n\n你{机关}《{来文标题}》（{文号}）收悉。经研究，批复如下：\n\n{批复意见}。\n\n{发文机关署名}\n{成文日期}");
			default:
				return null;
		}
	}

	private OdocSampleVO build(String code, String name, String title, String body) {
		OdocSampleVO vo = new OdocSampleVO();
		vo.setDocType(code);
		vo.setDocTypeName(name);
		vo.setTitle(title);
		vo.setBody(body);
		return vo;
	}

	private String nameOf(String code) {
		for (Gbt9704DocType t : Gbt9704DocType.values()) {
			if (t.getCode().equalsIgnoreCase(code)) {
				return t.getName();
			}
		}
		return code;
	}
}
