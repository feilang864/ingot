package com.ingot.cloud.bff.client;

import java.util.Map;

import com.ingot.cloud.bff.service.BffAuthService;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Auth 授权服务 Feign Client，通过 Nacos 服务发现进行 RPC 调用</p>
 *
 * <p>封装了 BFF 登录流程需要的三个 auth 端点：预授权、获取授权码、Token 换取/撤销。
 * 服务地址通过 {@link ServiceNameConstants#AUTH_SERVICE} 自动从 Nacos 解析，
 * 无需硬编码 IP 或域名。</p>
 *
 * @author jy
 * @apiNote 此 Client 由 {@link BffAuthService} 内部使用，不应被 Controller 直接调用。
 * @see BffAuthService
 * @see ServiceNameConstants#AUTH_SERVICE
 * @since 1.0.0
 */
@FeignClient(contextId = "authClient", value = ServiceNameConstants.AUTH_SERVICE)
public interface AuthClient {

    /**
     * 预授权接口（PKCE 公开客户端）
     *
     * @param cookie        转发的 Cookie（携带预授权 session）
     * @param userType      用户类型
     * @param preGrantType  预授权类型
     * @param clientId      客户端 ID
     * @param codeChallenge PKCE code_challenge
     * @param responseType  响应类型
     * @param redirectUri   重定向 URI
     * @param scope         授权范围
     * @param state         状态校验
     * @param formData      表单数据（username/password 等）
     * @return 预授权结果（包含可选租户列表）
     */
    @PostMapping(value = "/oauth2/pre_authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<R<Map<String, Object>>> preAuthorize(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam("user_type") String userType,
            @RequestParam("pre_grant_type") String preGrantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("response_type") String responseType,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            Map<String, ?> formData);

    /**
     * 获取授权码
     *
     * @param cookie        转发的 Cookie（携带预授权 session）
     * @param preGrantType  预授权类型
     * @param org           组织/租户 ID
     * @param clientId      客户端 ID
     * @param codeChallenge PKCE code_challenge
     * @param responseType  响应类型
     * @param redirectUri   重定向 URI
     * @param scope         授权范围
     * @param state         状态校验
     * @return 授权码结果
     */
    @GetMapping("/oauth2/authorize")
    R<Map<String, Object>> authorize(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam("pre_grant_type") String preGrantType,
            @RequestParam("org") String org,
            @RequestParam("client_id") String clientId,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("response_type") String responseType,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state);

    /**
     * 授权码换取 Token（PKCE 公开客户端，无需 client_secret）
     *
     * @param formData 表单数据（code/grant_type/code_verifier/client_id/redirect_uri）
     * @return Token 结果
     */
    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    R<Map<String, Object>> token(Map<String, ?> formData);

    /**
     * 撤销 Token
     *
     * @param authorization Bearer token
     * @return 撤销结果
     */
    @DeleteMapping("/token")
    R<?> revokeToken(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
