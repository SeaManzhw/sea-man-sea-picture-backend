package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.sharding.DynamicShardingManager;
import com.seaman.seamanseapicturebackend.mapper.SpaceMapper;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceAddRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceQueryRequest;
import com.seaman.seamanseapicturebackend.model.dto.space.SpaceUpdateRequest;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.SpaceUser;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.SpaceLevelEnum;
import com.seaman.seamanseapicturebackend.model.enums.SpaceRoleEnum;
import com.seaman.seamanseapicturebackend.model.enums.SpaceTypeEnum;
import com.seaman.seamanseapicturebackend.model.vo.SpaceVO;
import com.seaman.seamanseapicturebackend.model.vo.UserVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.SpaceService;
import com.seaman.seamanseapicturebackend.service.SpaceUserService;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author SeaMan
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-06-29 17:08:30
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    private static final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

    @Resource
    TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    @Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;

    /**
     * 校验空间
     *
     * @param space 待校验的空间
     * @param add   是否为新增空间
     */
    @Override
    public void validSpace(Space space, boolean add) {
        // 判空
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceLevel);
        // 若为新创建空间
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类别不能为空");
        }
        //校验参数
        ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 32, ErrorCode.PARAMS_ERROR, "空间名过长");
        ThrowUtils.throwIf(spaceType != null && spaceTypeEnum == null, ErrorCode.PARAMS_ERROR, "空间类别不存在");
    }

    /**
     * 获取单条空间包装类
     *
     * @param space   空间
     * @param request 请求头
     * @return 空间包装类
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取分页空间包装类
     *
     * @param spacePage 分页空间类
     * @param request   请求头
     * @return 分页空间包装类
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 1. 提取空间列表
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 2. 将Space转换为VO类
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 3. 查询用户信息
        Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 4. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 获取空间类查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 空间查询条件
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 根据等级填充空间
     *
     * @param space 待填充空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 根据id删除空间
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     * @return 删除是否成功
     */
    @Override
    public boolean deleteSpace(DeleteRequest deleteRequest, User loginUser) {
        // 1. 判断是否存在空间
        Long spaceId = deleteRequest.getId();
        Space oldSpace = this.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 2. 仅用户自己和管理员可以删除
        checkSpaceAuth(loginUser, oldSpace);
        // 4. 操作数据库
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 关联删除空间内所有图片
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("spaceId", spaceId);
            List<Picture> pictureList = pictureService.list(queryWrapper);
            for (Picture picture : pictureList) {
                DeleteRequest deleteRequestPicture = new DeleteRequest();
                deleteRequestPicture.setId(picture.getId());
                pictureService.deletePicture(deleteRequestPicture, loginUser);
            }
            // 删除空间
            return this.removeById(spaceId);
        }));
    }

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 更新空间请求
     * @param loginUser          登录用户
     * @return 更新是否成功
     */
    @Override
    public boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, User loginUser) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        fillSpaceBySpaceLevel(space);
        // 空间校验
        validSpace(space, false);
        // 判断是否存在
        Long id = space.getId();
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        return this.updateById(space);
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 空间编辑请求
     * @param loginUser        登录用户
     * @return 编辑是否成功
     */
    @Override
    public boolean editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
        // 类型转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        // 自动填充数据
        fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 空间校验
        validSpace(space, false);
        // 判断是否存在
        Long id = spaceEditRequest.getId();
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可以编辑
        checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        return this.updateById(space);
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        // 1. 填充默认参数
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认名称");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 2. 填充数据
        fillSpaceBySpaceLevel(space);
        // 3. 数据校验
        validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 4. 权限校验
        ThrowUtils.throwIf(SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);
        // 5.操作数据库
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, space.getUserId())
                        .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(loginUser.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    ThrowUtils.throwIf(!spaceUserService.save(spaceUser), ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                    // 旗舰版团队创建分表
                    if (SpaceLevelEnum.FLAGSHIP.getValue() == space.getSpaceLevel()) {
                        dynamicShardingManager.createSpacePictureTable(space);
                    }
                }
                // 写入数据库
                ThrowUtils.throwIf(!this.save(space), ErrorCode.OPERATION_ERROR);
            } finally {
                lockMap.remove(userId);
            }
            return Optional.ofNullable(space.getId()).orElse(-1L);
        }
    }

    /**
     * 更新空间大小
     *
     * @param spaceId 空间id
     * @param picSize 图片大小
     * @param isAdd   是否为新增，返回为减少
     * @return 更新是否成功
     */
    @Override
    public boolean updateSpaceCapacity(Long spaceId, Long picSize, boolean isAdd) {
        char c = '-';
        if (isAdd) {
            c = '+';
        }
        return this.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = totalSize " + c + " " + picSize)
                .setSql("totalCount = totalCount " + c + " 1")
                .update();
    }

    /**
     * 校验空间权限
     *
     * @param loginUser 登录用户
     * @param space     空间
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        ThrowUtils.throwIf(!Objects.equals(loginUser.getId(), space.getUserId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);
    }

}




