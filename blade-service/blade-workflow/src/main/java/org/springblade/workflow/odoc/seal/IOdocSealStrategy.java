package org.springblade.workflow.odoc.seal;

import java.util.List;

/**
 * 公文用印策略（决策 D2：自研印章与 e签宝印章并存，按配置启用）。
 *
 * <p>业务代码只依赖本抽象，不感知具体用印实现；当前实现：</p>
 * <ul>
 *   <li>{@code local} —— {@code LocalSealStrategy}：复用既有自研 CA 签章
 *       （{@code IWfOdocSealService}），同步出签章件，不改动现有能力；</li>
 *   <li>{@code esign} —— {@code EsignSealStrategy}：经 e签宝 SaaS API V3 发起签署流程，
 *       异步出签章件（回调/补偿驱动归档）。</li>
 * </ul>
 *
 * <p>两种模式的差异由 {@link OdocSealOutcome#isFinished()} 表达：local 同步完成，
 * esign 发起后即返回「进行中」，签章件在签署完成后由回调或补偿任务取回归档。</p>
 */
public interface IOdocSealStrategy {

	/** 策略标识：local（自研印章） */
	String MODE_LOCAL = "local";

	/** 策略标识：esign（e签宝印章） */
	String MODE_ESIGN = "esign";

	/** 策略标识 */
	String mode();

	/** 当前策略是否可用（依赖的 SPI / 厂商配置是否就绪） */
	boolean available();

	/** 可用印章列表（供前端选章；local 模式返回系统内置章型选项） */
	List<OdocSealOption> listSeals();

	/** 发起用印 */
	OdocSealOutcome seal(OdocSealCmd cmd);

}
