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
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/**
 * @author Spencer Gibb
 */
/**
 * 网关过滤器工厂，重置响应header的值，将指定key改为指定value，如果该key不存在就创建<br>
 * <br>
 * routes:<br>
 *       - id: setresponseheader_route<br>
 *         uri: https://example.org<br>
 *         predicates:<br>
 *         - Path=/foo/{segment}<br>
 *         filters:<br>
 *         - SetRequestHeader=X-Request-Foo, Bar<br>
 * <br>
 * 替换（而不是添加）具有给定名称的所有标头，因此，如果下游服务器以X-Response-Red:1234响应，则将其替换为X-Response-Red:Blue
 */
public class SetResponseHeaderGatewayFilterFactory
		extends AbstractNameValueGatewayFilterFactory {

	@Override
	public GatewayFilter apply(NameValueConfig config) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				String value = ServerWebExchangeUtils.expand(exchange, config.getValue());
				return chain.filter(exchange).then(Mono.fromRunnable(() -> exchange
						.getResponse().getHeaders().set(config.name, value)));
			}

			@Override
			public String toString() {
				return filterToStringCreator(SetResponseHeaderGatewayFilterFactory.this)
						.append(config.getName(), config.getValue()).toString();
			}
		};
	}

}
