package org.cuber.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.cuber.example.component.RabbitmqConstant;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MqListener {
    //延迟发送
    @RabbitListener(bindings = @QueueBinding(value = @Queue("test_delay"),
            exchange = @Exchange(value = RabbitmqConstant.DELAY_EXCHANGE, type = RabbitmqConstant.DELAY_EXCHANGE_TYPE, arguments =
            @Argument(name = RabbitmqConstant.DELAY_TYPE, value = ExchangeTypes.TOPIC)), key = "test_delay"))
    public void rabbitmqDelay(User gatewayAccess) {
        log.info("delayMessage:[{}]", gatewayAccess);
    }

    //点到点发送
    @RabbitListener(queuesToDeclare = @Queue("test_p2p"))
    public void rabbitmq_p2p(User gatewayAccess){
        log.info("point_point:[{}]", gatewayAccess);
    }

    //广播a
    @RabbitListener(bindings = @QueueBinding(value = @Queue("a"),
            exchange = @Exchange(value = "test_fanout", type = ExchangeTypes.FANOUT)))
    public void fanout_a(User gatewayAccess) {
        log.info("fanout_a:[{}]", gatewayAccess);
    }

    //广播b
    @RabbitListener(bindings = @QueueBinding(value = @Queue("b"),
            exchange = @Exchange(value = "test_fanout", type = ExchangeTypes.FANOUT)))
    public void fanout_b(User gatewayAccess) {
        log.info("fanout_a:[{}]", gatewayAccess);
    }

    //topic
    @RabbitListener(bindings = @QueueBinding(value = @Queue("topic_a"),
            exchange = @Exchange(value = "test_topic", type = ExchangeTypes.TOPIC), key = "topic_a"))
    public void topic_a(User gatewayAccess) {
        log.info("topic_a:[{}]", gatewayAccess);
    }

    //topic
    @RabbitListener(bindings = @QueueBinding(value = @Queue("topic_b"),
            exchange = @Exchange(value = "test_topic", type = ExchangeTypes.TOPIC), key = "topic_b"))
    public void topic_b(User gatewayAccess) {
        log.info("topic_b:[{}]", gatewayAccess);
    }

    //Rabbitmq2ErrorHandler
    @RabbitListener(bindings = @QueueBinding(value = @Queue("test_error"),
            exchange = @Exchange(value = RabbitmqConstant.DELAY_EXCHANGE, type = RabbitmqConstant.DELAY_EXCHANGE_TYPE, arguments =
            @Argument(name = RabbitmqConstant.DELAY_TYPE, value = ExchangeTypes.TOPIC)), key = "test_error"), errorHandler = "rabbitmq2ErrorHandler")
    public void test_error(User gatewayAccess)  {
        log.info("test_error[{}]", gatewayAccess);
        throw new RuntimeException("故意出错");
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue("test_error" + RabbitmqConstant._DEAD_CALL),
            exchange = @Exchange(value = RabbitmqConstant.DELAY_EXCHANGE, type = RabbitmqConstant.DELAY_EXCHANGE_TYPE, arguments =
            @Argument(name = RabbitmqConstant.DELAY_TYPE, value = ExchangeTypes.TOPIC)), key = "test_error" + RabbitmqConstant._DEAD_CALL))
    public void test_error_dead_call(User gatewayAccess)  {
        log.info("test_error_dead_call[{}]", gatewayAccess);
    }

}
