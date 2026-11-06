package com.susan.training.integration.message;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 只有演示 RocketMQ 章节时才创建并启动真实客户端。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter4.rocketmq",
        name = "enabled",
        havingValue = "true"
)
public class RocketMqIntegrationConfiguration {

    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer rocketMqProducer(
            @Value("${demo.chapter4.rocketmq.namesrv:127.0.0.1:9876}") String namesrv,
            @Value("${demo.chapter4.rocketmq.producer-group:chapter4-producer}") String group
    ) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(group);
        producer.setNamesrvAddr(namesrv);
        producer.start();
        return producer;
    }

    @Bean
    public OrderMessagePublisher orderMessagePublisher(
            DefaultMQProducer producer,
            @Value("${demo.chapter4.rocketmq.topic:order-created}") String topic
    ) {
        return new OrderMessagePublisher(producer, topic);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            prefix = "demo.chapter4.rocketmq",
            name = "consume",
            havingValue = "true"
    )
    public DefaultMQPushConsumer rocketMqConsumer(
            @Value("${demo.chapter4.rocketmq.namesrv:127.0.0.1:9876}") String namesrv,
            @Value("${demo.chapter4.rocketmq.consumer-group:chapter4-consumer}") String group,
            @Value("${demo.chapter4.rocketmq.topic:order-created}") String topic,
            OrderEventConsumer listener
    ) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(namesrv);
        consumer.subscribe(topic, "*");
        consumer.registerMessageListener(listener);
        consumer.start();
        return consumer;
    }

    @Bean
    public OrderEventConsumer orderEventConsumer() {
        return new OrderEventConsumer();
    }
}
