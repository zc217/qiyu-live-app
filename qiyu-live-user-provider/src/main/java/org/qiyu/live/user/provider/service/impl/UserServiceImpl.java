package org.qiyu.live.user.provider.service.impl;

import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.idea.qiyu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.qiyu.live.common.interfaces.ConvertBeanUtils;
import org.qiyu.live.user.dto.UserDTO;
import org.qiyu.live.user.provider.dao.mapper.IUserMapper;
import org.qiyu.live.user.provider.dao.po.UserPO;
import org.qiyu.live.user.provider.service.IUserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private IUserMapper userMapper;

    @Resource
    private RedisTemplate<String, UserDTO> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;

    @Override
    public UserDTO getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = userProviderCacheKeyBuilder.buildUserInfoKey(userId);
        UserDTO userDTO = redisTemplate.opsForValue().get(key);
        if (userDTO != null) {
            return userDTO;
        }
        userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
        if (userDTO != null) {
            redisTemplate.opsForValue().set(key, userDTO);
        }
        return userDTO;
    }

    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        userMapper.updateById(ConvertBeanUtils.convert(userDTO, UserPO.class));
        return true;
    }

    @Override
    public boolean insertOne(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        userMapper.insert(ConvertBeanUtils.convert(userDTO, UserPO.class));
        return true;
    }

    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Maps.newHashMap();
        }
        userIdList = userIdList.stream().filter(id -> id > 10000).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIdList)) {
            return Maps.newHashMap();
        }
        List<String> keyList = new ArrayList<>();
        userIdList.forEach(userId -> {
            keyList.add(userProviderCacheKeyBuilder.buildUserInfoKey(userId));
        });
        List<UserDTO> userDTOList = redisTemplate.opsForValue().multiGet(keyList).stream().filter(x -> x != null).toList();
        if (!CollectionUtils.isEmpty(userDTOList) && userDTOList.size() == userIdList.size()) {
            return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x -> x));
        }
        List<Long> userIdInCacheList = userDTOList.stream().map(UserDTO::getUserId).toList();
        List<Long> userIdNotInCacheList = userIdList.stream().filter(x -> !userIdInCacheList.contains(x)).toList();

        Map<Long, List<Long>> userIdMap = userIdList.stream().collect(Collectors.groupingBy(userId -> userId % 100));
        List<UserDTO> dbQueryResult = new CopyOnWriteArrayList<>();
        userIdMap.values().parallelStream().forEach(queryUserIdList -> {
            dbQueryResult.addAll(ConvertBeanUtils.convertList(userMapper.selectBatchIds(queryUserIdList), UserDTO.class));
        });
        if (!CollectionUtils.isEmpty(dbQueryResult)) {
            Map<String, UserDTO> saveCacheMap = dbQueryResult.stream().collect(Collectors.toMap(userDTO -> userProviderCacheKeyBuilder.buildUserInfoKey(userDTO.getUserId()), x -> x));
            redisTemplate.opsForValue().multiSet(saveCacheMap);
            userDTOList.addAll(dbQueryResult);
        }
        return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x -> x));
    }
}
