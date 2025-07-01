package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.common.DeleteRequest;
import com.seaman.seamanseapicturebackend.model.dto.picture.*;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author SeaManzhw
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-06-21 20:57:26
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param inputSource          输入源
     * @param pictureUploadRequest 上传图片请求DTO
     * @param loginUser            上传用户
     * @return 图片视图
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);


    /**
     * 获取查询查询条件
     *
     * @param pictureQueryRequest 查询图片请求
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类
     *
     * @param picture 待包装的图片
     * @param request 请求头
     * @return 包装后的图片
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     *
     * @param picturePage 分页图片集合
     * @param request     请求头
     * @return 封装后的分页图片结合
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture 待校验的图片
     */
    void validPicture(Picture picture);

    /**
     * 根据id删除图片
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     * @return 删除是否成功
     */
    boolean deletePicture(DeleteRequest deleteRequest, User loginUser);

    /**
     * 更新图片（仅管理员）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @param loginUser            登录用户
     * @return 更新是否成功
     */
    boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    /**
     * 编辑图片（用户使用）
     *
     * @param pictureEditRequest 编辑请求
     * @param loginUser          登录用户
     * @return 编辑是否成功
     */
    boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充神审核参数
     *
     * @param picture   待添加的图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest 抓取图片请求
     * @param loginUser                   登录用户
     * @return 成功抓取条数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 根据url删除某张图片
     *
     * @param url 图片地址
     */
    void deleteOnePictureFromCOS(String url);

    /**
     * 根据图片信息删除图片原图、缩略图
     *
     * @param oldPicture 旧图片
     */
    void deleteAllPictureFromCOS(Picture oldPicture);

    /**
     * 校验空间图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 根据 id 获取图片（封装类）
     *
     * @param id      图片id
     * @param request 请求头
     * @return 脱敏后的图片
     */
    PictureVO getPictureVOById(long id, HttpServletRequest request);

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 分页图片（脱敏）
     */
    Page<PictureVO> listPictureVOByPageWithoutCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 缓存方法查询图片
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求头
     * @return 查询结果
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

}
