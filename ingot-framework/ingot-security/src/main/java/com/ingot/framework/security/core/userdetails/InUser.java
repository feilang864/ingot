package com.ingot.framework.security.core.userdetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.RoleUtil;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * <p>Description  : 自定义User.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/28.</p>
 * <p>Time         : 12:54 PM.</p>
 */
@Getter
public class InUser extends User implements InUserDetails {
    private static final String N_A = "N/A";

    /**
     * 用户ID
     */
    private final Long id;
    /**
     * 租户ID
     */
    private final Long tenantId;
    /**
     * 登录客户端ID
     */
    private final String clientId;
    /**
     * Token认证类型 {@link TokenAuthTypeEnum}
     */
    private final String tokenAuthType;
    /**
     * 用户类型 {@link UserTypeEnum}
     */
    private final String userType;
    /**
     * 凭证警告码
     * <p>由 PMS/Member 在 getUserAuthDetails 时填充（如"pwd_expiring_soon"、"pwd_expired_with_grace"），
     * 通过 {@code OAuth2UserDetailsService.parse()} 传入，后续请求中此字段为 null</p>
     */
    private final String credentialWarning;
    /**
     * 凭证警告附加数据
     * <p>当 {@link #credentialWarning} 不为 null 时携带，由 PMS/Member 填充，由 JWT 定制器写入 claims：</p>
     * <ul>
     *   <li>{@code graceRemaining}（Integer）：宽限期剩余登录次数，对应 "pwd_expired_with_grace"</li>
     *   <li>{@code daysLeft}（Long）：距过期剩余天数，对应 "pwd_expiring_soon"</li>
     * </ul>
     */
    private final Map<String, Object> credentialMeta;

    @JsonCreator
    public InUser(Long id,
                  Long tenantId,
                  String clientId,
                  String tokenAuthType,
                  String userType,
                  String username,
                  String password,
                  boolean enabled,
                  boolean accountNonExpired,
                  boolean credentialsNonExpired,
                  boolean accountNonLocked,
                  Collection<? extends GrantedAuthority> authorities,
                  String credentialWarning,
                  Map<String, Object> credentialMeta) {
        super(username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.tokenAuthType = tokenAuthType;
        this.clientId = clientId;
        this.userType = userType;
        this.credentialWarning = credentialWarning;
        this.credentialMeta = credentialMeta;
    }

    /**
     * 无状态 UserDetails
     *
     * @return {@link InUser}
     */
    public static InUser stateless(Long id, Long tenantId, String clientId,
                                   String tokenAuthType, String userType, String username,
                                   Collection<? extends GrantedAuthority> authorities) {
        return standard(id, tenantId, clientId, tokenAuthType, userType, username, N_A,
                true, true, true, true,
                authorities);
    }

    public static InUser stateless(Long id, Long tenantId, String clientId,
                                   String tokenAuthType, String userType, String username,
                                   Collection<? extends GrantedAuthority> authorities,
                                   String credentialWarning,
                                   Map<String, Object> credentialMeta) {
        return standard(id, tenantId, clientId, tokenAuthType, userType, username, N_A,
                true, true, true, true,
                authorities, credentialWarning, credentialMeta);
    }

    /**
     * 无客户端信息({@link #clientId}, {@link #tokenAuthType})，
     * 如果可以访问的租户列表中存在主要租户，那么将TenantId设置为主要租户
     *
     * @return {@link InUser}
     */
    public static InUser userDetails(Long id, String userType, Long defaultTenant,
                                     String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities) {
        return standard(id, defaultTenant, N_A, N_A, userType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, null, null);
    }

    public static InUser userDetails(Long id, String userType, Long defaultTenant,
                                     String username, String password,
                                     boolean enabled, boolean accountNonExpired,
                                     boolean credentialsNonExpired, boolean accountNonLocked,
                                     Collection<? extends GrantedAuthority> authorities,
                                     String credentialWarning,
                                     Map<String, Object> credentialMeta) {
        return standard(id, defaultTenant, N_A, N_A, userType, username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, credentialWarning, credentialMeta);
    }

    /**
     * 标准实例化
     *
     * @return {@link InUser}
     */
    public static InUser standard(Long id, Long tenantId, String clientId,
                                  String tokenAuthType, String userType,
                                  String username, String password,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked,
                                  Collection<? extends GrantedAuthority> authorities) {
        return new InUser(id, tenantId, clientId, tokenAuthType, userType,
                username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, null, null);
    }

    public static InUser standard(Long id, Long tenantId, String clientId,
                                  String tokenAuthType, String userType,
                                  String username, String password,
                                  boolean enabled, boolean accountNonExpired,
                                  boolean credentialsNonExpired, boolean accountNonLocked,
                                  Collection<? extends GrantedAuthority> authorities,
                                  String credentialWarning,
                                  Map<String, Object> credentialMeta) {
        return new InUser(id, tenantId, clientId, tokenAuthType, userType,
                username, password,
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
                authorities, credentialWarning, credentialMeta);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return super.isAccountNonExpired();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return super.isAccountNonLocked();
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return super.isCredentialsNonExpired();
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    /**
     * {@link SecurityAuthContext#getRoles()}
     *
     * @return 角色编码列表
     */
    @JsonIgnore
    public List<String> getRoleCodeList() {
        Collection<? extends GrantedAuthority> authorities = SecurityAuthContext.getAuthentication().getAuthorities();
        return InAuthorityUtils.authorityListToScopes(authorities)
                .stream()
                .filter(RoleUtil::isRoleCode)
                .toList();
    }

    public static class Builder {
        private final String password;
        private final String username;
        private final Collection<GrantedAuthority> authorities;
        private final boolean accountNonExpired;
        private final boolean accountNonLocked;
        private final boolean credentialsNonExpired;
        private final boolean enabled;

        private final Long id;
        private Long tenantId;
        private String clientId;
        private String tokenAuthType;
        private String userType;
        private String credentialWarning;
        private Map<String, Object> credentialMeta;

        private Builder(InUser user) {
            this.password = user.getPassword();
            this.username = user.getUsername();
            this.authorities = user.getAuthorities();
            this.accountNonExpired = user.isAccountNonExpired();
            this.accountNonLocked = user.isAccountNonLocked();
            this.credentialsNonExpired = user.isCredentialsNonExpired();
            this.enabled = user.isEnabled();

            this.id = user.getId();
            this.tenantId = user.getTenantId();
            this.clientId = user.getClientId();
            this.tokenAuthType = user.getTokenAuthType();
            this.userType = user.getUserType();
            this.credentialWarning = user.credentialWarning;
            this.credentialMeta = user.credentialMeta;
        }

        public Builder tenantId(Long id) {
            this.tenantId = id;
            return this;
        }

        public Builder clientId(String id) {
            this.clientId = id;
            return this;
        }

        public Builder tokenAuthType(String type) {
            this.tokenAuthType = type;
            return this;
        }

        public Builder userType(String userType) {
            this.userType = userType;
            return this;
        }

        public Builder credentialWarning(String credentialWarning) {
            this.credentialWarning = credentialWarning;
            return this;
        }

        public Builder credentialMeta(Map<String, Object> credentialMeta) {
            this.credentialMeta = credentialMeta;
            return this;
        }

        public InUser build() {
            return InUser.standard(this.id, this.tenantId, this.clientId, this.tokenAuthType,
                    this.userType,
                    this.username, this.password,
                    this.enabled, this.accountNonExpired, this.credentialsNonExpired, this.accountNonLocked,
                    this.authorities, this.credentialWarning, this.credentialMeta);
        }
    }
}
