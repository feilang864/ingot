package com.ingot.framework.account.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 账号域配置属性
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@ConfigurationProperties(prefix = "ingot.account")
public class AccountDomainProperties {

    /**
     * 锁定策略配置
     */
    private LockoutPolicy lockout = new LockoutPolicy();

    @Data
    public static class LockoutPolicy {
        /**
         * 是否启用自动锁定
         */
        private boolean enabled = true;

        /**
         * 失败次数阈值
         */
        private int maxAttempts = 5;

        /**
         * 锁定时长（分钟），0=永久锁定
         */
        private int lockDurationMinutes = 30;

        /**
         * 失败计数窗口期（分钟）
         */
        private int attemptWindowMinutes = 15;
    }
}
