package org.qiyu.live.user.provider.service;

import org.qiyu.live.user.dto.UserDTO;

public interface IUserService {
    
    UserDTO getByUserId(Long userId);
    
    boolean updateUserInfo(UserDTO userDTO);

    boolean insertOne(UserDTO userDTO);
}
