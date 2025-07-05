package com.seaman.seamanseapicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员对空间使用排行分析
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;
}
