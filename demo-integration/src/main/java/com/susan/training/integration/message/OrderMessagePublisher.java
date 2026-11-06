package com.susan.training.integration.message;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 订单事件生产者。
 */
public class OrderMessagePublisher {
    private final DefaultMQProducer producer;
    private final String topic;

    public OrderMessagePublisher(DefaultMQProducer producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    public SendResult publish(String tag, String payload) throws Exception {
        Message message = new Message(
                topic,
                tag,
                payload.getBytes(StandardCharsets.UTF_8)
        );
        // 正式项目应同时传入业务唯一键，并使用同步发送结果更新 Outbox 状态。
        return producer.send(message);
    }
}
