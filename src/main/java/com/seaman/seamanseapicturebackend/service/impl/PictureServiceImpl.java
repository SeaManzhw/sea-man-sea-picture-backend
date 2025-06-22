package com.seaman.seamanseapicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.manager.FileManager;
import com.seaman.seamanseapicturebackend.mapper.PictureMapper;
import com.seaman.seamanseapicturebackend.model.dto.file.UploadPictureResult;
import com.seaman.seamanseapicturebackend.model.dto.picture.PictureUploadRequest;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.PictureVO;
import com.seaman.seamanseapicturebackend.service.PictureService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

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
}




