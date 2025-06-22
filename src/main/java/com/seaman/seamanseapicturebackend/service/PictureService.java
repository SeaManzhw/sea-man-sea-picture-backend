package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureUploadRequest;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author SeaManzhw
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-06-21 20:57:26
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param multipartFile        待上传图片
     * @param pictureUploadRequest 上传图片请求DTO
     * @param loginUser            上传用户
     * @return 图片视图
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

}
