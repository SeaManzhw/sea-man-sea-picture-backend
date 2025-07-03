package com.seaman.seamanseapicturebackend.api.imagesearch.baidu;

import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.model.ImageSearchResult;
import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.sub.GetImageFirstUrlApi;
import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.sub.GetImageListApi;
import com.seaman.seamanseapicturebackend.api.imagesearch.baidu.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 门面模式对外提供接口
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl 图片URL
     * @return 返回结果列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
