package com.ingot.framework.security.oauth2.server.authorization.common;

import java.util.Map;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;

/**
 * <p>Description  : OAuth2Util.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/10/16.</p>
 * <p>Time         : 15:26.</p>
 */
public class OAuth2Util {

    public static void setupLoginInfo(Map<String, Object> additionalParameters, String org, InUser user) {
        Optional.ofNullable(user)
                .ifPresent(target -> {
                    additionalParameters.put(InOAuth2ParameterNames.USERNAME, target.getUsername());
                    if (target.getId() != null) {
                        additionalParameters.put(InOAuth2ParameterNames.USER_ID, target.getId());
                    }
                    if (StrUtil.isNotEmpty(target.getUserType())) {
                        additionalParameters.put(InOAuth2ParameterNames.USER_TYPE, target.getUserType());
                    }
                    if (target.getTenantId() != null) {
                        additionalParameters.put(InOAuth2ParameterNames.TENANT, target.getTenantId().toString());
                    }
                    if (StrUtil.isNotEmpty(target.getCredentialWarning())) {
                        additionalParameters.put(InOAuth2ParameterNames.CREDENTIAL_WARNING, target.getCredentialWarning());
                        Map<String, Object> meta = user.getCredentialMeta();
                        if (meta != null) {
                            Object graceRemaining = meta.get(JwtClaimNamesExtension.CREDENTIAL_META_KEY_GRACE_REMAINING);
                            if (graceRemaining != null) {
                                additionalParameters.put(InOAuth2ParameterNames.CREDENTIAL_GRACE_REMAINING, graceRemaining);
                            }
                            Object daysLeft = meta.get(JwtClaimNamesExtension.CREDENTIAL_META_KEY_DAYS_LEFT);
                            if (daysLeft != null) {
                                additionalParameters.put(InOAuth2ParameterNames.CREDENTIAL_EXPIRE_IN_DAYS, daysLeft);
                            }
                        }
                    }

                });
        if (StrUtil.isNotEmpty(org)) {
            additionalParameters.put(InOAuth2ParameterNames.TENANT, org);
        }
    }

    public static String getLoginOrg(Map<String, Object> additionalParameters) {
        return (String) additionalParameters.get(InOAuth2ParameterNames.TENANT);
    }

    public static String getLoginUsername(Map<String, Object> additionalParameters) {
        return (String) additionalParameters.get(InOAuth2ParameterNames.USERNAME);
    }

    public static boolean isIgnoreKey(String key) {
        return StrUtil.startWith(key, InOAuth2ParameterNames.IGNORE_PREFIX);
    }
}
