package com.ingot.framework.commons.model.security;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.Data;

/**
 * <p>Description  : UserDetailsResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:25 下午.</p>
 */
@Data
public class UserDetailsResponse implements Serializable {
    /**
     * 用户类型 {@link UserTypeEnum}
     */
    private String userType;
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 默认登录tenant
     */
    private Long tenant;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 是否启用（true-启用 false-禁用）
     */
    private Boolean enabled;
    /**
     * 是否锁定（true-锁定 false-正常）
     */
    private Boolean locked;
    /**
     * 凭证是否未过期（true-未过期 false-已过期）
     * <p>为 null 时等同于 true</p>
     */
    private Boolean credentialsNonExpired;
    /**
     * 凭证警告码（可为 null）
     * <p>硬过期（credentialsNonExpired=false）时不填；软警告（宽限期/即将过期）时填写对应错误码</p>
     * <p>值参考 {@code CredentialErrorCode.getCode()}，如 "pwd_expired_with_grace"、"pwd_expiring_soon"</p>
     */
    private String credentialWarning;
    /**
     * 凭证警告附加数据（可为 null）
     * <p>当 {@link #credentialWarning} 不为 null 时，携带策略计算出的数字字段，供前端自行组装提示语：</p>
     * <ul>
     *   <li>{@code graceRemaining}：宽限期剩余登录次数（Integer），对应 "pwd_expired_with_grace"</li>
     *   <li>{@code daysLeft}：距过期剩余天数（Long），对应 "pwd_expiring_soon"</li>
     * </ul>
     */
    private Map<String, Object> credentialMeta;
    /**
     * 权限列表，包含roleCode以及authorityCode
     */
    private List<String> scopes;
    /**
     * 可以访问的租户列表
     */
    private List<TenantMainDTO> allows;
}
