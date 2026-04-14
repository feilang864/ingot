package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.AllOrgUserFilterDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final PasswordEncoder passwordEncoder;
    private final AssertionChecker assertionChecker;

    @Override
    public IPage<UserPageItemVO> conditionPage(Page<SysUser> page, UserQueryDTO condition, Long orgId) {
        return baseMapper.conditionPageWithTenant(page, condition, orgId);
    }

    @Override
    public IPage<UserPageItemVO> pageByDept(Page<SysUser> page, Long deptId, Long orgId) {
        return baseMapper.pageByDept(page, deptId, orgId);
    }

    @Override
    public IPage<SysUser> allOrgUserPage(Page<SysUser> page, AllOrgUserFilterDTO filter) {
        // 查询系统所有组织用户，不进行数据隔离
        return TenantEnv.applyAs(null, () ->
                page(page, Wrappers.<SysUser>lambdaQuery()
                        .like(StrUtil.isNotEmpty(filter.getPhone()), SysUser::getPhone, filter.getPhone())
                        .like(StrUtil.isNotEmpty(filter.getNickname()), SysUser::getNickname, filter.getNickname())
                        .like(StrUtil.isNotEmpty(filter.getEmail()), SysUser::getEmail, filter.getEmail()))
        );
    }

    @Override
    public void create(SysUser user) {
        user.setMustChangePwd(Boolean.TRUE);
        user.setPasswordChangedAt(DateUtil.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(DateUtil.now());
        if (user.getEnabled() == null) {
            user.setEnabled(Boolean.TRUE);
        }

        checkUserUniqueField(user, null);

        assertionChecker.checkOperation(save(user),
                "SysUserServiceImpl.CreateFailed");
    }

    @Override
    public void update(SysUser user) {
        SysUser current = getById(user.getId());

        checkUserUniqueField(user, current);
        user.setUpdatedAt(DateUtil.now());
        assertionChecker.checkOperation(updateById(user),
                "SysUserServiceImpl.UpdateFailed");
    }

    @Override
    public void delete(long id) {
        assertionChecker.checkOperation(removeById(id),
                "SysUserServiceImpl.RemoveFailed");
    }

    private void checkUserUniqueField(SysUser update, SysUser current) {
        BizUtils.checkUserUniqueField(update, current, this, assertionChecker);
    }
}
