package org.qiyu.live.user.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.common.interfaces.ConvertBeanUtils;
import org.qiyu.live.user.dto.UserDTO;
import org.qiyu.live.user.provider.dao.mapper.IUserMapper;
import org.qiyu.live.user.provider.dao.po.UserPO;
import org.qiyu.live.user.provider.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService {
    
    @Resource
    private IUserMapper userMapper;
    
    @Override
    public UserDTO getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
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
}
