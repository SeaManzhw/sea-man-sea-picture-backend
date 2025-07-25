package com.seaman.seamanseapicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片DTO
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 图片 id（用于修改）
     */
    private Long id;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 空间 id
     */
    private Long spaceId;
}
