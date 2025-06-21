package com.seaman.seamanseapicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
* @author SeaManzhw
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-06-21 20:57:26
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

}




