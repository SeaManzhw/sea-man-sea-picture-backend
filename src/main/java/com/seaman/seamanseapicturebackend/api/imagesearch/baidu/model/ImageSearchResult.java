package com.seaman.seamanseapicturebackend.api.imagesearch.baidu.model;

import lombok.Data;

/**
 * 百度以图搜图返回类
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
