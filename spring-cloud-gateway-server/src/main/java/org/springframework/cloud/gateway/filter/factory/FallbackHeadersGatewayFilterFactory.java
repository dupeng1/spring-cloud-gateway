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

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR;

/**
 * @author Olga Maciaszek-Sharma
 * @author Ryan Baxter
 */
/**
 * 网关过滤器工厂，允许在转发到fallbackUri外部应用程序中的请求的标头中添加Spring Cloud Circuit Breaker执行异常详细信息<br>
 * <br>
 * routes:<br>
 *       - id: ingredients-fallback<br>
 *         uri: http://localhost:9994<br>
 *         predicates:<br>
 *         - Path=/fallback<br>
 *         filters:<br>
 *         - name: FallbackHeaders<br>
 *           args:<br>
 *             executionExceptionTypeHeaderName: Test-Header<br>
 * <br>
 * 示例中，在运行断路器时发生执行异常后，请求将转发到fallback运行于 localhost:9994上的应用程序中，
 * 带有异常类型、消息和（如果可用）根本原因异常类型和消息的标头由FallbackHeaders过滤器添加到该请求中，
 * 其它可设置的参数名<br>
 * executionExceptionTypeHeaderName (“Execution-Exception-Type”)<br>
 * executionExceptionMessageHeaderName (“Execution-Exception-Message”)<br>
 * rootCauseExceptionTypeHeaderName (“Root-Cause-Exception-Type”)<br>
 * rootCauseExceptionMessageHeaderName (“Root-Cause-Exception-Message”)<br>
 */
public class FallbackHeadersGatewayFilterFactory
		extends AbstractGatewayFilterFactory<FallbackHeadersGatewayFilterFactory.Config> {

	public FallbackHeadersGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return singletonList(NAME_KEY);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerWebExchange filteredExchange = ofNullable(ofNullable(
					(Throwable) exchange.getAttribute(HYSTRIX_EXECUTION_EXCEPTION_ATTR))
							.orElseGet(() -> exchange.getAttribute(
									CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR)))
											.map(executionException -> {
												ServerHttpRequest.Builder requestBuilder = exchange
														.getRequest().mutate();
												requestBuilder.header(
														config.executionExceptionTypeHeaderName,
														executionException.getClass()
																.getName());
												requestBuilder.header(
														config.executionExceptionMessageHeaderName,
														executionException.getMessage());
												ofNullable(
														getRootCause(executionException))
																.ifPresent(rootCause -> {
																	requestBuilder.header(
																			config.rootCauseExceptionTypeHeaderName,
																			rootCause
																					.getClass()
																					.getName());
																	requestBuilder.header(
																			config.rootCauseExceptionMessageHeaderName,
																			rootCause
																					.getMessage());
																});
												return exchange.mutate()
														.request(requestBuilder.build())
														.build();
											}).orElse(exchange);
			return chain.filter(filteredExchange);
		};
	}

	public static class Config {

		private static final String EXECUTION_EXCEPTION_TYPE = "Execution-Exception-Type";

		private static final String EXECUTION_EXCEPTION_MESSAGE = "Execution-Exception-Message";

		private static final String ROOT_CAUSE_EXCEPTION_TYPE = "Root-Cause-Exception-Type";

		private static final String ROOT_CAUSE_EXCEPTION_MESSAGE = "Root-Cause-Exception-Message";

		private String executionExceptionTypeHeaderName = EXECUTION_EXCEPTION_TYPE;

		private String executionExceptionMessageHeaderName = EXECUTION_EXCEPTION_MESSAGE;

		private String rootCauseExceptionTypeHeaderName = ROOT_CAUSE_EXCEPTION_TYPE;

		private String rootCauseExceptionMessageHeaderName = ROOT_CAUSE_EXCEPTION_MESSAGE;

		public String getExecutionExceptionTypeHeaderName() {
			return executionExceptionTypeHeaderName;
		}

		public void setExecutionExceptionTypeHeaderName(
				String executionExceptionTypeHeaderName) {
			this.executionExceptionTypeHeaderName = executionExceptionTypeHeaderName;
		}

		public String getExecutionExceptionMessageHeaderName() {
			return executionExceptionMessageHeaderName;
		}

		public void setExecutionExceptionMessageHeaderName(
				String executionExceptionMessageHeaderName) {
			this.executionExceptionMessageHeaderName = executionExceptionMessageHeaderName;
		}

		public String getRootCauseExceptionTypeHeaderName() {
			return rootCauseExceptionTypeHeaderName;
		}

		public void setRootCauseExceptionTypeHeaderName(
				String rootCauseExceptionTypeHeaderName) {
			this.rootCauseExceptionTypeHeaderName = rootCauseExceptionTypeHeaderName;
		}

		public String getCauseExceptionMessageHeaderName() {
			return rootCauseExceptionMessageHeaderName;
		}

		public void setCauseExceptionMessageHeaderName(
				String causeExceptionMessageHeaderName) {
			this.rootCauseExceptionMessageHeaderName = causeExceptionMessageHeaderName;
		}

	}

}
