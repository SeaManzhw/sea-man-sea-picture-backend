package com.seaman.seamanseapicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.seaman.seamanseapicturebackend.manager.websocket.PictureEditHandler;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.seaman.seamanseapicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.seaman.seamanseapicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片编辑时间处理器（消费者）
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    /**
     * 处理事件
     *
     * @param pictureEditEvent 图片编辑事件
     * @throws Exception 异常
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 获取参数
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        Long pictureId = pictureEditEvent.getPictureId();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                pictureEditHandler.handleErrorMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
        }
    }
}
