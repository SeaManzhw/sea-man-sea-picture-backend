package com.seaman.seamanseapicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.seaman.seamanseapicturebackend.model.entity.User;
import com.seaman.seamanseapicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    /**
     * 连接建立成功后操作
     *
     * @param session 连接会话
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将session保存到集合会话中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造广播信息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播信息给所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 处理消息
     *
     * @param session 当前回话
     * @param message 消息
     * @throws Exception 异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析消息
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从session中获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 根据不同消息进行处理
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                handleErrorMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
        }
    }

    /**
     * 进入编辑消息
     *
     * @param pictureEditRequestMessage 消息
     * @param session                   当前会话
     * @param user                      用户
     * @param pictureId                 图片id
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                       User user, Long pictureId) throws IOException {
        // 当没有用户正在编辑时
        if (pictureEditingUsers.containsKey(pictureId)) {
            return;
        }
        // 设置用户正在编辑图片
        pictureEditingUsers.put(pictureId, user.getId());
        // 构造响应，发送进入编辑广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        String message = String.format("用户 %s 开始编辑图片", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 编辑动作消息
     *
     * @param pictureEditRequestMessage 消息
     * @param session                   当前会话
     * @param user                      用户
     * @param pictureId                 图片id
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                        User user, Long pictureId) throws IOException {
        // 获取正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId == null || !editingUserId.equals(user.getId())) {
            return;
        }
        // 判断操作是否合法
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (pictureEditActionEnum == null) {
            log.error("无效编辑动作");
            return;
        }
        // 构造响应，发送进入编辑广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        String message = String.format("%s 执行了 %s 操作", user.getUserName(), pictureEditActionEnum.getText());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        pictureEditResponseMessage.setEditAction(editAction);
        // 为其他会话广播
        broadcastToPicture(pictureId, pictureEditResponseMessage, session);
    }

    /**
     * 退出编辑消息
     *
     * @param pictureEditRequestMessage 消息
     * @param session                   当前会话
     * @param user                      用户
     * @param pictureId                 图片id
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws IOException {
        // 获取正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId == null || !editingUserId.equals(user.getId())) {
            return;
        }
        pictureEditingUsers.remove(pictureId);
        // 构造响应，发送进入编辑广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        String message = String.format("%s 结束了编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 为其他会话广播
        broadcastToPicture(pictureId, pictureEditResponseMessage, session);


    }

    /**
     * 错误消息
     *
     * @param pictureEditRequestMessage 消息
     * @param session                   当前会话
     * @param user                      用户
     * @param pictureId                 图片id
     */
    private void handleErrorMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                    User user, Long pictureId) throws IOException {
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setMessage("消息类型错误");
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
        String JsonMessage = toJsonWithLongType(pictureEditResponseMessage);
        TextMessage textMessage = new TextMessage(JsonMessage);
        session.sendMessage(textMessage);
    }

    /**
     * 关闭连接释放资源
     *
     * @param session 会话
     * @param status  关闭状态
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (webSocketSessions != null) {
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        // 构造广播消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        String message = String.format("%s 离开了编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 为其他会话广播
        broadcastToPicture(pictureId, pictureEditResponseMessage, session);
    }

    /**
     * 将对象保留long精度转为Json
     *
     * @param object 需要序列化的对象
     * @param <T>    对象类型
     * @return Json格式字符串
     * @throws JsonProcessingException 抛出异常
     */
    private <T> String toJsonWithLongType(T object) throws JsonProcessingException {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
        objectMapper.registerModule(module);
        // 序列化为JSON字符串
        return objectMapper.writeValueAsString(object);
    }

    /**
     * 发送广播，排除某个回话
     *
     * @param pictureId                  图片id
     * @param pictureEditResponseMessage 图片编辑信息
     * @param excludeSession             排除广播会话
     * @throws IOException IO异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isEmpty(webSocketSessions)) {
            return;
        }
        // 序列化为JSON字符串
        String message = toJsonWithLongType(pictureEditResponseMessage);
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : webSocketSessions) {
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    /**
     * 发送广播，不排除会话
     *
     * @param pictureId                  图片id
     * @param pictureEditResponseMessage 图片编辑信息
     * @throws IOException IO异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }


}
