package com.seaman.seamanseapicturebackend.controller;


import com.seaman.seamanseapicturebackend.annotation.AuthCheck;
import com.seaman.seamanseapicturebackend.common.BaseResponse;
import com.seaman.seamanseapicturebackend.common.ResultUtils;
import com.seaman.seamanseapicturebackend.constant.UserConstant;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureUploadRequest;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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


    /**
     * 上传图片（管理员）
     *
     * @param multipartFile        待上传图片
     * @param pictureUploadRequest 图片上传请求
     * @param request              请求头
     * @return 图片视图
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser));
    }


}
