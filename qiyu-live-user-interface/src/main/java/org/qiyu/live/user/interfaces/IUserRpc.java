package org.qiyu.live.user.interfaces;

import org.qiyu.live.user.dto.UserDTO;

public interface IUserRpc {

    UserDTO getByUserId(Long userId);
    
    boolean updateUserInfo(UserDTO userDTO);
    
    boolean insertOne(UserDTO userDTO);

    String test();
}
