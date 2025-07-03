package com.seaman.seamanseapicturebackend.manager.upload;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.seaman.seamanseapicturebackend.config.CosClientConfig;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.manager.CosManager;
import com.seaman.seamanseapicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource      输入源
     * @param uploadPathPrefix 图片路径前缀
     * @return 图片信息封装对象
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        String suffix = validPicture(inputSource);
        // 2. 获取图片上传路径
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String key = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件，下载文件到服务器
            file = File.createTempFile(key, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 4. 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(key, file);
            // 5. 获取文件信息，返回封装结构
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject, key);
            }
            // 封装原图返回结果
            return buildResult(originFilename, file, key, imageInfo);
        } catch (Exception e) {
            // 打印日志抛出异常
            log.error("上传图片到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验输入源书否合法
     *
     * @param inputSource 输入源
     */
    protected abstract String validPicture(Object inputSource);

    /**
     * 获取原始文件名
     *
     * @param inputSource 输入源
     * @return 原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理文件来源
     *
     * @param inputSource 输入源
     * @param file        文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装图片信息
     *
     * @param imageInfo      图片信息
     * @param key            上传地址
     * @param originFilename 原始图片名
     * @param file           文件
     * @return 封装图片信息对象
     */
    private UploadPictureResult buildResult(String originFilename, File file, String key, ImageInfo imageInfo) {
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + key);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果（压缩图）
     *
     * @param originFilename     图片名
     * @param compressedCiObject 压缩对象webp
     * @param thumbnailCiObject  缩略图
     * @param key                原图key用于删除
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, String key) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setOldUrl(key);
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicColor(cosManager.getImageAve(key));
        return uploadPictureResult;
    }

    /**
     * 删除临时图片
     *
     * @param file 待删除临时图片
     */
    public void deleteTempFile(File file) {
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete fail, file path = {}", file.getAbsolutePath());
            }
        }
    }
}
