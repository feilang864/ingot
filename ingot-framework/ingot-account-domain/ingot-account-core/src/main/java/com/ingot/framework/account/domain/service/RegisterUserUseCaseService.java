package com.ingot.framework.account.domain.service;

import java.time.LocalDateTime;

import com.ingot.framework.account.domain.config.AccountMessageSource;
import com.ingot.framework.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.account.domain.model.enums.EventSource;
import com.ingot.framework.account.domain.model.enums.SecurityEventType;
import com.ingot.framework.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册用户用例实现
 *
 * @author jymot
 * @since 2026-02-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseService implements RegisterUserUseCase {

    private final UserAccountPort userAccountPort;
    private final LockStatePort lockStatePort;
    private final SecurityEventPort securityEventPort;
    private final CredentialSecurityService credentialSecurityService;
    private final PasswordEncoder passwordEncoder;

    private final MessageSourceAccessor message = AccountMessageSource.getAccessor();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount register(RegisterUserCommand command) {
        log.info("注册新用户: {}", command.getUsername());

        // 1. 校验用户名唯一性
        if (userAccountPort.existsByUsername(command.getUsername(), command.getUserType())) {
            throw new IllegalArgumentException(message.getMessage("Account.UsernameExists"));
        }

        // 2. 注册场景：仅校验密码强度，不传 userId（此时用户尚未创建，无法保存历史）
        //    注意：CredentialScene.REGISTER 场景在 validate() 内部会尝试保存历史，
        //    但注册阶段 userId 为 null，这里只需要强度校验，历史由后续步骤手动保存。
        //    因此使用 GENERAL 场景通过 validator 走强度策略
        //    （GENERAL 不触发自动历史保存，具体策略由 PasswordPolicy.getApplicableScenes() 控制）
        CredentialValidateRequest strengthRequest = CredentialValidateRequest.builder()
                .scene(CredentialScene.REGISTER)
                .userId(null)    // 新用户尚无ID，不触发历史保存和过期更新
                .username(command.getUsername())
                .password(command.getPassword())
                .userType(command.getUserType())
                .manualProcessError(false)
                .autoProcessUpdatePasswordLogic(false)
                .build();
        // 注册场景：validate 内部在 userId=null 时不会自动保存历史/更新过期，只校验强度
        credentialSecurityService.validate(strengthRequest); // 失败自动抛出异常

        // 3. 加密密码
        String passwordHash = passwordEncoder.encode(command.getPassword());

        // 4. 构建并保存用户账号
        LocalDateTime now = LocalDateTime.now();
        UserAccount account = UserAccount.builder()
                .userType(command.getUserType())
                .username(command.getUsername())
                .password(passwordHash)
                .nickname(command.getNickname())
                .phone(command.getPhone())
                .email(command.getEmail())
                .avatar(command.getAvatar())
                .mustChangePwd(false)
                .passwordChangedAt(now)
                .enabled(true)
                .locked(false)
                .version(0L)
                .createdAt(now)
                .createdBy(command.getCreatedBy())
                .build();

        UserAccount savedAccount = userAccountPort.save(account);
        Long userId = savedAccount.getId();

        // 5. 初始化锁定状态
        lockStatePort.initialize(userId, command.getUserType());

        // 6. 手动保存初始密码历史 + 更新密码过期时间（userId 已确定）
        credentialSecurityService.savePasswordHistory(userId, passwordHash);
        credentialSecurityService.updatePasswordExpiration(userId);

        // 7. 发布账号创建事件
        securityEventPort.publishEvent(AccountSecurityEvent.builder()
                .userId(userId)
                .userType(command.getUserType())
                .eventType(SecurityEventType.ACCOUNT_CREATED)
                .result(true)
                .source(EventSource.SYSTEM)
                .operatorId(command.getCreatedBy())
                .createdAt(now)
                .build());

        log.info("用户 {} 注册成功，ID: {}", command.getUsername(), userId);
        return savedAccount;
    }
}
