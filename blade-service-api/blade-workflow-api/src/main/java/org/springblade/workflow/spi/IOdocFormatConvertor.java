package org.springblade.workflow.spi;

/**
 * 公文版式转换 SPI（L2）。
 *
 * <p>把 docx 正文转换为版式文件（PDF / OFD）。实现放在可选库模块
 * {@code blade-workflow-odoc-layout}（数科/永中/福昕等厂商 SDK），通过
 * {@code @ConditionalOnProperty} + {@link #available()} 装配；未装配时 blade-workflow
 * 判定「版式未启用」，前端隐藏入口、停留 L1。</p>
 *
 * <p>契约为「字节进 / 字节出」，不依赖 blade-workflow 内部服务，厂商实现零耦合。</p>
 */
public interface IOdocFormatConvertor {

	/** 是否可用（未配置/未授权时返回 false，前端据此隐藏按钮） */
	boolean available();

	/** 转换器名称（厂商标识，落库到 wf_odoc_layout_file.vendor） */
	String name();

	/**
	 * 是否支持目标格式（12=PDF / 13=OFD）。默认支持全部；内置渲染器按各自格式返回，
	 * 便于服务按 format 择优（如内置 PDF 处理 12、内置 OFD 处理 13，厂商实现两者皆可）。
	 */
	default boolean supports(String format) {
		return true;
	}

	/**
	 * 转换 docx → 版式文件。
	 *
	 * @param docxBytes 源 docx 字节
	 * @param format    目标格式：12=PDF / 13=OFD
	 * @param fileName  目标文件名（含扩展名）
	 */
	OdocConvertResult toLayout(byte[] docxBytes, String format, String fileName);
}
