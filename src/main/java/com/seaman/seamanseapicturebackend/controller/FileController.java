package com.seaman.seamanseapicturebackend.controller;


import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.seaman.seamanseapicturebackend.annotation.AuthCheck;
import com.seaman.seamanseapicturebackend.common.BaseResponse;
import com.seaman.seamanseapicturebackend.common.ResultUtils;
import com.seaman.seamanseapicturebackend.constant.UserConstant;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 文件控制类
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile 待上传的文件
     * @return 上传后的文件地址
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart MultipartFile multipartFile) {
        // 获取文件目录
        String originalFilename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", originalFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            // 上传文件
            cosManager.putObject(filePath, file);
            // 返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("upload file error, filePath = {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        } finally {
            // 删除临时文件
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filePath = {}", filePath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 下载文件路径
     * @param response 响应头
     * @throws IOException 抛出IO异常
     */
    @GetMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            // 获取存储对象输入流
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 将输入流转换为字节数组
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            //写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}
