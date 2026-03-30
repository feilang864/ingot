package com.ingot.framework.account.domain.port.outbound;

import com.ingot.framework.account.domain.model.AccountSecurityEvent;

import java.util.List;

/**
 * 安全事件发布端口
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface SecurityEventPort {

    /**
     * 发布账号安全事件
     *
     * @param event 安全事件
     */
    void publishEvent(AccountSecurityEvent event);

    /**
     * 批量发布事件
     *
     * @param events 事件列表
     */
    void publishBatch(List<AccountSecurityEvent> events);
}
