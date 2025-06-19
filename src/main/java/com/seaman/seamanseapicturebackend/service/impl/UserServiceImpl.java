package com.seaman.seamanseapicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.service.UserService;
import com.seaman.seamanseapicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author SeaManzhw
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-06-19 16:46:30
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




