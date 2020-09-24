package org.cuber.example.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cuber.example.component.Rabbitmq2ErrorHandler;
import org.cuber.example.component.RabbitmqPlusTemplate;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.annotation.Resource;

@EnableRabbit
@Configuration
public class RabbitmqConf {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public RabbitmqPlusTemplate getPlusTemplate(RabbitTemplate rabbitTemplate) {
        return new RabbitmqPlusTemplate(rabbitTemplate);
    }

    @Bean(name = "rabbitmq2ErrorHandler")
    public Rabbitmq2ErrorHandler getErrorHandler(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        return new Rabbitmq2ErrorHandler(rabbitTemplate, amqpAdmin);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @EventListener
    public void applicationReady(ApplicationReadyEvent applicationReadyEvent) {
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = applicationContext.getBean(Jackson2JsonMessageConverter.class);
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = applicationContext.getBean(SimpleRabbitListenerContainerFactory.class);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        simpleRabbitListenerContainerFactory.setMessageConverter(jackson2JsonMessageConverter);
    }

}
