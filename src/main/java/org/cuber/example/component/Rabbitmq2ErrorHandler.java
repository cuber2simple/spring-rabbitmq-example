package org.cuber.example.component;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.cuber.example.component.RabbitmqConstant.*;


@Slf4j
public class Rabbitmq2ErrorHandler implements RabbitListenerErrorHandler {

    private RabbitTemplate rabbitTemplate;

    private AmqpAdmin amqpAdmin;


    public Rabbitmq2ErrorHandler(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public Object handleError(Message message, org.springframework.messaging.Message<?> message1, ListenerExecutionFailedException e) throws Exception {
        String receivedExchange = message.getMessageProperties().getReceivedExchange();
        String consumerQueue = message.getMessageProperties().getConsumerQueue();
        log.warn("[{}]消费消息失败:{}", consumerQueue, message);
        log.error("消费失败", e.getCause());
        try {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            String resendQueue = resendQueue(headers, consumerQueue);
            if (StringUtils.isNotBlank(resendQueue)) {
                Message resendMessage = MessageBuilder.withBody(message.getBody())
                        .setContentType(message.getMessageProperties().getContentType())
                        .copyHeaders(headers)
                        .build();
                rabbitTemplate.send(receivedExchange, resendQueue, resendMessage);
            } else {
                log.warn("消息不会重新发送没有设置deadCallQueue");
            }
        } catch (Exception error) {
            log.error("错误处理遇见问题", error);
        }
        return null;
    }

    public String resendQueue(Map<String, Object> headers, String consumerQueue) {
        String dealCall = getDealCallQueue(consumerQueue);
        String queueName = dealCall;
        if (MapUtils.isNotEmpty(headers) && headers.containsKey(DELAY_SEQUENCE)) {
            String[] durations = StringUtils.split(MapUtils.getString(headers, DELAY_SEQUENCE), ",");
            Integer index = MapUtils.getInteger(headers, DELAY_SEQUENCE_CURRENT) + 1;
            if (index < durations.length) {
                headers.put(DELAY_SEQUENCE_CURRENT, index);
                Duration duration = Duration.parse(durations[index]);
                headers.put(MessageProperties.X_DELAY, duration.toMillis());
                queueName = consumerQueue;
            } else {
                headers.remove(DELAY_SEQUENCE);
            }
        }
        //如果转到deal_call
        if (StringUtils.equals(queueName, dealCall)) {
            QueueInformation dealCallQueue = amqpAdmin.getQueueInfo(dealCall);
            if (Objects.isNull(dealCallQueue)) {
                queueName = StringUtils.EMPTY;
            }
        }
        return queueName;
    }

    private String getDealCallQueue(String consumerQueue) {
        return consumerQueue + _DEAD_CALL;
    }
}
