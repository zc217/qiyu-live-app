package org.qiyu.live.api.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.user.dto.UserDTO;
import org.qiyu.live.user.interfaces.IUserRpc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @DubboReference
    private IUserRpc userRpc;
    
    @GetMapping("/getUserInfo")
    public UserDTO getUserInfo(Long userId) {
        return userRpc.getByUserId(userId);
    }

    @GetMapping("/updateUserInfo")
    public boolean updateUserInfo(Long userId,String nickName) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName(nickName);
        return userRpc.updateUserInfo(userDTO);
    }

    @GetMapping("/insertOne")
    public boolean insertOne(Long userId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName("test-user");
        userDTO.setSex(1);
        return userRpc.insertOne(userDTO);
    }

    @GetMapping("/dubbo")
    public String dubbo(){
        return userRpc.test();
    }
}
