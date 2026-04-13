package com.ingot.cloud.pms.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.MetaApp;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.credential.model.CredentialScene;
import com.ingot.framework.security.credential.model.PasswordCheckResult;
import com.ingot.framework.security.credential.model.request.CredentialValidateRequest;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.tenant.TenantEnv;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
@Slf4j
public class IdentityUtil {
    /**
     * 填充凭证状态到 {@link UserDetailsResponse}（仅用户名/密码登录场景调用）
     * <p>
     * PMS 拥有凭证数据库，在此查询密码过期状态并填充到 response，Auth 服务无需直接访问数据：
     * <ul>
     *   <li>密码硬过期（无宽限）：{@code credentialsNonExpired = false}，Auth 侧会阻断登录</li>
     *   <li>宽限期内或即将过期：{@code credentialWarning} 填入警告码，Auth 侧允许登录并记录日志</li>
     * </ul>
     * 账号不可用（禁用 / 锁定）时跳过检查。
     *
     * @param result                    待填充的响应对象
     * @param userId                    用户 ID
     * @param credentialSecurityService 凭证安全服务
     */
    public static void fillCredentialState(UserDetailsResponse result,
                                           Long userId,
                                           CredentialSecurityService credentialSecurityService) {
        if (result == null) {
            return;
        }
        // 账号不可用时不检查凭证（Auth 侧会直接因 enabled=false/locked=true 拒绝）
        if (!Boolean.TRUE.equals(result.getEnabled()) || Boolean.TRUE.equals(result.getLocked())) {
            return;
        }

        try {
            PasswordCheckResult checkResult = credentialSecurityService.validate(
                    CredentialValidateRequest.builder()
                            .scene(CredentialScene.LOGIN)
                            .userId(userId)
                            .manualProcessError(true)
                            .build());

            if (!checkResult.isPassed()) {
                result.setCredentialsNonExpired(false);
            } else if (checkResult.hasWarnings() && checkResult.getWarningCode() != null) {
                result.setCredentialWarning(checkResult.getWarningCode().getCode());

                // 提取策略计算好的数字字段，供前端自行组装提示语
                Map<String, Object> credentialMeta = getCredentialMeta(checkResult);
                if (!credentialMeta.isEmpty()) {
                    result.setCredentialMeta(credentialMeta);
                }
            }
        } catch (Exception e) {
            // 凭证检查失败不应阻断登录，记录日志后放行
            log.warn("[IdentityUtil] 凭证过期检查异常，放行登录 userId={}", userId, e);
        }
    }

    private static @NonNull Map<String, Object> getCredentialMeta(PasswordCheckResult checkResult) {
        Map<String, Object> rawMeta = checkResult.getMetadata();
        Map<String, Object> credentialMeta = new HashMap<>(2);
        Object graceRemaining = rawMeta.get(PasswordExpirationPolicy.META_GRACE_REMAINING);
        if (graceRemaining != null) {
            credentialMeta.put(PasswordExpirationPolicy.META_GRACE_REMAINING, graceRemaining);
        }
        Object daysLeft = rawMeta.get(PasswordExpirationPolicy.META_DAYS_LEFT);
        if (daysLeft != null) {
            credentialMeta.put(PasswordExpirationPolicy.META_DAYS_LEFT, daysLeft);
        }
        return credentialMeta;
    }

    /**
     * 映射用户信息
     *
     * @param user                 用户信息
     * @param userType             用户类型
     * @param tenant               租户ID
     * @param sysTenantService     租户服务
     * @param sysUserTenantService 用户租户服务
     * @param bizUserService       用户服务
     * @param bizAppService        应用服务
     * @param bizRoleService       角色服务
     * @return 用户信息
     */
    public static UserDetailsResponse map(SysUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          SysTenantService sysTenantService,
                                          SysUserTenantService sysUserTenantService,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<TenantMainDTO> allows = getAllowTenants(user, sysTenantService, sysUserTenantService);

                    // 租户维度可访问性：allows 不为空，且登录 tenant 在允许列表内
                    boolean tenantAccessible = CollUtil.isNotEmpty(allows)
                            && (tenant == null || allows.stream()
                            .anyMatch(item -> Long.parseLong(item.getId()) == tenant));

                    // 账号维度：来自 sys_user.enabled / sys_user.locked
                    boolean userEnabled = Boolean.TRUE.equals(value.getEnabled()) && tenantAccessible;
                    boolean userLocked = Boolean.TRUE.equals(value.getLocked());

                    UserDetailsResponse result = UserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);
                    result.setEnabled(userEnabled);
                    result.setLocked(userLocked);

                    // 如果账号不可用（禁用或锁定）则不需要查询 scope，直接返回
                    if (!userEnabled || userLocked) {
                        return result;
                    }

                    // 设置用户 Scope
                    List<String> scopes = new ArrayList<>();
                    // 强制修改密码
                    if (BooleanUtil.isTrue(user.getMustChangePwd())) {
                        scopes.add(PermissionConstants.INIT_PASSWORD);
                        result.setScopes(scopes);
                        return result;
                    }

                    // 确认登录的租户不为空，那么查询用户在当前租户下的Scope
                    if (tenant != null) {
                        scopes.addAll(getScopes(tenant, user, bizUserService, bizAppService, bizRoleService));
                    } else {
                        scopes.addAll(allows.stream()
                                .flatMap(org ->
                                        TenantEnv.applyAs(Long.parseLong(org.getId()),
                                                        () -> getScopes(Long.parseLong(org.getId()), user,
                                                                bizUserService, bizAppService, bizRoleService))
                                                .stream())
                                .toList());
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    private static List<TenantMainDTO> getAllowTenants(SysUser user,
                                                       SysTenantService sysTenantService,
                                                       SysUserTenantService sysUserTenantService) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.getUserOrgs(user.getId());
        if (CollUtil.isEmpty(userTenantList)) {
            return ListUtil.empty();
        }
        return BizUtils.getTenants(sysTenantService,
                userTenantList.stream()
                        .map(UserTenantType::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t ->
                                Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
    }

    private static List<String> getScopes(Long tenant,
                                          SysUser user,
                                          BizUserService bizUserService,
                                          BizAppService bizAppService,
                                          BizRoleService bizRoleService) {
        // 查询所有角色
        List<RoleType> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        // InAuthorityUtils.authorityWithTenant 包装角色编码
        List<String> scopes = new ArrayList<>(getRoleCodes(roles, tenant));
        // 查询组织不可用应用
        List<MetaApp> disabledApps = bizAppService.getDisabledApps();
        List<String> authorities = bizRoleService.getRolesPermissions(roles).stream()
                .filter(auth -> disabledApps.stream()
                        .noneMatch(app -> Objects.equals(auth.getId(), app.getPermissionId())))
                .map(auth -> InAuthorityUtils.authorityWithTenant(auth.getCode(), tenant))
                .toList();
        scopes.addAll(authorities);

        return scopes;
    }

    private static List<String> getRoleCodes(List<? extends RoleType> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream()
                .map(item ->
                        InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant))
                .toList();
    }
}
