package org.qiyu.live.user.provider.service;

import org.qiyu.live.user.dto.UserDTO;

import java.util.List;
import java.util.Map;

public interface IUserService {
    
    UserDTO getByUserId(Long userId);
    
    boolean updateUserInfo(UserDTO userDTO);

    boolean insertOne(UserDTO userDTO);

    Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList);
}
