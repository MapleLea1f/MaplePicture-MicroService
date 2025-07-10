package com.maple.userservice.domain.constant;


/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    // region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    // endregion

    /**
     * 管理员调用添加用户接口时的默认密码
     */
    String DEFAULT_PASSWORD = "12345678";

}
