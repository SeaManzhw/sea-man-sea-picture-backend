package com.seaman.seamanseapicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.seaman.seamanseapicturebackend.manager.auth.SpaceUserAuthManager;
import com.seaman.seamanseapicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.seaman.seamanseapicturebackend.model.entity.Picture;
import com.seaman.seamanseapicturebackend.model.entity.Space;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.model.enums.SpaceTypeEnum;
import com.seaman.seamanseapicturebackend.service.PictureService;
import com.seaman.seamanseapicturebackend.service.SpaceService;
import com.seaman.seamanseapicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket拦截器，建立连接前要先校验
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 建立连接前校验并设置参数
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return 校验是否成功
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 校验参数与权限
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        // 校验用户
        User loginUser = userService.getLoginUser(servletRequest);
        if (ObjUtil.isEmpty(loginUser)) {
            log.error("用户未登录，拒绝握手");
            return false;
        }
        // 校验图片
        String pictureId = servletRequest.getParameter("pictureId");
        if (StrUtil.isBlank(pictureId)) {
            log.error("缺少图片参数，拒绝握手");
            return false;
        }
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            log.error("图片不存在，拒绝握手");
            return false;
        }
        // 校验空间
        Long spaceId = picture.getSpaceId();
        if (spaceId == null || spaceId <= 0) {
            log.error("非法空间参数，拒绝握手");
            return false;
        }
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            log.error("非法空间，拒绝握手");
            return false;
        }
        if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
            log.error("非团队空间，拒绝握手");
            return false;
        }
        // 设置参数
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
            log.error("没有图片编辑权限，拒绝握手");
            return false;
        }
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", Long.valueOf(pictureId)); // 记得转换为 Long 类型
        return true;

    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
