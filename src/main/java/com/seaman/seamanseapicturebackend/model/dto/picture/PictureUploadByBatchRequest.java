package com.seaman.seamanseapicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 搜索词
     */
    private String searchText;
    /**
     * 抓取数量
     */
    private Integer count = 10;
    /**
     * 图片名称前缀
     */
    private String namePrefix;

}
