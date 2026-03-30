package com.ingot.cloud.auth.event;

import com.ingot.cloud.pms.api.model.dto.auth.LoginRecordDTO;
import com.ingot.cloud.pms.api.rpc.RemotePmsLoginRecordService;
import com.ingot.framework.commons.model.common.AuthFailureDTO;
import com.ingot.framework.commons.model.common.AuthSuccessDTO;
import com.ingot.framework.commons.model.event.LoginSuccessEvent;
import com.ingot.framework.commons.model.event.LoginFailureEvent;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录事件监听器
 * <p>
 * 异步处理登录成功/失败事件，通知 PMS/Member 服务更新登录状态：
 * - 成功：更新 last_login_at、last_login_ip、重置失败计数
 * - 失败：累加失败计数，触发自动锁定策略
 * </p>
 *
 * @author wangchao
 * @since 2023/6/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private final RemotePmsLoginRecordService pmsLoginRecordService;

    @Async
    @Order
    @EventListener(LoginSuccessEvent.class)
    public void onLoginSuccess(LoginSuccessEvent event) {
        AuthSuccessDTO payload = event.payload();
        log.info("[LoginEventListener] 登录成功 username={} userId={} userType={} ip={}",
                payload.getUsername(), payload.getUserId(), payload.getUserType(), payload.getIp());

        // 只处理平台用户（ADMIN），Member 用户暂时不处理
        if (!UserTypeEnum.ADMIN.getValue().equals(payload.getUserType())) {
            log.debug("[LoginEventListener] 非 ADMIN 用户，暂不处理 userType={}", payload.getUserType());
            return;
        }

        try {
            LoginRecordDTO dto = new LoginRecordDTO();
            dto.setSuccess(true);
            dto.setUserId(payload.getUserId());
            dto.setUsername(payload.getUsername());
            dto.setClientIp(payload.getIp());
            dto.setUserType(payload.getUserType());
            dto.setLoginAt(payload.getTime());
            pmsLoginRecordService.record(dto);
        } catch (Exception e) {
            log.error("[LoginEventListener] 通知 PMS 登录成功失败 username={}", payload.getUsername(), e);
        }
    }

    @Async
    @Order
    @EventListener(LoginFailureEvent.class)
    public void onLoginFailure(LoginFailureEvent event) {
        AuthFailureDTO payload = event.payload();
        log.warn("[LoginEventListener] 登录失败 username={} userType={} ip={} reason={}",
                payload.getUsername(), payload.getUserType(), payload.getIp(), payload.getErrorCode());

        // 只处理平台用户（ADMIN），Member 用户待 M5 阶段接入
        if (!UserTypeEnum.ADMIN.getValue().equals(payload.getUserType())) {
            log.debug("[LoginEventListener] 非 ADMIN 用户，暂不处理 userType={}", payload.getUserType());
            return;
        }

        try {
            LoginRecordDTO dto = new LoginRecordDTO();
            dto.setSuccess(false);
            dto.setUsername(payload.getUsername());
            dto.setClientIp(payload.getIp());
            dto.setUserType(payload.getUserType());
            dto.setLoginAt(payload.getTime());
            dto.setFailureReason(payload.getErrorCode());

            Long tenantId = null;
            if (payload.getTenantId() != null) {
                try {
                    tenantId = Long.parseLong(payload.getTenantId());
                } catch (NumberFormatException ignored) {
                }
            }
            dto.setTenantId(tenantId);

            pmsLoginRecordService.record(dto);
        } catch (Exception e) {
            log.error("[LoginEventListener] 通知 PMS 登录失败失败 username={}", payload.getUsername(), e);
        }
    }
}
