package com.susan.training.integration.message;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 订单事件消费者，演示成功确认与失败重试的返回语义。
 */
public class OrderEventConsumer implements MessageListenerConcurrently {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(
            List<MessageExt> messages,
            org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext context
    ) {
        for (MessageExt message : messages) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("received order event: msgId={}, body={}", message.getMsgId(), body);
            // 教学示例直接确认；生产端应保证业务处理幂等后再返回成功。
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
