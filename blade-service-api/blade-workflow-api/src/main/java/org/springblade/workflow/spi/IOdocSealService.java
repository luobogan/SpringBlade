package org.springblade.workflow.spi;

/**
 * 电子签章 SPI（L2）。
 *
 * <p>对版式文件（PDF/OFD）进行 CA 签章与验章（含 RFC3161 时间戳、篡改检测）。
 * 实现放在可选库模块 {@code blade-workflow-odoc-layout}（契约锁/CA 等），
 * {@code @ConditionalOnProperty} + {@link #available()} 装配；未装配时前端隐藏签章入口。</p>
 */
public interface IOdocSealService {

	boolean available();

	String name();

	/** 对版式文件签章 */
	OdocSealResult sign(byte[] layoutBytes, OdocSealRequest request);

	/** 验章 + 篡改检测 */
	OdocVerifyResult verify(byte[] layoutBytes);
}
