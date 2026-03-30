package com.ingot.framework.security.oauth2.jwt;

import java.util.ArrayList;

import cn.hutool.core.map.MapUtil;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/**
 * <p>Description  : {@link JwtClaimNames}的扩展，并且包含之前的常量.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/18.</p>
 * <p>Time         : 2:19 下午.</p>
 */
public interface JwtClaimNamesExtension {

    /**
     * {@code iss} - the Issuer claim identifies the principal that issued the JWT
     */
    String ISS = JwtClaimNames.ISS;

    /**
     * {@code sub} - the Subject claim identifies the principal that is the subject of the
     * JWT
     */
    String SUB = JwtClaimNames.SUB;

    /**
     * {@code aud} - the Audience claim identifies the recipient(s) that the JWT is
     * intended for
     */
    String AUD = JwtClaimNames.AUD;

    /**
     * {@code exp} - the Expiration time claim identifies the expiration time on or after
     * which the JWT MUST NOT be accepted for processing
     */
    String EXP = JwtClaimNames.EXP;

    /**
     * {@code nbf} - the Not Before claim identifies the time before which the JWT MUST
     * NOT be accepted for processing
     */
    String NBF = JwtClaimNames.NBF;

    /**
     * {@code iat} - The Issued at claim identifies the time at which the JWT was issued
     */
    String IAT = JwtClaimNames.IAT;

    /**
     * {@code jti} - The JWT ID claim provides a unique identifier for the JWT
     */
    String JTI = JwtClaimNames.JTI;

    /**
     * ID
     */
    String ID = "i";
    /**
     * 租户 ID
     */
    String TENANT = "org";
    /**
     * 认证类型
     */
    String AUTH_TYPE = "tat";
    /**
     * 用户类型
     */
    String USER_TYPE = "ut";
    String SCOPE = OAuth2ParameterNames.SCOPE;

    /**
     * 凭证警告码（仅当密码即将过期或处于宽限期时写入）
     * <p>值参考 {@code CredentialErrorCode.getCode()}：</p>
     * <ul>
     *   <li>{@code "pwd_expiring_soon"}：密码即将过期</li>
     *   <li>{@code "pwd_expired_with_grace"}：密码已过期但处于宽限期</li>
     * </ul>
     */
    String CREDENTIAL_WARNING = "cw";

    /**
     * 宽限期剩余登录次数（Integer，对应 {@code "pwd_expired_with_grace"}）
     */
    String CREDENTIAL_GRACE_REMAINING = "cgr";

    /**
     * 距密码过期剩余天数（Long，对应 {@code "pwd_expiring_soon"}）
     */
    String CREDENTIAL_EXPIRE_IN_DAYS = "ced";

    /**
     * credentialMeta Map 中：宽限期剩余次数的 key（与 {@code PasswordExpirationPolicy.META_GRACE_REMAINING} 保持一致）
     */
    String CREDENTIAL_META_KEY_GRACE_REMAINING = "graceRemaining";

    /**
     * credentialMeta Map 中：距过期剩余天数的 key（与 {@code PasswordExpirationPolicy.META_DAYS_LEFT} 保持一致）
     */
    String CREDENTIAL_META_KEY_DAYS_LEFT = "daysLeft";

    static String getUsername(Jwt source) {
        return MapUtil.get(source.getClaims(), SUB, String.class);
    }

    static Long getId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.ID, Long.class);
    }

    static Long getTenantId(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.TENANT, Long.class);
    }

    static String getAuthType(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.AUTH_TYPE, String.class);
    }

    static String getUserType(Jwt source) {
        return MapUtil.get(source.getClaims(), JwtClaimNamesExtension.USER_TYPE, String.class);
    }

    @SuppressWarnings("unchecked")
    static String getAud(Jwt source) {
        ArrayList<String> clientIds = MapUtil.get(source.getClaims(), AUD, ArrayList.class);
        return clientIds.get(0);
    }
}
