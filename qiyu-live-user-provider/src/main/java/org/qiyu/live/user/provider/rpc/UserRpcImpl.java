package org.qiyu.live.user.provider.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.qiyu.live.user.interfaces.IUserRpc;

@DubboService
public class UserRpcImpl implements IUserRpc {

    @Override
    public String test() {
        System.out.println("Dubbo test");
        return "success";
    }
}
