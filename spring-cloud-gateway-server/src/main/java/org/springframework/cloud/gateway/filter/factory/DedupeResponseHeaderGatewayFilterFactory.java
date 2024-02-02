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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/*
Use case: Both your legacy backend and your API gateway add CORS header values. So, your consumer ends up with
          Access-Control-Allow-Credentials: true, true
          Access-Control-Allow-Origin: https://musk.mars, https://musk.mars
(The one from the gateway will be the first of the two.) To fix, add
          DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin

Configuration parameters:
- name
    String representing response header names, space separated. Required.
- strategy
	RETAIN_FIRST - Default. Retain the first value only.
	RETAIN_LAST - Retain the last value only.
	RETAIN_UNIQUE - Retain all unique values in the order of their first encounter.

Example 1
      default-filters:
      - DedupeResponseHeader=Access-Control-Allow-Credentials

Response header Access-Control-Allow-Credentials: true, false
Modified response header Access-Control-Allow-Credentials: true

Example 2
      default-filters:
      - DedupeResponseHeader=Access-Control-Allow-Credentials, RETAIN_LAST

Response header Access-Control-Allow-Credentials: true, false
Modified response header Access-Control-Allow-Credentials: false

Example 3
      default-filters:
      - DedupeResponseHeader=Access-Control-Allow-Credentials, RETAIN_UNIQUE

Response header Access-Control-Allow-Credentials: true, true
Modified response header Access-Control-Allow-Credentials: true
 */

/**
 * @author Vitaliy Pavlyuk
 */
/**
 * 网关过滤器工厂，服务提供方返回的response的header中，如果有的key出线了多个value（例如跨域场景下的Access-Control-Allow-Origin）
 * DedupeResponseHeader过滤器可以将重复的value剔除掉，剔除策略有三种：RETAIN_FIRST (保留第一个，默认), RETAIN_LAST（保留最后一个）,
 * RETAIN_UNIQUE（去重），接受一个name参数和一个可选strategy参数，name可以包含以空格分隔的标题名称列表<br>
 * <br>
 * routes:<br>
 *       - id: dedupe_response_header_route<br>
 *         uri: https://example.org<br>
 *         filters:<br>
 *         - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin, RETAIN_LAST<br>
 * <br>
 * 如果网关CORS逻辑和下游逻辑都添加了响应头Access-Control-Allow-Credentials和Access-Control-Allow-Origin响应头的重复值，这将删除它们
 */
public class DedupeResponseHeaderGatewayFilterFactory extends
		AbstractGatewayFilterFactory<DedupeResponseHeaderGatewayFilterFactory.Config> {

	private static final String STRATEGY_KEY = "strategy";

	public DedupeResponseHeaderGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(NAME_KEY, STRATEGY_KEY);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				return chain.filter(exchange).then(Mono.fromRunnable(
						() -> dedupe(exchange.getResponse().getHeaders(), config)));
			}

			@Override
			public String toString() {
				return filterToStringCreator(
						DedupeResponseHeaderGatewayFilterFactory.this)
								.append(config.getName(), config.getStrategy())
								.toString();
			}
		};
	}

	public enum Strategy {

		/**
		 * Default: Retain the first value only.
		 */
		RETAIN_FIRST,

		/**
		 * Retain the last value only.
		 */
		RETAIN_LAST,

		/**
		 * Retain all unique values in the order of their first encounter.
		 */
		RETAIN_UNIQUE

	}

	void dedupe(HttpHeaders headers, Config config) {
		String names = config.getName();
		Strategy strategy = config.getStrategy();
		if (headers == null || names == null || strategy == null) {
			return;
		}
		for (String name : names.split(" ")) {
			dedupe(headers, name.trim(), strategy);
		}
	}

	private void dedupe(HttpHeaders headers, String name, Strategy strategy) {
		List<String> values = headers.get(name);
		if (values == null || values.size() <= 1) {
			return;
		}
		switch (strategy) {
		case RETAIN_FIRST:
			headers.set(name, values.get(0));
			break;
		case RETAIN_LAST:
			headers.set(name, values.get(values.size() - 1));
			break;
		case RETAIN_UNIQUE:
			headers.put(name, new ArrayList<>(new LinkedHashSet<>(values)));
			break;
		default:
			break;
		}
	}

	public static class Config extends AbstractGatewayFilterFactory.NameConfig {

		private Strategy strategy = Strategy.RETAIN_FIRST;

		public Strategy getStrategy() {
			return strategy;
		}

		public Config setStrategy(Strategy strategy) {
			this.strategy = strategy;
			return this;
		}

	}

}
