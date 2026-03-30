package com.ingot.framework.commons.model.types;

/**
 * <p>Description  : UserType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/5/7.</p>
 * <p>Time         : 16:59.</p>
 */
public interface UserType {
    /**
     * ID
     */
    Long getId();

    /**
     * 用户名
     */
    String getUsername();

    /**
     * 昵称
     */
    String getNickname();

    /**
     * 手机号
     */
    String getPhone();

    /**
     * 邮件地址
     */
    String getEmail();

    /**
     * 头像
     */
    String getAvatar();

    /**
     * 是否启用（true-启用 false-禁用）
     */
    Boolean getEnabled();

    /**
     * 是否锁定（true-锁定 false-正常）
     */
    Boolean getLocked();
}
