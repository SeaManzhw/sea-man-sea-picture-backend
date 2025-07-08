package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.mapper.SpaceUserMapper;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.SpaceUser;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.SpaceRoleEnum;
import com.seaman.seamanseapicturebackend.model.vo.SpaceUserVO;
import com.seaman.seamanseapicturebackend.model.vo.SpaceVO;
import com.seaman.seamanseapicturebackend.model.vo.UserVO;
import com.seaman.seamanseapicturebackend.service.SpaceService;
import com.seaman.seamanseapicturebackend.service.SpaceUserService;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author SeaMan
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-07-05 19:45:07
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    UserService userService;

    @Resource
    SpaceService spaceService;

    /**
     * 校验空间成员
     *
     * @param spaceUser 待校验的空间成员
     * @param add       是否为新增空间成员
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 创建空间成员请求
     * @return 空间成员id
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 根据id删除空间成员
     *
     * @param deleteRequest 删除请求
     * @return 删除是否成功
     */
    @Override
    public Boolean deleteSpaceUser(DeleteRequest deleteRequest) {
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = this.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 编辑空间成员
     *
     * @param spaceUserEditRequest 编辑空间成员请求
     * @return 编辑是否成功
     */
    @Override
    public Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = this.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 查询用户加入的空间
     *
     * @param loginUser 登录用户
     * @return 用户加入的空间
     */
    @Override
    public List<SpaceUserVO> listMyTeamSpace(User loginUser) {
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = this.list(getQueryWrapper(spaceUserQueryRequest));
        return getSpaceUserVOList(spaceUserList);
    }

    /**
     * 查询某个成员在某个空间中的信息
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 返回空间成员
     */
    @Override
    public SpaceUserVO getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest,HttpServletRequest request) {
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = this.getOne(getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return getSpaceUserVO(spaceUser,request);
    }

    /**
     * 查询空间中的成员
     *
     * @param spaceUserQueryRequest 查询空间中的成员请求
     * @return 空间成员
     */
    @Override
    public List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        List<SpaceUser> spaceUserList = this.list(getQueryWrapper(spaceUserQueryRequest));
        return getSpaceUserVOList(spaceUserList);
    }


    /**
     * 获取单条空间成员包装类
     *
     * @param spaceUser 空间成员
     * @param request   请求头
     * @return 空间成员包装类
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员包装类列表
     *
     * @param spaceUserList 空间成员列表
     * @return 间成员包装类列表
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    /**
     * 获取空间成员类查询条件
     *
     * @param spaceUserQueryRequest 空间成员查询请求
     * @return 空间成员查询条件
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

}