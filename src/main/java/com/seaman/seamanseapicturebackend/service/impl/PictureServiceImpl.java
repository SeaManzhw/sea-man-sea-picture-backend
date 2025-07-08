package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seaman.seamanseapicturebackend.api.aliyunai.AliYunAiApi;
import com.seaman.seamanseapicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.seaman.seamanseapicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.CosManager;
import com.seaman.seamanseapicturebackend.manager.auth.SpaceUserAuthManager;
import com.seaman.seamanseapicturebackend.manager.auth.StpKit;
import com.seaman.seamanseapicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.seaman.seamanseapicturebackend.manager.upload.FilePictureUpload;
import com.seaman.seamanseapicturebackend.manager.upload.PictureUploadTemplate;
import com.seaman.seamanseapicturebackend.manager.upload.UrlPictureUpload;
import com.seaman.seamanseapicturebackend.mapper.PictureMapper;
import com.seaman.seamanseapicturebackend.model.dto.file.UploadPictureResult;
import com.seaman.seamanseapicturebackend.model.dto.picture.*;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.PictureReviewStatusEnum;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import com.seaman.seamanseapicturebackend.model.vo.UserVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.SpaceService;
import com.seaman.seamanseapicturebackend.service.UserService;
import com.seaman.seamanseapicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author SeaManzhw
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-21 20:57:26
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 根据上传结果构造图片信息
     *
     * @param userId              上传用户Id
     * @param uploadPictureResult 上传图片结果
     * @param pictureId           图片id
     * @return 图片信息
     */
    private static Picture getPicture(Long userId, UploadPictureResult uploadPictureResult,
                                      Long pictureId, PictureUploadRequest pictureUploadRequest, Long spaceId) {
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);//设置空间id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(userId);
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

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
     * @param inputSource          输入源
     * @param pictureUploadRequest 上传图片请求DTO
     * @param loginUser            上传用户
     * @return 图片视图
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间
        Long spaceId = null;
        if (pictureUploadRequest != null) {
            spaceId = pictureUploadRequest.getSpaceId();
        }
        if (spaceId != null) {
            // 检查空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            // 检查空间是否有权限
            ThrowUtils.throwIf(!Objects.equals(space.getUserId(), loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
            // 校验空间额度
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小不足");
        }
        // 判断新增或更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 若为更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            // 图片不存在
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 判断权限
            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
            //校验空间是否一致
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 校验私有空间权限
                ThrowUtils.throwIf(!Objects.equals(spaceId, oldPicture.getSpaceId()), ErrorCode.PARAMS_ERROR, "上传空间错误");
            }
            // 删除旧图信息
            deleteAllPictureFromCOS(oldPicture);
        }
        // 上传图片，得到图片信息
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("private/%s", spaceId);
        }
        // 调用模板类
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 删除原图信息
        deleteOnePictureFromCOS(uploadPictureResult.getOldUrl());
        // 构造存入数据库的图片信息
        Picture picture = getPicture(loginUser.getId(), uploadPictureResult, pictureId, pictureUploadRequest, spaceId);
        // 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        if (spaceId != null) {
            // 开启事务
            Long finalSpaceId = spaceId;
            transactionTemplate.execute(status -> {
                // 保存图片
                ThrowUtils.throwIf(!this.saveOrUpdate(picture), ErrorCode.OPERATION_ERROR, "图片上传失败");
                // 更新空间的使用额度
                ThrowUtils.throwIf(!spaceService.updateSpaceCapacity(finalSpaceId, picture.getPicSize(), true),
                        ErrorCode.OPERATION_ERROR, "额度更新失败");
                return true;
            });
        } else {
            ThrowUtils.throwIf(!this.saveOrUpdate(picture), ErrorCode.OPERATION_ERROR, "图片上传失败");
        }
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
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
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
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
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
     * @param loginUser     登录用户
     * @return 删除是否成功
     */
    @Override
    public boolean deletePicture(DeleteRequest deleteRequest, User loginUser) {
        // 1. 判断是否存在图片
        Long pictureId = deleteRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 2. 仅用户自己和管理员可以删除
        // 已使用Sa-Token注解鉴权
        //checkPictureAuth(loginUser, oldPicture);
        // 3. 清理COS中的文件资源
        deleteAllPictureFromCOS(oldPicture);
        // 4. 操作数据库
        return this.removeById(pictureId);
    }

    /**
     * 更新图片（仅管理员）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @param loginUser            登录用户
     * @return 更新是否成功
     */
    @Override
    public boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        // 1. 将DTO类转换为实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 2. 校验图片是否合法
        validPicture(picture);
        // 3. 校验图片是否存在
        Picture oldPicture = this.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 4. 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 5. 操作数据库
        return this.updateById(picture);
    }

    /**
     * 编辑图片（用户使用）
     *
     * @param pictureEditRequest 编辑请求
     * @param loginUser          登录用户
     * @return 编辑是否成功
     */
    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
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
        // 已使用Sa-Token注解鉴权
        //checkPictureAuth(loginUser, oldPicture);
        // 5. 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 6. 操作数据库
        return this.updateById(picture);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.PARAMS_ERROR);
        // 2. 图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 修改状态是否没变
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 4. 构造更新对象
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewMessage(reviewMessage);
        // 5. 操作数据库
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充神审核参数
     *
     * @param picture   待添加的图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
        } else {
            // 普通用户无论创建还是编辑都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest 抓取图片请求
     * @param loginUser                   登录用户
     * @return 成功抓取条数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获取参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 构造要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        // 抓取地址
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 获取元素
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isNull(div), ErrorCode.OPERATION_ERROR, "获取元素失败");
        // 处理并上传图片
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 根据url删除某张图片
     *
     * @param url 图片地址
     */
    @Override
    public void deleteOnePictureFromCOS(String url) {
        //直接调用该方法，而不是通过deleteAllPictureFromCOS时，只有删除原图。此时不需要修改空间大小
        cosManager.deleteObject(url);
    }

    /**
     * 根据图片信息删除图片原图、缩略图
     *
     * @param oldPicture 旧图片
     */
    @Override
    public void deleteAllPictureFromCOS(Picture oldPicture) {
        Long spaceId = oldPicture.getSpaceId();
        if (spaceId != null) {
            boolean result = spaceService.updateSpaceCapacity(spaceId, oldPicture.getPicSize(), false);
            if (!result) {
                spaceService.updateSpaceCapacity(spaceId, oldPicture.getPicSize(), true);
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
        deleteOnePictureFromCOS(oldPicture.getUrl());
        deleteOnePictureFromCOS(oldPicture.getThumbnailUrl());
    }

    /**
     * 校验空间图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            //  公共空间，仅本人或管理员
            ThrowUtils.throwIf(!Objects.equals(picture.getUserId(), loginUserId) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR);
        } else {
            // 私有空间，仅本人
            ThrowUtils.throwIf(!Objects.equals(picture.getUserId(), loginUserId), ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 根据 id 获取图片（封装类）
     *
     * @param id      图片id
     * @param request 请求头
     * @return 脱敏后的图片
     */
    @Override
    public PictureVO getPictureVOById(long id, HttpServletRequest request) {
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 只能查看过审的权限
        PictureReviewStatusEnum statusEnum = PictureReviewStatusEnum.getEnumByValue(picture.getReviewStatus());
        ThrowUtils.throwIf(!PictureReviewStatusEnum.PASS.equals(statusEnum), ErrorCode.NO_AUTH_ERROR, " 图片审核未通过");
        // 校验空间权限
        // 使用Sa-Token注解鉴权要求一定登录，这里使用编程时校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//            checkPictureAuth(userService.getLoginUser(request), picture);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return pictureVO;
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    @Override
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //访问公共图库
            // 普通用户默认只能看到审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
            return listPictureVOByPageWithCache(pictureQueryRequest, request);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            ThrowUtils.throwIf(!Objects.equals(loginUser.getId(), space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            return listPictureVOByPageWithoutCache(pictureQueryRequest, request);
        }
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithoutCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 操作数据库
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = page(new Page<>(current, pageSize), getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIf(picturePage == null, ErrorCode.NOT_FOUND_ERROR);
        return getPictureVOPage(picturePage, request);
    }

    /**
     * 缓存方法查询图片
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 查询结果
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 构造缓存 key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(jsonStr.getBytes(StandardCharsets.UTF_8));
        String cacheKey = "seamanseapicture:listPictureVOByPage" + hashKey;
        // 查询本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null) {
            return JSONUtil.toBean(cacheValue, Page.class);
        }
        // 查询redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cacheValue = opsForValue.get(cacheKey);
        if (cacheValue != null) {
            LOCAL_CACHE.put(cacheKey, cacheValue);
            return JSONUtil.toBean(cacheValue, Page.class);
        }
        // 操作数据库
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = this.page(new Page<>(current, pageSize), this.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIf(picturePage == null, ErrorCode.NOT_FOUND_ERROR);
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        // 存入 Redis 数据库与本地缓存
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存过期时间，5 到 10 分钟随机，避免缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        LOCAL_CACHE.put(cacheKey, cacheValue);
        return pictureVOPage;
    }

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId   空间id
     * @param picColor  图片颜色
     * @param loginUser 登录用户
     * @return 按相似度返回图片列表
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        // 校验空间及权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        ThrowUtils.throwIf(!Objects.equals(space.getUserId(), loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        // 查询图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 排序并返回
        Color targetColor = Color.decode(picColor);
        return pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, Color.decode(hexColor));
                }))
                .limit(12)
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑图片请求
     * @param loginUser                 登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 获取并校验参数
        String category = pictureEditByBatchRequest.getCategory();
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!Objects.equals(space.getUserId(), loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        // 3. 查询指定图片，仅查询需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureIdList)) {
            return;
        }
        // 4. 修改图片对象
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRole(pictureList, nameRule);
        // 5. 操作数据库
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建扩图任务请求
     * @param loginUser                           登录用户
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
        // 校验权限
        // 已使用Sa-Token注解鉴权
        //checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建请求
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则
     */
    private void fillPictureWithNameRole(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }
}
