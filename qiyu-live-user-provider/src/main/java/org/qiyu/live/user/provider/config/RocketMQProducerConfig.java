package org.qiyu.live.user.provider.config;


import jakarta.annotation.Resource;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class RocketMQProducerConfig {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQProducerConfig.class);

    @Resource
    private RocketMQProducerProperties producerProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public MQProducer mqProducer() {
        ThreadPoolExecutor asyncThreadExecutor = new ThreadPoolExecutor(100, 150, 3, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread();
                thread.setName(applicationName + ":rmq-producer" + ThreadLocalRandom.current().nextInt(1000));
                return thread;
            }
        });
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer();
        try {
            defaultMQProducer.setNamesrvAddr(producerProperties.getNameSer());
            defaultMQProducer.setProducerGroup(producerProperties.getGroupName());
            defaultMQProducer.setRetryTimesWhenSendFailed(producerProperties.getRetryTimes());
            defaultMQProducer.setRetryTimesWhenSendAsyncFailed(producerProperties.getRetryTimes());
            defaultMQProducer.setRetryAnotherBrokerWhenNotStoreOK(true);
            defaultMQProducer.setAsyncSenderExecutor(asyncThreadExecutor);
            defaultMQProducer.start();
            logger.info("mq生产者启动成功,nameSrv is {}",producerProperties.getNameSer());
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
        return defaultMQProducer;
    }
}
