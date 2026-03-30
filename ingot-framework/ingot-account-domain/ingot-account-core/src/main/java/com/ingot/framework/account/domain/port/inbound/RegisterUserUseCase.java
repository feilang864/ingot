package com.ingot.framework.account.domain.port.inbound;

import com.ingot.framework.account.domain.model.UserAccount;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 注册用户用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface RegisterUserUseCase {

    /**
     * 注册新用户
     *
     * @param command 注册命令
     * @return 创建的用户账号
     */
    UserAccount register(RegisterUserCommand command);

    /**
     * 注册用户命令
     */
    @Value
    @Builder
    class RegisterUserCommand {
        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 用户名
         */
        String username;

        /**
         * 原始密码（UseCase内部会加密）
         */
        String password;

        /**
         * 手机号
         */
        String phone;

        /**
         * 邮箱
         */
        String email;

        /**
         * 昵称
         */
        String nickname;

        /**
         * 头像
         */
        String avatar;

        /**
         * 创建人ID
         */
        Long createdBy;
    }
}
