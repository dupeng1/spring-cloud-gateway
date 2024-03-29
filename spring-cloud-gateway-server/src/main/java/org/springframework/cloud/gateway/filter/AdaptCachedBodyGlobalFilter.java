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

package org.springframework.cloud.gateway.filter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 将请求体缓存到网关中，以便再后续的请求中重复使用，这个过滤器通常用于优化跨集群的服务通信<br>
 * 服务之间的通信可能需要经过网关进行路由和转发，在某些情况下，多个服务可能需要对同一个请求体进行处理，但由于HTTP请求特性，请求体在传递
 * 过程中只能被读取一次，为了解决这个问题，通过缓存请求体的方式，使得多个服务可以在后续的请求中重复使用该请求体<br>
 * 通过缓存请求体，可以提高服务之间的通信效率，减少不必要的数据传输和处理，它可以避免在每个服务中都重新读取请求体，从而节省了网络带宽
 * 和服务资源
 */
public class AdaptCachedBodyGlobalFilter
		implements GlobalFilter, Ordered, ApplicationListener<EnableBodyCachingEvent> {

	private ConcurrentMap<String, Boolean> routesToCache = new ConcurrentHashMap<>();

	/**
	 * Cached request body key.
	 */
	@Deprecated
	public static final String CACHED_REQUEST_BODY_KEY = CACHED_REQUEST_BODY_ATTR;

	@Override
	public void onApplicationEvent(EnableBodyCachingEvent event) {
		this.routesToCache.putIfAbsent(event.getRouteId(), true);
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// the cached ServerHttpRequest is used when the ServerWebExchange can not be
		// mutated, for example, during a predicate where the body is read, but still
		// needs to be cached.
		ServerHttpRequest cachedRequest = exchange
				.getAttributeOrDefault(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR, null);
		if (cachedRequest != null) {
			exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
			return chain.filter(exchange.mutate().request(cachedRequest).build());
		}

		//
		DataBuffer body = exchange.getAttributeOrDefault(CACHED_REQUEST_BODY_ATTR, null);
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

		if (body != null || !this.routesToCache.containsKey(route.getId())) {
			return chain.filter(exchange);
		}
		//此处是缓存过滤器的核心，在此工具方法中会将缓存存入网关上下文中
		return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
			// don't mutate and build if same request object
			if (serverHttpRequest == exchange.getRequest()) {
				return chain.filter(exchange);
			}
			return chain.filter(exchange.mutate().request(serverHttpRequest).build());
		});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1000;
	}

}
