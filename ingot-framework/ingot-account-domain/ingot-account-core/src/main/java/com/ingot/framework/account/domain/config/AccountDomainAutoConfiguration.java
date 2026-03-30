package com.ingot.framework.account.domain.config;

import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.account.domain.port.outbound.UserCredentialPort;
import com.ingot.framework.account.domain.port.outbound.noop.NoOpLockStatePort;
import com.ingot.framework.account.domain.port.outbound.noop.NoOpSecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.noop.NoOpUserAccountPort;
import com.ingot.framework.account.domain.port.outbound.noop.NoOpUserCredentialPort;
import com.ingot.framework.account.domain.service.AccountUseCaseModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 账号域自动配置
 * <p>
 * UseCase 实现通过 {@code @Service} 自动注入，
 * Port 接口的 NoOp 实现在没有具体实现时作为默认值生效。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Configuration
@ComponentScan(basePackageClasses = AccountUseCaseModule.class)
@EnableConfigurationProperties(AccountDomainProperties.class)
public class AccountDomainAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserAccountPort.class)
    public UserAccountPort noOpUserAccountPort() {
        return new NoOpUserAccountPort();
    }

    @Bean
    @ConditionalOnMissingBean(UserCredentialPort.class)
    public UserCredentialPort noOpUserCredentialPort() {
        return new NoOpUserCredentialPort();
    }

    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort noOpLockStatePort() {
        return new NoOpLockStatePort();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventPort.class)
    public SecurityEventPort noOpSecurityEventPort() {
        return new NoOpSecurityEventPort();
    }
}
