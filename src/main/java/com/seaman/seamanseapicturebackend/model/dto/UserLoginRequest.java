package com.seaman.seamanseapicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求类
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -7056962548480481227L;

    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;
}
