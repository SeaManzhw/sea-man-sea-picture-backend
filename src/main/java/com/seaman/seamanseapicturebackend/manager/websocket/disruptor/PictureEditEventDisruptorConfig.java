package com.seaman.seamanseapicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 图片编辑事件 Disruptor 配置
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    /**
     * 构造 Disruptor 队列
     *
     * @return 返回队列
     */
    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 设置大小
        int bufferSize = 1024 * 256;
        // 创建无锁队列
        Disruptor<PictureEditEvent> pictureEditEventDisruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        // 设置消费者
        pictureEditEventDisruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动无锁队列
        pictureEditEventDisruptor.start();
        return pictureEditEventDisruptor;
    }

}
