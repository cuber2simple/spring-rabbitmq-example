package org.cuber.example;

import lombok.extern.slf4j.Slf4j;
import org.cuber.example.component.RabbitmqPlusTemplate;
import org.cuber.example.demo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RabbitmqApplication.class)
@Slf4j
public class RabbitmqTest {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RabbitmqPlusTemplate rabbitmqPlusTemplate;

    @Test
    public void doTest() throws Exception {
        User gatewayAccess = new User();
        gatewayAccess.setUsername("cuber");
        gatewayAccess.setAge(21);
        //延迟发送2S
        rabbitmqPlusTemplate.sendDelay("test_delay", gatewayAccess, TimeUnit.SECONDS, 2l);
        //点到点发送
        rabbitTemplate.convertAndSend("test_p2p", gatewayAccess);
        //广播放松
        rabbitTemplate.convertAndSend("test_fanout", "", gatewayAccess);
        //路由发送
        rabbitTemplate.convertAndSend("test_topic", "topic_a", gatewayAccess);

        rabbitTemplate.convertAndSend("test_topic", "topic_b", gatewayAccess);
        //超过Integer最大值
        rabbitmqPlusTemplate.sendDelay("test_delay", gatewayAccess, TimeUnit.DAYS, 3l);
        //测试一个seq
        rabbitmqPlusTemplate.sendSequence("test_error", gatewayAccess, "pt5s,pt20s,pt1m");
        //休息两秒
        TimeUnit.MINUTES.sleep(4l);
    }
}
