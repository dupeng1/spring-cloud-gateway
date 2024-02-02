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

import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/**
 * @author Spencer Gibb
 */
public class RemoveRequestHeaderGatewayFilterFactory
		extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {

	public RemoveRequestHeaderGatewayFilterFactory() {
		super(NameConfig.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(NAME_KEY);
	}

	@Override
	public GatewayFilter apply(NameConfig config) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				ServerHttpRequest request = exchange.getRequest().mutate()
						.headers(httpHeaders -> httpHeaders.remove(config.getName()))
						.build();

				return chain.filter(exchange.mutate().request(request).build());
			}

			@Override
			public String toString() {
				return filterToStringCreator(RemoveRequestHeaderGatewayFilterFactory.this)
						.append("name", config.getName()).toString();
			}
		};
	}

}
