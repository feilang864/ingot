package com.ingot.framework.commons.constants;

/**
 * <p>内部系统BFF相关常量</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface BffConstants {

    /**
     * BFF登录端点
     */
    String BFF_URL = "/bff/auth/login";

    /**
     * BFF选择组织端点
     */
    String BFF_ORG_SELECT = "/bff/auth/tenant/select";
}
