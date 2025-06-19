package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.model.dto.UserLoginRequest;
import com.seaman.seamanseapicturebackend.model.dto.UserRegisterRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录DTO
     * @param request          请求头
     * @return 用户视图（脱敏）
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获得脱敏后的用户登录信息
     *
     * @param user 未脱敏的用户
     * @return 脱敏后的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取登录用户
     *
     * @param request 请求头
     * @return 登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户登录态注销
     *
     * @param request 请求头
     * @return 注销用户登录态是否成功
     */
    boolean userLogout(HttpServletRequest request);


}
