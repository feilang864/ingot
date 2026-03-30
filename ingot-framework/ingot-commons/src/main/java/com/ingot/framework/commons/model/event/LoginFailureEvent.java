package com.ingot.framework.commons.model.event;

import com.ingot.framework.commons.model.common.AuthFailureDTO;

/**
 * 登录失败事件（仅指密码错误等用户认证失败，不包含客户端认证失败）
 *
 * @author jymot
 * @since 2026-02-13
 */
public record LoginFailureEvent(AuthFailureDTO payload) {
}
