package com.maple.userservice.interfaces.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserLoginRequest implements Serializable {

    // 标志 保证序列化前后一致性
    private static final long serialVersionUID = 5062354962902562735L;


    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
