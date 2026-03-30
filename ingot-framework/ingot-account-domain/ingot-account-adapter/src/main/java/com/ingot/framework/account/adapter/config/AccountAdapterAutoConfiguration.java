package com.ingot.framework.account.adapter.config;

import com.ingot.framework.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.account.adapter.mapper.AccountSecurityEventMapper;
import com.ingot.framework.account.adapter.mapper.MapperModule;
import com.ingot.framework.account.adapter.port.DefaultLockStatePortAdapter;
import com.ingot.framework.account.adapter.port.DefaultSecurityEventPortAdapter;
import com.ingot.framework.account.adapter.task.TaskModule;
import com.ingot.framework.account.domain.config.AccountDomainAutoConfiguration;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * account-adapter 模块自动配置
 * <p>
 * 提供基于 MyBatis-Plus 的 LockStatePort 和 SecurityEventPort 具体实现。
 * 通过 {@link AutoConfigureBefore} 确保在 {@link AccountDomainAutoConfiguration} 之前处理，
 * 使 core 模块的 NoOp 回退 Bean 检测到此处已注册的具体实现并自动跳过。
 * </p>
 *
 * @author jymot
 * @since 2026-02-13
 */
@Configuration
@AutoConfigureBefore(AccountDomainAutoConfiguration.class)
@MapperScan(basePackageClasses = MapperModule.class)
@ComponentScan(basePackageClasses = TaskModule.class)
public class AccountAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockStatePort.class)
    public LockStatePort lockStatePort(AccountLockStateMapper lockStateMapper) {
        return new DefaultLockStatePortAdapter(lockStateMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityEventPort.class)
    public SecurityEventPort securityEventPort(AccountSecurityEventMapper securityEventMapper) {
        return new DefaultSecurityEventPortAdapter(securityEventMapper);
    }
}
