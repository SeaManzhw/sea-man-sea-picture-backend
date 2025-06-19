package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.constant.UserConstant;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.mapper.UserMapper;
import com.seaman.seamanseapicturebackend.model.dto.UserLoginRequest;
import com.seaman.seamanseapicturebackend.model.dto.UserRegisterRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.UserRoleEnum;
import com.seaman.seamanseapicturebackend.model.vo.LoginUserVO;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * @author SeaManzhw
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-06-19 16:46:30
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 实现用户注册接口
     *
     * @param userRegisterRequest 用户注册请请求
     * @return 注册是否成功
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 1. 校验参数
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "注册参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账户过短");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        // 2. 检测用户账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账户重复");
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入用户到数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        return user.getId();
    }

    /**
     * 加密密码
     *
     * @param userPassword 需要加密的密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "SeaMan";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录DTO
     * @param request          请求头
     * @return 用户视图（脱敏）
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 校验参数
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号非法");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码非法");
        // 2. 对用户密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询数据库用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");
        // 4. 保存用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获得脱敏后的用户登录信息
     *
     * @param user 未脱敏的用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }
}




