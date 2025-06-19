package com.seaman.seamanseapicturebackend.controller;

import com.seaman.seamanseapicturebackend.common.BaseResponse;
import com.seaman.seamanseapicturebackend.common.ResultUtils;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.model.dto.UserLoginRequest;
import com.seaman.seamanseapicturebackend.model.dto.UserRegisterRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.LoginUserVO;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制类
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    UserService userService;

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册DTO
     * @return 返回用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR, "注册请求为空");
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录接口
     *
     * @param userLoginRequest 用户登录DTO
     * @param request          请求头
     * @return 用户视图（脱敏）
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR, "登录请求为空");
        return ResultUtils.success(userService.userLogin(userLoginRequest, request));
    }

    /**
     * 获取当前登录用户（脱敏）
     *
     * @param request 请求头
     * @return 脱敏后的登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户登录态注销
     *
     * @param request 请求头
     * @return 用户注销是否成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }


}
