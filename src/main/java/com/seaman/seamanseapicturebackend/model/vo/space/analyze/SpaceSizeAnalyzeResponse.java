package com.seaman.seamanseapicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片大小分析请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 图片大小范围
     */
    private String sizeRange;
    /**
     * 图片数量
     */
    private Long count;
}
