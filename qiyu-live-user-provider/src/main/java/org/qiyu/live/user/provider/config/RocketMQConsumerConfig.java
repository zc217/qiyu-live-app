package org.qiyu.live.user.provider.config;


import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.idea.qiyu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.qiyu.live.user.constants.CacheAsyncDeleteCode;
import org.qiyu.live.user.constants.UserProviderTopicNames;
import org.qiyu.live.user.dto.UserCacheAsyncDeleteDTO;
import org.qiyu.live.user.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Objects;

@Configuration
public class RocketMQConsumerConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQConsumerConfig.class);

    @Resource
    private RocketMQConsumerProperties consumerProperties;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        initConsumer();
    }

    private void initConsumer() {

        try {
            DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();
            defaultMQPushConsumer.setNamesrvAddr(consumerProperties.getNameSer());
            defaultMQPushConsumer.setConsumerGroup(consumerProperties.getGroupName());
            defaultMQPushConsumer.setConsumeMessageBatchMaxSize(1);
            defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            defaultMQPushConsumer.subscribe(UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC, "*");
            defaultMQPushConsumer.setMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    String json = new String(msgs.get(0).getBody());
                    UserCacheAsyncDeleteDTO userCacheAsyncDeleteDTO = JSON.parseObject(json, UserCacheAsyncDeleteDTO.class);
                    if (CacheAsyncDeleteCode.USER_INFO_DELETE.getCode() == userCacheAsyncDeleteDTO.getCode()) {
                        Long userId = JSON.parseObject(userCacheAsyncDeleteDTO.getJson()).getLong("userId");
                        redisTemplate.delete(cacheKeyBuilder.buildUserInfoKey(userId));
                        logger.info("延迟删除用户信息缓存，userId is {}", userId);
                    } else if (CacheAsyncDeleteCode.USER_TAG_DELETE.getCode() == userCacheAsyncDeleteDTO.getCode()) {
                        Long userId = JSON.parseObject(userCacheAsyncDeleteDTO.getJson()).getLong("userId");
                        redisTemplate.delete(cacheKeyBuilder.buildTagKey(userId));
                        logger.info("延迟删除用户标签缓存，userId is {}", userId);
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

//                    String msgStr = new String(msgs.get(0).getBody());
//                    UserDTO userDTO = JSON.parseObject(msgStr, UserDTO.class);
//                    if (userDTO == null || userDTO.getUserId() == null){
//                        logger.error("用户id为空，参数异常，内容：{}", msgStr);
//                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
//                    }
//                    redisTemplate.delete(userProviderCacheKeyBuilder.buildUserInfoKey(userDTO.getUserId()));
//                    logger.info("延迟删除处理，userDTO is {}", userDTO);
//                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            defaultMQPushConsumer.start();
            logger.info("mq消费者启动成功,nameSrv is {}", consumerProperties.getNameSer());
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }
}
