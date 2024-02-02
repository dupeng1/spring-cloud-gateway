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

package org.springframework.cloud.gateway.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties(GatewayProperties.PREFIX)
@Validated
//读取封装配置文件中配置的RouteDefinition、FilterDefinition、PredicationDefinition，即路由信息
public class GatewayProperties {

	/**
	 * Properties prefix.
	 */
	public static final String PREFIX = "spring.cloud.gateway";

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * List of Routes.
	 */
	@NotNull
	@Valid
	private List<RouteDefinition> routes = new ArrayList<>();

	/**
	 * List of filter definitions that are applied to every route.
	 */
	private List<FilterDefinition> defaultFilters = new ArrayList<>();

	private List<MediaType> streamingMediaTypes = Arrays
			.asList(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_STREAM_JSON);

	/**
	 * Option to fail on route definition errors, defaults to true. Otherwise, a warning
	 * is logged.
	 */
	private boolean failOnRouteDefinitionError = true;

	private Metrics metrics = new Metrics();

	public List<RouteDefinition> getRoutes() {
		return routes;
	}

	public void setRoutes(List<RouteDefinition> routes) {
		this.routes = routes;
		if (routes != null && routes.size() > 0 && logger.isDebugEnabled()) {
			logger.debug("Routes supplied from Gateway Properties: " + routes);
		}
	}

	public List<FilterDefinition> getDefaultFilters() {
		return defaultFilters;
	}

	public void setDefaultFilters(List<FilterDefinition> defaultFilters) {
		this.defaultFilters = defaultFilters;
	}

	public List<MediaType> getStreamingMediaTypes() {
		return streamingMediaTypes;
	}

	public void setStreamingMediaTypes(List<MediaType> streamingMediaTypes) {
		this.streamingMediaTypes = streamingMediaTypes;
	}

	public boolean isFailOnRouteDefinitionError() {
		return failOnRouteDefinitionError;
	}

	public void setFailOnRouteDefinitionError(boolean failOnRouteDefinitionError) {
		this.failOnRouteDefinitionError = failOnRouteDefinitionError;
	}

	public Metrics getMetrics() {
		return metrics;
	}

	public void setMetrics(Metrics metrics) {
		this.metrics = metrics;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("routes", routes)
				.append("defaultFilters", defaultFilters)
				.append("streamingMediaTypes", streamingMediaTypes)
				.append("failOnRouteDefinitionError", failOnRouteDefinitionError)
				.append("metrics", metrics).toString();

	}

	public static class Metrics {

		/**
		 * Default metrics prefix.
		 */
		public static final String DEFAULT_PREFIX = "gateway";

		/**
		 * Enables the collection of metrics data.
		 */
		private boolean enabled;

		/**
		 * The prefix of all metrics emitted by gateway.
		 */
		private String prefix = DEFAULT_PREFIX;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("enabled", enabled)
					.append("prefix", prefix).toString();

		}

	}

}
