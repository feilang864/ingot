package com.ingot.cloud.gateway.filter;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.utils.FingerprintUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>网关全局前置过滤器，负责请求清洗和客户端 IP 标准化</p>
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>清洗外部伪造的内部 Header（{@code In-Inner-From}、{@code X-Client-Real-IP}）</li>
 *     <li>解析客户端真实 IP，标准化后注入 {@code X-Client-Real-IP} Header，
 *         供下游服务和网关过滤器统一读取，避免不同网络层获取的 IP 不一致</li>
 * </ul>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
public class RequestGlobalFilter implements GlobalFilter, Ordered {
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("[Filter] - RequestGlobalFilter - path={}", exchange.getRequest().getPath());

        String clientIp = resolveClientIp(exchange.getRequest());

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove(SecurityConstants.HEADER_FROM);
                    httpHeaders.remove(HeaderConstants.CLIENT_REAL_IP);
                    httpHeaders.set(HeaderConstants.CLIENT_REAL_IP, clientIp);
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (StrUtil.isNotEmpty(ip) && !NetUtil.isUnknown(ip)) {
                return FingerprintUtil.normalizeIp(NetUtil.getMultistageReverseProxyIp(ip));
            }
        }
        if (request.getRemoteAddress() != null) {
            return FingerprintUtil.normalizeIp(request.getRemoteAddress().getAddress().getHostAddress());
        }
        return "";
    }
}
