package com.seaman.seamanseapicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量编辑图片
 */
@Data
public class PictureEditByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 图片 id 列表
     */
    private List<Long> pictureIdList;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 分类
     */
    private String category;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 命名规则
     */
    private String nameRule;
}
