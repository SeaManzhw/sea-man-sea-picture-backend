package com.seaman.seamanseapicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.seaman.seamanseapicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 图片编辑 disruptor 生产者
 */
@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布事件
     *
     * @param pictureEditRequestMessage 编辑请求信息
     * @param session                   会话
     * @param user                      用户
     * @param pictureId                 图片id
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                             User user, Long pictureId) {
        // 获取位置
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        long next = ringBuffer.next();
        // 构造事件
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setPictureId(pictureId);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
