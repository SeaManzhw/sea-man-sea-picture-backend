package com.seaman.seamanseapicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seaman.seamanseapicturebackend.model.dto.space.analyze.*;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
 * @author SeaManzhw
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间资源使用分析
     *
     * @param spaceUsageAnalyzeRequest 空间资源使用分析请求
     * @param loginUser                登录用户
     * @return 空间资源使用分析响应
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类分析请求
     * @param loginUser                   登录用户
     * @return 空间分类分析响应
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签分析
     *
     * @param spaceTagAnalyzeRequest 获取空间标签分析请求
     * @param loginUser              登录用户
     * @return 获取空间标签分析响应
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取图片大小分析
     *
     * @param spaceSizeAnalyzeRequest 获取图片大小分析请求
     * @param loginUser               登录用户
     * @return 获取图片大小分析响应
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 获取用户上传行为分析请求
     * @param loginUser               登录用户
     * @return 获取用户上传行为分析响应
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 管理员对空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 对空间使用排行分析请求
     * @param loginUser               登录用户
     * @return 对空间使用排行分析响应
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
