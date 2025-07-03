package com.seaman.seamanseapicturebackend.api.imagesearch.so;

import com.seaman.seamanseapicturebackend.api.imagesearch.so.model.SoImageSearchResult;
import com.seaman.seamanseapicturebackend.api.imagesearch.so.sub.GetSoImageListApi;
import com.seaman.seamanseapicturebackend.api.imagesearch.so.sub.GetSoImageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 360搜图图片搜索接口
 * 这里用了 门面模式
 */
@Slf4j
public class SoImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl 需要以图搜图的图片地址
     * @return 图片搜索结果列表
     */
    public static List<SoImageSearchResult> searchImage(String imageUrl) {
        String soImageUrl = GetSoImageUrlApi.getSoImageUrl(imageUrl);
        return GetSoImageListApi.getImageList(soImageUrl, 0);
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://sea-man-sea-picture-1365414139.cos.ap-nanjing.myqcloud.com/public/1935661316996423681/2025-06-27_iIeahY7ZcOxJotGn.webp";
        List<SoImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
