package org.cuber.example.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static org.cuber.example.component.RabbitmqConstant.*;

@Slf4j
public class RabbitmqPlusTemplate {

    private RabbitTemplate rabbitTemplate;


    public RabbitmqPlusTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public <T> void sendDelay(String routingKey, T t, TimeUnit timeUnit, long times) {
        rabbitTemplate.convertAndSend(DELAY_EXCHANGE, routingKey, t, message -> {
            long timeDelay = timeUnit.toMillis(times);
            message.getMessageProperties().setHeader(MessageProperties.X_DELAY, timeDelay);
            return message;
        });
    }

    public <T> void sendSequence(String routingKey, T t, String sequence) {
        if (!isRight(sequence)) {
            throw new RuntimeException("[" + sequence + "]非法");
        }
        rabbitTemplate.convertAndSend(DELAY_EXCHANGE, routingKey, t, message -> {
            String[] durations = sequence.split(",");
            Duration duration = Duration.parse(durations[0]);
            message.getMessageProperties().setHeader(DELAY_SEQUENCE, sequence);
            message.getMessageProperties().setHeader(DELAY_SEQUENCE_CURRENT, 0);
            message.getMessageProperties().setHeader(MessageProperties.X_DELAY, duration.toMillis());
            return message;
        });
    }

    public static boolean isRight(String sequence) {
        boolean result = false;
        if (StringUtils.isNotBlank(sequence)) {
            String[] durations = sequence.split(",");
            result = Arrays.stream(durations).allMatch(RabbitmqPlusTemplate::match);
        }
        return result;
    }

    private static boolean match(String text) {
        Matcher matcher = PATTERN.matcher(text);
        return matcher.matches();
    }

}
