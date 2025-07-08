package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.seaman.seamanseapicturebackend.model.entity.SpaceUser;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author SeaMan
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-07-05 19:45:07
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 校验空间成员
     *
     * @param spaceUser 待校验的空间成员
     * @param add       是否为新增空间成员
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 创建空间成员请求
     * @return 空间成员id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 根据id删除空间成员
     *
     * @param deleteRequest 删除请求
     * @return 删除是否成功
     */
    Boolean deleteSpaceUser(DeleteRequest deleteRequest);

    /**
     * 获取单条空间成员包装类
     *
     * @param spaceUser 空间成员
     * @param request   请求头
     * @return 空间成员包装类
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类列表
     *
     * @param spaceUserList 空间成员列表
     * @return 间成员包装类列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取空间成员类查询条件
     *
     * @param spaceUserQueryRequest 空间成员查询请求
     * @return 空间成员查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询某个成员在某个空间中的信息
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 返回空间成员
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询空间中的成员
     *
     * @param spaceUserQueryRequest 查询空间中的成员请求
     * @return 空间成员
     */
    List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 编辑空间成员
     *
     * @param spaceUserEditRequest 编辑空间成员请求
     * @return 编辑是否成功
     */
    Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

    /**
     * 查询用户加入的空间
     *
     * @param loginUser 登录用户
     * @return 用户加入的空间
     */
    List<SpaceUserVO> listMyTeamSpace(User loginUser);


}
