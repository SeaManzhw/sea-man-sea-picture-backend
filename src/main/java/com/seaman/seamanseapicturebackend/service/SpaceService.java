package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceQueryRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceUpdateRequest;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author SeaManzhw
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-06-29 17:08:30
 */
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     *
     * @param space 待校验的空间
     * @param add   是否为新增空间
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取单条空间包装类
     *
     * @param space   空间
     * @param request 请求头
     * @return 空间包装类
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取分页空间包装类
     *
     * @param spacePage 分页空间类
     * @param request   请求头
     * @return 分页空间包装类
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取空间类查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 空间查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据等级填充空间
     *
     * @param space 待填充空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 根据id删除空间
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     * @return 删除是否成功
     */
    boolean deleteSpace(DeleteRequest deleteRequest, User loginUser);

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 更新空间请求
     * @param loginUser          登录用户
     * @return 更新是否成功
     */
    boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, User loginUser);

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 空间编辑请求
     * @param loginUser        登录用户
     * @return 编辑是否成功
     */
    boolean editSpace(SpaceEditRequest spaceEditRequest, User loginUser);

    /**
     * 创建空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 更新空间大小
     *
     * @param spaceId 空间id
     * @param picSize 图片大小
     * @param isAdd   是否为新增，返回为减少
     * @return 更新是否成功
     */
    boolean updateSpaceCapacity(Long spaceId, Long picSize, boolean isAdd);

    /**
     * 校验空间权限
     *
     * @param loginUser 登录用户
     * @param space     空间
     */
    void checkSpaceAuth(User loginUser, Space space);

}
