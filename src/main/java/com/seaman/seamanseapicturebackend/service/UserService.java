package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.model.dto.user.UserAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.user.UserLoginRequest;
import com.seaman.seamanseapicturebackend.model.dto.user.UserQueryRequest;
import com.seaman.seamanseapicturebackend.model.dto.user.UserRegisterRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.LoginUserVO;
import com.seaman.seamanseapicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     * 获得脱敏后的用户信息
     *
     * @param user 未脱敏用户
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList 未脱敏用户列表
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

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

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询用户DTO
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    /**
     * 创建新用户
     *
     * @param userAddRequest 创建新用户请求
     * @return 用户id
     */
    long addUser(UserAddRequest userAddRequest);

    /**
     * 分页查询用户（脱敏）
     *
     * @param userQueryRequest 查询DTO
     * @return 查询结果
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user 需要判断的用户
     * @return 是否为管理员
     */
    boolean isAdmin(User user);

}
