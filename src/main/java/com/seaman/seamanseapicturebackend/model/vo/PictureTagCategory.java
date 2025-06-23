package com.seaman.seamanseapicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片标签分类列表视图
 */
@Data
public class PictureTagCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 分类列表
     */
    private List<String> categoryList;
}
