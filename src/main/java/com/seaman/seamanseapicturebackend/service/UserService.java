package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.model.dto.UserRegisterRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;

/**
 * @author SeaManzhw
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-06-19 16:46:30
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册请请求
     * @return 注册是否成功
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 密码加密
     *
     * @param userPassword 需要加密的密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

}
