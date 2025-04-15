package org.qiyu.live.user.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.qiyu.live.user.provider.dao.po.UserPO;

/**
 * @author zhou
 */
@Mapper
public interface IUserMapper extends BaseMapper<UserPO> {
}
