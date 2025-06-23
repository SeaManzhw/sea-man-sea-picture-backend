package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.FileManager;
import com.seaman.seamanseapicturebackend.mapper.PictureMapper;
import com.seaman.seamanseapicturebackend.model.dto.file.UploadPictureResult;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureEditRequest;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureQueryRequest;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureUpdateRequest;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureUploadRequest;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import com.seaman.seamanseapicturebackend.model.vo.UserVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author SeaManzhw
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-21 20:57:26
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    FileManager fileManager;

    @Resource
    UserService userService;

    /**
     * 根据上传结果构造图片信息
     *
     * @param userId              上传用户Id
     * @param uploadPictureResult 上传图片结果
     * @param pictureId           图片id
     * @return 图片信息
     */
    private static Picture getPicture(Long userId, UploadPictureResult uploadPictureResult, Long pictureId) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(userId);
        //若为更新操作需要补充id和编辑时间
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        return picture;
    }

    /**
     * 上传图片
     *
     * @param multipartFile        待上传图片
     * @param pictureUploadRequest 上传图片请求DTO
     * @param loginUser            上传用户
     * @return 图片视图
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断新增或更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 若为更新，判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (exists) {
                // todo 若存在，需要删除COS原图片
            } else {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        // 上传图片，得到图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造存入数据库的图片信息
        Picture picture = getPicture(loginUser.getId(), uploadPictureResult, pictureId);
        // 操作数据库
        ThrowUtils.throwIf(!this.saveOrUpdate(picture), ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }


    /**
     * 获取查询查询条件
     *
     * @param pictureQueryRequest 查询图片请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片包装类
     *
     * @param picture 待包装的图片
     * @param request 请求头
     * @return 包装后的图片
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     *
     * @param picturePage 分页图片集合
     * @param request     请求头
     * @return 封装后的分页图片结合
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 1. 提取图片列表
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 2. 将picture转换为VO类
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 3. 查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 4. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片
     *
     * @param picture 待校验的图片
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 根据id删除图片
     *
     * @param deleteRequest 删除请求
     * @param request       请求头
     * @return 删除是否成功
     */
    @Override
    public boolean deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 1. 判断是否存在图片
        Long pictureId = deleteRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 2. 仅用户自己和管理员可以删除
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        // 3. 操作数据库
        return this.removeById(pictureId);
    }

    /**
     * 更新图片（仅管理员）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @return 更新是否成功
     */
    @Override
    public boolean updatePicture(PictureUpdateRequest pictureUpdateRequest) {
        // 1. 将DTO类转换为实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 2. 校验图片是否合法
        validPicture(picture);
        // 3. 校验图片是否存在
        Picture oldPicture = this.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 操作数据库
        return this.updateById(picture);
    }

    /**
     * 编辑图片（用户使用）
     *
     * @param pictureEditRequest 编辑请求
     * @param request            请求头
     * @return 编辑是否成功
     */
    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 1. 将DTO类转换为实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        picture.setEditTime(new Date());
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 2. 图片是否合法
        validPicture(picture);
        // 3. 判断图片是否存在
        Picture oldPicture = this.getById(picture);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 只有用户自身或管理员才可编辑
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        // 5. 操作数据库
        return this.updateById(picture);
    }

}
