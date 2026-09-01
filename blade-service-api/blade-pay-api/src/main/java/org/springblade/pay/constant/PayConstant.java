package org.springblade.pay.constant;

/**
 * 支付模块常量
 */
public interface PayConstant {

	/**
	 * 支付服务名称（Nacos 注册名）
	 */
	String APPLICATION_PAY_NAME = "blade-pay";

	// ==================== 支付状态 ====================

	/** 待支付 */
	String STATUS_PENDING = "PENDING";

	/** 已支付 */
	String STATUS_SUCCESS = "SUCCESS";

	/** 已退款 */
	String STATUS_REFUNDED = "REFUNDED";

	/** 已取消 */
	String STATUS_CANCELLED = "CANCELLED";

}
