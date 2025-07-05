package com.seaman.seamanseapicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.exception.BusinessException;
import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import com.seaman.seamanseapicturebackend.exception.ThrowUtils;
import com.seaman.seamanseapicturebackend.mapper.SpaceMapper;
import com.seaman.seamanseapicturebackend.model.dto.space.analyze.*;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.vo.space.analyze.*;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.SpaceAnalyzeService;
import com.seaman.seamanseapicturebackend.service.SpaceService;
import com.seaman.seamanseapicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SeaMan
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 获取空间资源使用分析
     *
     * @param spaceUsageAnalyzeRequest 空间资源使用分析请求
     * @param loginUser                登录用户
     * @return 空间资源使用分析响应
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        // 分析公共空间
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 构造查询条件
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            // 查询数据
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
            long usedCount = pictureObjList.size();
            // 构造返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setMaxSize(usedSize);
            spaceUsageAnalyzeResponse.setMaxCount(usedCount);
            // 公共图库不存在上限
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        }
        // 分析私人空间
        Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);// 构造返回结果
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
        spaceUsageAnalyzeResponse.setMaxSize(space.getTotalSize());
        spaceUsageAnalyzeResponse.setMaxCount(space.getTotalCount());
        // 公共图库不存在上限
        double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
        double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
        spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
        spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
        spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
        spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
        return spaceUsageAnalyzeResponse;
    }

    /**
     * 获取空间分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类分析请求
     * @param loginUser                   登录用户
     * @return 空间分类分析响应
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        // 查询数据库并构造返回结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .filter(result -> result.get("category") != null)
                .map(result -> {
                    String category = (String) result.get("category");
                    System.out.println("category = " + category);
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 获取空间标签分析
     *
     * @param spaceTagAnalyzeRequest 获取空间标签分析请求
     * @param loginUser              登录用户
     * @return 获取空间标签分析响应
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        // 查询数据库并构造返回结果
        // 查询数据库
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        // 转map计数
        Map<String, Long> tagCountMap = tagJsonList.stream()
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 排序并构造返回对象
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取图片大小分析
     *
     * @param spaceSizeAnalyzeRequest 获取图片大小分析请求
     * @param loginUser               登录用户
     * @return 获取图片大小分析响应
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        // 查询数据库并构造返回结果
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(size -> (Long) size)
                .collect(Collectors.toList());
        // 使用有序 Map 定义分段范围
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

    }

    /**
     * 获取用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 获取用户上传行为分析请求
     * @param loginUser               登录用户
     * @return 获取用户上传行为分析响应
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            case "year":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询数据
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        // 构造返回参数
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 管理员对空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 对空间使用排行分析请求
     * @param loginUser               登录用户
     * @return 对空间使用排行分析响应
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        return spaceService.list(queryWrapper);
    }


    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest 空间分析请求
     * @param loginUser           登录用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        // 全图库分析或分析公共图库
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 分析私人空间，仅本人和管理员可以访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据请求对象封装查询条件
     *
     * @param spaceAnalyzeRequest 空间分析请求
     * @param queryWrapper        查询条件
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

}




