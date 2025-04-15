package org.qiyu.live.user.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.qiyu.live.user.dto.UserDTO;
import org.qiyu.live.user.interfaces.IUserRpc;
import org.qiyu.live.user.provider.dao.mapper.IUserMapper;
import org.qiyu.live.user.provider.service.IUserService;

@DubboService
public class UserRpcImpl implements IUserRpc {
    
    @Resource
    private IUserService userService;

    @Override
    public UserDTO getByUserId(Long userId) {
        return userService.getByUserId(userId);
    }

    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        return userService.updateUserInfo(userDTO);
    }

    @Override
    public boolean insertOne(UserDTO userDTO) {
        return userService.insertOne(userDTO);
    }

    @Override
    public String test() {
        System.out.println("Dubbo test");
        return "success";
    }
}
