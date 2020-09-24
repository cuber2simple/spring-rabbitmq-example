#1. mq的选型
  |         | rabbitq          | kafka|  rocketmq[开源]  | activemq| rocketmq[ali]|SQS[aws]|
|:-------------:|:----------:|:---------:|:------:|:-----:|:-----:|:-----:|
| 云服务支持      | &times;(aws)&radic;(ali)| &radic; |&times; |&radic; |&radic; |&radic; |
| 性能     | 3     |   5|4 |2 | 5| 5|
| 易用性 | 5   |   4 |3 |5 |3 |3 |
| 功能性| 高级特性支持mqtt/延迟  |   不支持 |手动性[^1] |支持延迟 |支持延迟 |支持延迟(最大15分钟延时) |
更多参数对比请参照[链接](https://blog.csdn.net/belvine/article/details/80842240 "链接")
    
#2. rabbitmq基础知识普及
+     生产者（Producer）：发送消息的应用   
+     消费者（Consumer）：接收消息的应用。
+     队列（Queue）：存储消息的缓存。
+     消息（Message）：由生产者通过RabbitMQ发送给消费者的信息。由（headers[hashmap]和payload）组成
+     连接（Connection）：连接RabbitMQ和应用服务器的TCP连接
+     通道（Channel）：连接里的一个虚拟通道。当你通过消息队列发送或者接收消息时，这个操作都是通过通道进行的。
+     连接（Connection）：连接RabbitMQ和应用服务器的TCP连接
+     交换机（Exchange）：交换机负责从生产者那里接收消息，并根据交换类型分发到对应的消息列队里。要实现消息的接收，一个队列必须到绑定一个交换机。
$\color{#FF0000}{存在的意义:AMQP 协议中的核心思想就是生产者和消费者的解耦}$
1. direct--------------------------点到点的模式
2. fanout-------------------------广播模式
3. topic---------------------------路由模式(rountingKey) ~~正则表达式~~
4. ~~header------------------------路由模式(headers)~~
5. x-delayed-message-------需要安装延迟队列插件标识(x-delayed-type 标志了这个队列除了是个延迟队列，他还属于上面4种队列种的一个)
![在管理界面新建延时exchange.png](https://i.loli.net/2020/09/23/DzH4tRXguNaQVAZ.png)
安装延迟队列插件:
    + [插件](https://www.rabbitmq.com/community-plugins.html "插件")下载  rabbitmq_delayed_message_exchange.ez【请下载rabbitmq对应的版本】
    + 放在rabbitmq安装目录的plugins目录下
    + 执行`rabbitmq-plugins enable rabbitmq_delayed_message_exchange`
+     绑定（Binding）：绑定是队列和交换机的一个关联连接。
+     路由键（Routing Key）：路由键是供交换机查看并根据键来决定如何分发消息到列队的一个键。路由键可以说是消息的目的地址。
![图片来源https://www.jianshu.com/p/256c502d09cd](https://upload-images.jianshu.io/upload_images/15423847-0c9b03fc31e8ac5a.jpg-itluobo?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)
 #3.rabbitmq在java的运用
   因为我们经常使用`spring boot`，所以**cuber**决定选用`spring-boot-starter-amqp`
   引用依赖
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
``` 
application.yml设置如下：

详细参数设置说明 https://www.cnblogs.com/qts-hope/p/11242559.html
```yml
spring:
    rabbitmq:
        addresses: 10.8.3.95:5672
        username: admin
        password: admin
        virtual-host: rabbitmq_vhost
```
> 输入输出jackson
``` java
    @Bean
    //定义message 转换类[使用spring 已有的jackson bean objectMapper(这样定义的jackson配置参数对rabbitmq也共用一套)]
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @EventListener
    // 不自定义rabbitTemplate 和 Jackson2JsonMessageConverter 是因为不影响配置参数被清空
    public void applicationReady(ApplicationReadyEvent applicationReadyEvent) {
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = applicationContext.getBean(Jackson2JsonMessageConverter.class);
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = applicationContext.getBean(SimpleRabbitListenerContainerFactory.class);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        simpleRabbitListenerContainerFactory.setMessageConverter(jackson2JsonMessageConverter);
    }
```
> 发送延迟队列
``` java
//1.所有的延迟队列都使用一个交换器（exchange）， 不应该使用网上现有的direct的延迟队列
//2.延迟参数其实可以为long类型， 最大为2^31-1 毫秒， 也就是说最大延时大概是49天多一点
public <T> void sendDelay(String routingKey, T t, TimeUnit timeUnit, long times) {
        rabbitTemplate.convertAndSend("x-delay-exchange", routingKey, t, message -> {
            long timeDelay = timeUnit.toMillis(times);
            message.getMessageProperties().setHeader(MessageProperties.X_DELAY, timeDelay);
            return message;
        });
    }
```
发送延时流 pt5s,pt20s,pt1m,pt1m,pt5m,pt30m,pt2h,p1d   
延迟5s执行， 执行失败延迟20s执行.....
结合message 的header 和延时队列来做 
>发送端 
```java
   //将延迟数组字符串放在头部，当前游标放置头部
    public <T> void sendSequence(String routingKey, T t, String sequence) {
        if (!isRight(sequence)) {
            throw new RuntimeException("[" + sequence + "]非法");
        }
        rabbitTemplate.convertAndSend("x-delay-exchange", routingKey, t, message -> {
            String[] durations = sequence.split(",");
            Duration duration = Duration.parse(durations[0]);
            message.getMessageProperties().setHeader("PHP-DELAY-SEQUENCE", sequence);
            message.getMessageProperties().setHeader("PHP-DELAY-SEQUENCE-CURRENT", 0);
            message.getMessageProperties().setHeader(MessageProperties.X_DELAY, duration.toMillis());
            return message;
        });
    }
    //延迟数组字符串是否合法
    public static boolean isRight(String sequence) {
        boolean result = false;
        if (StringUtils.isNotBlank(sequence)) {
            String[] durations = sequence.split(",");
            result = Arrays.stream(durations).allMatch(RabbitmqPlusTemplate::match);
        }
        return result;
    }
    //Duration 实现类的正则表达式
    public static final Pattern PATTERN =
            Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)D)?" +
                            "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
                    Pattern.CASE_INSENSITIVE);
    private static boolean match(String text) {
        Matcher matcher = PATTERN.matcher(text);
        return matcher.matches();
    }
````
>错误处理类
```java
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
        if (MapUtils.isNotEmpty(headers) && headers.containsKey("PHP-DELAY-SEQUENCE")) {
            String[] durations = StringUtils.split(MapUtils.getString(headers, "PHP-DELAY-SEQUENCE"), ",");
            Integer index = MapUtils.getInteger(headers, "PHP-DELAY-SEQUENCE-CURRENT") + 1;
            if (index < durations.length) {
                headers.put("PHP-DELAY-SEQUENCE-CURRENT", index);
                Duration duration = Duration.parse(durations[index]);
                headers.put(MessageProperties.X_DELAY, duration.toMillis());
                queueName = consumerQueue;
            } else {
                headers.remove("PHP-DELAY-SEQUENCE");
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
        return consumerQueue + "_DEAD_CALL";
    }
}
```
队列监听
```java
    //点到点发送
    @RabbitListener(queuesToDeclare = @Queue("test_p2p"))
    public void rabbitmq_p2p(GatewayAccess gatewayAccess) throws Exception {
        log.info("point_point:[{}]", gatewayAccess);
    }

    //广播a
    @RabbitListener(bindings = @QueueBinding(value = @Queue("a"),
            exchange = @Exchange(value = "test_fanout", type = ExchangeTypes.FANOUT)))
    public void fanout_a(GatewayAccess gatewayAccess) throws Exception {
        log.info("fanout_a:[{}]", gatewayAccess);
    }

    //广播b
    @RabbitListener(bindings = @QueueBinding(value = @Queue("b"),
            exchange = @Exchange(value = "test_fanout", type = ExchangeTypes.FANOUT)))
    public void fanout_b(GatewayAccess gatewayAccess) throws Exception {
        log.info("fanout_a:[{}]", gatewayAccess);
    }

    //topic
    @RabbitListener(bindings = @QueueBinding(value = @Queue("topic_a"),
            exchange = @Exchange(value = "test_topic", type = ExchangeTypes.TOPIC), key = "topic_a"))
    public void topic_a(GatewayAccess gatewayAccess) throws Exception {
        log.info("topic_a:[{}]", gatewayAccess);
    }

    //topic
    @RabbitListener(bindings = @QueueBinding(value = @Queue("topic_b"),
            exchange = @Exchange(value = "test_topic", type = ExchangeTypes.TOPIC), key = "topic_b"))
    public void topic_b(GatewayAccess gatewayAccess) throws Exception {
        log.info("topic_b:[{}]", gatewayAccess);
    }

    //Rabbitmq2ErrorHandler
    @RabbitListener(bindings = @QueueBinding(value = @Queue("test_error"),
            exchange = @Exchange(value = "x-delay-exchange", type = "x-delayed-message", arguments =
            @Argument(name ="x-delayed-type", value = ExchangeTypes.TOPIC)), key = "test_error"), errorHandler = "rabbitmq2ErrorHandler")
    public void test_error(GatewayAccess gatewayAccess) throws Exception {
        log.info("test_error[{}]", gatewayAccess);
        throw new RuntimeException("故意出错");
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue("test_error" + RabbitmqConstant._DEAD_CALL),
            exchange = @Exchange(value = "x-delay-exchange", type = "x-delayed-message", arguments =
            @Argument(name = "x-delayed-type", value = ExchangeTypes.TOPIC)), key = "test_error" + "_DEAD_CALL"))
    public void test_error_dead_call(GatewayAccess gatewayAccess) throws Exception {
        log.info("test_error_dead_call[{}]", gatewayAccess);
    }
```
[完整代码](https://github.com/cuber2simple/spring-rabbitmq-example "完整代码")
 1. 使用`@queuesToDeclare`会创建队列，当队列不存在的时候
 2.使用 `@QueueBinding`会创建exchange,bing,queue还有rountingKey

[^1]: 手动设置延迟时间，在rocketmq 启动的时候就需要设置好，以后都不能更改
[^2]:HyperText Markup Language 超文本标记语言