/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.gateway.provider.RequestProvider;
import org.springblade.gateway.provider.ResponseProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 内部接口隔离，拒绝外部对 Feign 内部接口的访问。
 * <p>
 * 全部 Feign 接口统一以 {@code /feign/client/} 为路径前缀，仅用于服务间调用。服务间调用经注册中心直连、不经过网关，
 * 故凡是抵达网关且路径命中该前缀的请求必来自外部，一律拒绝，与是否持有合法令牌无关。
 *
 * @author Chill
 */
@Slf4j
@Component
@AllArgsConstructor
public class InnerFilter implements GlobalFilter, Ordered {

	private static final String MSG_INNER_FORBIDDEN = "禁止访问内部接口";
	private static final String FEIGN_CLIENT_PREFIX = "/feign/client";

	private final ObjectMapper objectMapper;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 取裁剪服务名前缀之前的原始请求路径进行判定，避免依赖 StripPrefix 的裁剪结果
		String originalRequestUrl = RequestProvider.getOriginalRequestUrl(exchange);
		if (isInnerRequest(originalRequestUrl)) {
			return forbid(exchange.getResponse());
		}
		return chain.filter(exchange);
	}

	/**
	 * 判断请求路径是否命中 Feign 内部接口前缀
	 *
	 * @param requestUrl 原始请求路径，可能携带查询串
	 * @return 是否为内部接口
	 */
	private boolean isInnerRequest(String requestUrl) {
		if (!StringUtils.hasText(requestUrl)) {
			return false;
		}
		// 查询串不参与前缀判定，避免入参内容误触发拦截
		String path = requestUrl.split("\\?", 2)[0];
		// 全部 Feign 接口固定以 /feign/client 开头，前缀匹配比单段匹配更精确，不误伤业务路径
		return path.contains(FEIGN_CLIENT_PREFIX);
	}

	private Mono<Void> forbid(ServerHttpResponse resp) {
		resp.setStatusCode(HttpStatus.FORBIDDEN);
		resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
		String result = "";
		try {
			result = objectMapper.writeValueAsString(ResponseProvider.response(HttpStatus.FORBIDDEN.value(), MSG_INNER_FORBIDDEN));
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
		}
		DataBuffer buffer = resp.bufferFactory().wrap(result.getBytes(StandardCharsets.UTF_8));
		return resp.writeWith(Flux.just(buffer));
	}

	@Override
	public int getOrder() {
		// 晚于 RequestFilter(-1000) 以取到原始请求路径，早于 AuthFilter(-100) 以免对被拒路径做无谓鉴权
		return -150;
	}

}
