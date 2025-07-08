package com.seaman.seamanseapicturebackend.controller;

import com.seaman.seamanseapicturebackend.common.BaseResponse;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.common.ResultUtils;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.seaman.seamanseapicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.seaman.seamanseapicturebackend.model.entity.SpaceUser;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.SpaceUserVO;
import com.seaman.seamanseapicturebackend.service.SpaceUserService;
import com.seaman.seamanseapicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     *
     * @param spaceUserAddRequest 添加成员请求
     * @param request             请求头
     * @return 空间成员关系id
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.addSpaceUser(spaceUserAddRequest));
    }

    /**
     * 从空间移除成员
     *
     * @param deleteRequest 移除成员请求
     * @param request       请求头
     * @return 移除是否成功
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.deleteSpaceUser(deleteRequest));
    }

    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest 查询成员空间关系请求
     * @return 空间成员关系
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.getSpaceUser(spaceUserQueryRequest));
    }

    /**
     * 查询成员信息列表
     *
     * @param spaceUserQueryRequest 查询成员信息列表请求
     * @param request               请求头
     * @return 成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.listSpaceUser(spaceUserQueryRequest));
    }

    /**
     * 编辑成员信息（设置权限）
     *
     * @param spaceUserEditRequest 编辑成员信息请求
     * @param request              请求头
     * @return 编辑是否成功
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(spaceUserService.editSpaceUser(spaceUserEditRequest));
    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request 请求头
     * @return 空间成员信息
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceUserService.listMyTeamSpace(loginUser));
    }
}
