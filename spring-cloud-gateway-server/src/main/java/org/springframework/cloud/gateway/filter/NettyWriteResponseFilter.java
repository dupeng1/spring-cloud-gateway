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

import java.util.List;

import io.netty.buffer.ByteBuf;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;

/**
 * @author Spencer Gibb
 */

/**
 * NettyWriteResponseFilter，与NettyRoutingFilter成对使用的网关过滤器，其将NettyRoutingFilter请求后端Http服务的响应回写客户端
 */
public class NettyWriteResponseFilter implements GlobalFilter, Ordered {

	/**
	 * Order for write response filter.
	 */
	public static final int WRITE_RESPONSE_FILTER_ORDER = -1;

	private static final Log log = LogFactory.getLog(NettyWriteResponseFilter.class);

	private final List<MediaType> streamingMediaTypes;

	public NettyWriteResponseFilter(List<MediaType> streamingMediaTypes) {
		this.streamingMediaTypes = streamingMediaTypes;
	}

	@Override
	public int getOrder() {
		return WRITE_RESPONSE_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// NOTICE: nothing in "pre" filter stage as CLIENT_RESPONSE_CONN_ATTR is not added
		// until the NettyRoutingFilter is run
		// @formatter:off
		return chain.filter(exchange)
				.doOnError(throwable -> cleanup(exchange))
				.then(Mono.defer(() -> {
					//获得NettyRoutingFilter返回的Response
					Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);

					if (connection == null) {
						//如果NettyRoutingFilter返回的connection为null，直接返回空
						return Mono.empty();
					}
					if (log.isTraceEnabled()) {
						log.trace("NettyWriteResponseFilter start inbound: "
								+ connection.channel().id().asShortText() + ", outbound: "
								+ exchange.getLogPrefix());
					}
					//获得http response
					ServerHttpResponse response = exchange.getResponse();

					// TODO: needed?
					//获得connection的inbound数据流，并转成DataBuffer类型元素
					final Flux<DataBuffer> body = connection
							.inbound()
							.receive()
							.retain()
							.map(byteBuf -> wrap(byteBuf, response));

					//获取响应内容类型
					MediaType contentType = null;
					try {
						contentType = response.getHeaders().getContentType();
					}
					catch (Exception e) {
						if (log.isTraceEnabled()) {
							log.trace("invalid media type", e);
						}
					}
					//使用http response输出流或文本
					return (isStreamingMediaType(contentType)
							? response.writeAndFlushWith(body.map(Flux::just))
							: response.writeWith(body));
				})).doOnCancel(() -> cleanup(exchange));
		// @formatter:on
	}

	//把netty请求的ByteBuf转成DataBuffer
	protected DataBuffer wrap(ByteBuf byteBuf, ServerHttpResponse response) {
		DataBufferFactory bufferFactory = response.bufferFactory();
		if (bufferFactory instanceof NettyDataBufferFactory) {
			NettyDataBufferFactory factory = (NettyDataBufferFactory) bufferFactory;
			return factory.wrap(byteBuf);
		}
		// MockServerHttpResponse creates these
		else if (bufferFactory instanceof DefaultDataBufferFactory) {
			DataBuffer buffer = ((DefaultDataBufferFactory) bufferFactory)
					.allocateBuffer(byteBuf.readableBytes());
			buffer.write(byteBuf.nioBuffer());
			byteBuf.release();
			return buffer;
		}
		throw new IllegalArgumentException(
				"Unkown DataBufferFactory type " + bufferFactory.getClass());
	}

	private void cleanup(ServerWebExchange exchange) {
		Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
		if (connection != null && connection.channel().isActive()
				&& !connection.isPersistent()) {
			connection.dispose();
		}
	}

	// TODO: use framework if possible
	private boolean isStreamingMediaType(@Nullable MediaType contentType) {
		if (contentType != null) {
			for (int i = 0; i < streamingMediaTypes.size(); i++) {
				if (streamingMediaTypes.get(i).isCompatibleWith(contentType)) {
					return true;
				}
			}
		}
		return false;
	}

}
