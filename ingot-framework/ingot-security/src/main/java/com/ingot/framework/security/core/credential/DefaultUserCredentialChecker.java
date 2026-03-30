package com.ingot.framework.security.core.credential;

import com.ingot.framework.security.core.InSecurityMessageSource;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

/**
 * 默认凭证检查器
 * <p>
 * 负责 PASSWORD 授权模式下的密码验证。这是框架内置的基础实现，
 * 服务层可继承本类并在 {@code super.check()} 调用后追加更多策略检查
 * （如密码过期、强制改密等），注册为 Spring Bean 后框架会自动替换本默认实现。
 * <p>
 *
 * @author jymot
 * @see UserCredentialChecker
 * @since 2026-02-13
 */
@Slf4j
public class DefaultUserCredentialChecker implements UserCredentialChecker {

    protected MessageSourceAccessor messages;

    private PasswordEncoder passwordEncoder;

    public DefaultUserCredentialChecker() {
        setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        setMessageSource(new InSecurityMessageSource());
    }

    /**
     * 设置 PasswordEncoder
     */
    @Override
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 设置 MessageSource
     */
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 仅 PASSWORD 授权模式执行密码验证，其他模式直接通过。
     */
    @Override
    public void check(UserDetails user, OAuth2Authentication token) {
        if (token.getGrantType() != InAuthorizationGrantType.PASSWORD) {
            return;
        }

        if (token.getCredentials() == null) {
            log.debug("Failed to authenticate since no credentials provided");
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        String presentedPassword = token.getCredentials().toString();
        if (!this.passwordEncoder.matches(presentedPassword, user.getPassword())) {
            log.debug("Failed to authenticate since password does not match stored value");
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    protected PasswordEncoder getPasswordEncoder() {
        return this.passwordEncoder;
    }
}
