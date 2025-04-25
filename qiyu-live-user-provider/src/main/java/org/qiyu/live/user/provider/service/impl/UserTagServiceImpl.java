package org.qiyu.live.user.provider.service.impl;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.idea.qiyu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.qiyu.live.common.interfaces.ConvertBeanUtils;
import org.qiyu.live.user.constants.CacheAsyncDeleteCode;
import org.qiyu.live.user.constants.UserProviderTopicNames;
import org.qiyu.live.user.constants.UserTagFieldNameConstants;
import org.qiyu.live.user.constants.UserTagsEnum;
import org.qiyu.live.user.dto.UserCacheAsyncDeleteDTO;
import org.qiyu.live.user.dto.UserTagDTO;
import org.qiyu.live.user.provider.dao.mapper.IUserTagMapper;
import org.qiyu.live.user.provider.dao.po.UserTagPO;
import org.qiyu.live.user.provider.service.IUserTagService;
import org.qiyu.live.user.utils.TagInfoUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserTagServiceImpl implements IUserTagService {

    @Resource
    private IUserTagMapper userTagMapper;
    @Resource
    private RedisTemplate<String, UserTagDTO> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;
    @Resource
    private MQProducer mqProducer;

    @Override
    public boolean setTag(Long userId, UserTagsEnum userTagsEnum) {
        boolean updateStatus = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateStatus) {
            deleteUserTagDTOFromRedis(userId);
            return true;
        }
        String setNxKey = cacheKeyBuilder.buildTagLockKey(userId);
        String setNxResult = redisTemplate.execute(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
                return (String) connection.execute("set", keySerializer.serialize(setNxKey),
                        valueSerializer.serialize("-1"),
                        "NX".getBytes(StandardCharsets.UTF_8),
                        "EX".getBytes(StandardCharsets.UTF_8),
                        "3".getBytes(StandardCharsets.UTF_8));
            }
        });
        if (!"OK".equals(setNxResult)) {
            return false;
        }
        UserTagPO userTagPO = userTagMapper.selectById(userId);
        if (userTagPO != null) {
            return false;
        }
        userTagPO = new UserTagPO();
        userTagPO.setUserId(userId);
        userTagMapper.insert(userTagPO);
        updateStatus = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        System.out.println("测试更新标签成功");
        redisTemplate.delete(setNxKey);
        return updateStatus;
    }

    @Override
    public boolean cancelTag(Long userId, UserTagsEnum userTagsEnum) {
        boolean cancelStatus = userTagMapper.cancelTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (!cancelStatus) {
            return false;
        }
        deleteUserTagDTOFromRedis(userId);
        return true;
    }

    @Override
    public boolean containTag(Long userId, UserTagsEnum userTagsEnum) {
        UserTagDTO userTagDTO = this.queryByUserIdFromRedis(userId);
        if (userTagDTO == null) {
            return false;
        }
        String queryFieldName = userTagsEnum.getFieldName();
        if (UserTagFieldNameConstants.TAG_INF0_01.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo01(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INF0_02.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo02(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INF0_03.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo03(), userTagsEnum.getTag());
        }
        return false;
    }

    private void deleteUserTagDTOFromRedis(Long userId) {
        String redisKey = cacheKeyBuilder.buildTagLockKey(userId);
        redisTemplate.delete(redisKey);
        UserCacheAsyncDeleteDTO userCacheAsyncDeleteDTO = new UserCacheAsyncDeleteDTO();
        userCacheAsyncDeleteDTO.setCode(CacheAsyncDeleteCode.USER_TAG_DELETE.getCode());
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("userId", userId);
        userCacheAsyncDeleteDTO.setJson(JSON.toJSONString(jsonParam));
        Message message = new Message();
        message.setTopic(UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC);
        message.setBody(JSON.toJSONString(userCacheAsyncDeleteDTO).getBytes());
        message.setDelayTimeLevel(1);
        try {
            mqProducer.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserTagDTO queryByUserIdFromRedis(Long userId) {
        String redisKey = cacheKeyBuilder.buildTagLockKey(userId);
        UserTagDTO userTagDTO = redisTemplate.opsForValue().get(redisKey);
        if (userTagDTO != null) {
            return userTagDTO;
        }
        UserTagPO userTagPO = userTagMapper.selectById(userId);
        if (userTagPO == null) {
            return null;
        }
        userTagDTO = ConvertBeanUtils.convert(userTagPO, UserTagDTO.class);
        redisTemplate.opsForValue().set(redisKey, userTagDTO);
        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
        return userTagDTO;
    }
}
