package com.seaman.seamanseapicturebackend.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seaman.seamanseapicturebackend.annotation.AuthCheck;
import com.seaman.seamanseapicturebackend.api.aliyunai.AliYunAiApi;
import com.seaman.seamanseapicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.seaman.seamanseapicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.ImageSearchApiFacade;
import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.model.ImageSearchResult;
import com.seaman.seamanseapicturebackend.common.BaseResponse;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.common.ResultUtils;
import com.seaman.seamanseapicturebackend.constant.UserConstant;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.seaman.seamanseapicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.seaman.seamanseapicturebackend.model.dto.picture.*;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.PictureReviewStatusEnum;
import com.seaman.seamanseapicturebackend.model.vo.PictureTagCategory;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 图片控制类
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    /**
     * 上传图片
     *
     * @param multipartFile        待上传图片
     * @param pictureUploadRequest 图片上传请求
     * @param request              请求头
     * @return 图片视图
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser));
    }

    /**
     * 通过URL上传图片
     *
     * @param pictureUploadRequest 上传图片DTO
     * @param request              请求头
     * @return 图片视图
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        return ResultUtils.success(pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser));
    }

    /**
     * 根据id删除图片
     *
     * @param deleteRequest 通用删除请求
     * @param request       请求头
     * @return 删除成功
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.deletePicture(deleteRequest, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @return 更新是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.updatePicture(pictureUpdateRequest, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（仅管理员）
     *
     * @param id      图片id
     * @param request 请求头
     * @return 图片（未脱敏）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     *
     * @param id      图片id
     * @param request 请求头
     * @return 脱敏后的图片
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureService.getPictureVOById(id, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 分页图片（未脱敏）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIf(picturePage == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询公共图库带有多级缓存，查询个人图库没有缓存
        return ResultUtils.success(pictureService.listPictureVOByPage(pictureQueryRequest, request));
    }

    /**
     * 获取公共图库（带有缓存）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        return ResultUtils.success(pictureService.listPictureVOByPageWithCache(pictureQueryRequest, request));
    }

    /**
     * 编辑图片（用户使用）
     *
     * @param pictureEditRequest 编辑请求
     * @param request            请求头
     * @return 编辑是否成功
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.editPicture(pictureEditRequest, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 硬编码的标签、分类
     *
     * @return 返回标签分类视图
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     *
     * @param pictureReviewRequest 审核图片请求
     * @param request              请求头
     * @return true
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求
     * @param request                     请求头
     * @return 成功抓取数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser));
    }

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest 以图搜图请求
     * @return 搜图结果
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPictureBaiDu(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getThumbnailUrl());
        return ResultUtils.success(resultList);
    }

//    /**
//     * 以图搜图
//     * 需要修改前端部分
//     *
//     * @param searchPictureByPictureRequest 以图搜图请求
//     * @return 搜图结果
//     */
//    @PostMapping("/search/picture")
//    public BaseResponse<List<SoImageSearchResult>> searchPictureByPictureSO(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
//        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
//        Long pictureId = searchPictureByPictureRequest.getPictureId();
//        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
//        Picture oldPicture = pictureService.getById(pictureId);
//        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        List<SoImageSearchResult> resultList = SoImageSearchApiFacade.searchImage(oldPicture.getUrl());
//        return ResultUtils.success(resultList);
//    }

    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorRequest 根据颜色搜索图片请求
     * @param request                     请求头
     * @return 图片结果
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.searchPictureByColor(spaceId, picColor, loginUser));
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑图片请求
     * @param request                   请求头
     * @return 图片结果
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建扩图任务请求
     * @param request                             请求头
     * @return 创建扩图任务响应
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser));
    }

    /**
     * 查询 AI 扩图任务
     *
     * @param taskId 任务id
     * @return 获取扩图任务响应
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

}
