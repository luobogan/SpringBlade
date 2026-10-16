package org.springblade.workflow.odoc.layout.ofd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.workflow.spi.IOdocFormatConvertor;
import org.springblade.workflow.spi.OdocConvertResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * OFD/PDF 厂商转换器（L2 扩展点，@Order(0) 优先于内置渲染器）。
 *
 * <p>仅当配置 {@code odoc.layout.vendor} 且非 none/builtin 时才视为「可用」；未配置或
 * 配置为 none 时本 bean 要么不存在、要么 {@link #available()} 返回 false，由 blade-workflow
 * 的内置渲染器（{@code BuiltinPdfConvertor}）兜底，保证离线也能出 PDF。</p>
 *
 * <p>真实转换需接入所选厂商 SDK（数科/永中/福昕/福昕GSDK）：引入 SDK 依赖后，
 * 在 {@link #toLayout} 内调用其 docx→PDF/OFD 能力。当前为脚手架，未接入 SDK 时
 * 返回明确失败信息（不会静默出错）。</p>
 */
@Component
@Order(0)
@ConditionalOnProperty(prefix = "odoc.layout", name = "vendor")
public class OfdVendorConvertor implements IOdocFormatConvertor {

	private static final Logger log = LoggerFactory.getLogger(OfdVendorConvertor.class);

	@Value("${odoc.layout.vendor:}")
	private String vendor;

	@Override
	public boolean available() {
		return vendor != null && !vendor.isBlank()
			&& !"none".equalsIgnoreCase(vendor) && !"builtin".equalsIgnoreCase(vendor);
	}

	@Override
	public boolean supports(String format) {
		// 真实厂商 SDK 通常同时支持 PDF(12) 与 OFD(13)
		return true;
	}

	@Override
	public String name() {
		return vendor;
	}

	@Override
	public OdocConvertResult toLayout(byte[] docxBytes, String format, String fileName) {
		log.warn("OfdVendorConvertor 被调用但厂商 SDK 尚未接入 vendor={} format={}", vendor, format);
		return OdocConvertResult.fail(
			"厂商[" + vendor + "] 的 docx→" + ("13".equals(format) ? "OFD" : "PDF") + " 转换 SDK 尚未接入，"
				+ "请在 blade-workflow-odoc-layout 的 OfdVendorConvertor#toLayout 中实现");
	}
}
