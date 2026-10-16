package org.springblade.portal.provider;

import org.springblade.portal.vo.ElementDataVO;

/**
 * 门户元素数据 Provider（引擎扩展点）
 * <p>
 * <b>设计蓝本</b>：doc/md/ecology-portal-config-analysis.md 的结论——
 * 「配置决定有什么、显示什么数据、什么样式；<b>代码决定每种元素怎么渲染/怎么取数</b>」。
 * 因此新增一种门户元素类型时：
 * <ol>
 *   <li>本接口新增一个实现类（声明 {@link #type()}），并在
 *       {@code blade_portal_element.element_type} 使用该类型值落配置；</li>
 *   <li>前端元素组件注册表新增对应组件；</li>
 *   <li><b>引擎本身无需改动</b>（注册表自动收集 Spring 容器内全部实现）。</li>
 * </ol>
 * 对应泛微侧：{@code hpelement.frommodule} 决定元素类型，
 * {@code weaver.homepage.HomepageExtShow} 里每种元素一个方法——
 * 本项目把这段「每种元素一个方法」收敛为可插拔 Provider。
 * </p>
 */
public interface IPortalElementDataProvider {

	/**
	 * 元素类型（对应 blade_portal_element.element_type）
	 *
	 * @return 类型标识，如 todo / notice / message
	 */
	String type();

	/**
	 * 加载元素数据。
	 * <p>
	 * <b>实现约定</b>：内部（含跨服务调用）异常必须自行捕获，
	 * 降级返回 {@code success=false} + 空列表，<b>不得向外抛异常</b>，
	 * 以保证「单元素失败不拖垮整页」。
	 * </p>
	 *
	 * @param ctx 取数上下文（元素配置、用户、条数上限等）
	 * @return 元素数据（失败时 success=false）
	 */
	ElementDataVO load(ElementDataContext ctx);
}