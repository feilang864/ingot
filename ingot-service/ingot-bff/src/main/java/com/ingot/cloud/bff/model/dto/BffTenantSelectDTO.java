package com.ingot.cloud.bff.model.dto;

import lombok.Data;

/**
 * <p>BFF 选择租户请求参数</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffTenantSelectDTO {
    private String tenantId;
    /**
     * 登录成功后前端跳转地址（可选）。
     * 后端会校验该值是否在白名单内，校验通过后原样返回。
     * 未传时使用 {@code ingot.bff.security.default-redirect-uri} 配置。
     */
    private String redirectUri;
}
