package com.ingot.cloud.bff;

import com.ingot.framework.openapi.EnableOpenAPI;
import com.ingot.framework.security.config.annotation.web.configuration.EnableInWebSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p>BFF（Backend for Frontend）服务启动类</p>
 *
 * <p>为内部前端系统提供安全的会话管理，封装 OAuth2 授权流程，
 * 使前端不接触任何 OAuth2 参数和 JWT，仅通过 HttpOnly Cookie 维持会话。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@EnableFeignClients
@EnableOpenAPI("bff")
@EnableInWebSecurity
@EnableTransactionManagement
@EnableDiscoveryClient
@SpringBootApplication
public class InBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(InBffApplication.class, args);
    }
}
