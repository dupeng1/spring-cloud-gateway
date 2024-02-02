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
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE;

/**
 * @author Spencer Gibb
 */
/**
 * 网关过滤器工厂，没有参数，在转发请求到服务提供者的时候，会保留host信息，发送原始Host消息头，而不是由HTTP客户端确定的Host消息头，
 * 如果没有配置此过滤器，服务提供者收到的请求header中的host就是网关配置的信息<br>
 * <br>
 * routes:<br>
 *       - id: preserve_host_route<br>
 *         uri: https://example.org<br>
 *         filters:<br>
 *         - PreserveHostHeader<br>
 * <br>
 */
public class PreserveHostHeaderGatewayFilterFactory
		extends AbstractGatewayFilterFactory<Object> {

	public GatewayFilter apply() {
		return apply(o -> {
		});
	}

	public GatewayFilter apply(Object config) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				exchange.getAttributes().put(PRESERVE_HOST_HEADER_ATTRIBUTE, true);
				return chain.filter(exchange);
			}

			@Override
			public String toString() {
				return filterToStringCreator(PreserveHostHeaderGatewayFilterFactory.this)
						.toString();
			}
		};
	}

}
