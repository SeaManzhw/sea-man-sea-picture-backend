package com.seaman.seamanseapicturebackend.manager.upload;


import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.seaman.seamanseapicturebackend.config.CosClientConfig;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * COSBean
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片（获取图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        thumbnailRule.setBucket(cosClientConfig.getBucket());
        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
        thumbnailRule.setFileId(thumbnailKey);
        // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
        thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
        rules.add(thumbnailRule);
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 对象URL
     */
    public void deleteObject(String key) {
        //处理字符串，去除前缀网址
        int index = key.indexOf("com/");
        if (index >= 0) {
            key = key.substring(index + 4); // 4是"com/"的长度
        }
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 获取图片主色调
     *
     * @param key 文件 key
     * @return 图片主色调
     */
    public String getImageAve(String key) {
        // 1. 构造获取对象请求，指定桶名和文件key
        GetObjectRequest getObj = new GetObjectRequest(cosClientConfig.getBucket(), key);
        // 2. 设置自定义图片处理参数，imageAve 表示请求主色调信息
        String rule = "imageAve";
        getObj.putCustomQueryParameter(rule, null);
        // 3. 通过cosClient获取对象，返回COSObject
        COSObject object = cosClient.getObject(getObj);
        // 4. 获取对象内容的输入流（包含主色调信息的HTTP请求）
        try (COSObjectInputStream objectContent = object.getObjectContent()) {
            // 5. 创建字节输出流用于存储读取到的数据
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            // 6. 循环读取输入流内容到字节数组，直到读取完毕
            while ((len = objectContent.read(buffer)) != -1) {
                result.write(buffer, 0, len);
            }
            // 7. 将字节输出流内容转换为字符串（假设为JSON格式）
            String json = new String(result.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            // 8. 解析JSON字符串，获取RGB字段的值
            String rgb = JSONUtil.parseObj(json).getStr("RGB");
            // 9. 如果RGB字段为null，抛出自定义异常
            ThrowUtils.throwIf(rgb == null, ErrorCode.SYSTEM_ERROR, "未获取到图片主色调");
            // 10. 返回RGB主色调字符串
            return rgb;
        } catch (IOException e) {
            // 11. 发生IO异常时，抛出自定义业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
