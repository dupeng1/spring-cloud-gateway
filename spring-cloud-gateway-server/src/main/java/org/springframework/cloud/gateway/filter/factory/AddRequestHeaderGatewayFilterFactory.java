/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.filter.factory;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/**
 * @author Spencer Gibb
 */
/**
 * 网关过滤器工厂，在请求头部添加参数，需要name和value参数<br>
 * <br>
 * routes:<br>
 *       - id: add_request_header_route<br>
 *         uri: https://example.org<br>
 *         filters:<br>
 *         - AddRequestHeader=X-Request-red, blue<br>
 * <br>
 * 示例表示将X-Request-red：blue消息头添加到所有匹配请求的下游请求消息头中
 */
public class AddRequestHeaderGatewayFilterFactory
		extends AbstractNameValueGatewayFilterFactory {

	@Override
	public GatewayFilter apply(NameValueConfig config) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				String value = ServerWebExchangeUtils.expand(exchange, config.getValue());
				ServerHttpRequest request = exchange.getRequest().mutate()
						.headers(httpHeaders -> httpHeaders.add(config.getName(), value))
						.build();

				return chain.filter(exchange.mutate().request(request).build());
			}

			@Override
			public String toString() {
				return filterToStringCreator(AddRequestHeaderGatewayFilterFactory.this)
						.append(config.getName(), config.getValue()).toString();
			}
		};
	}

}
