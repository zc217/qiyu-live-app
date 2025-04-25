package org.qiyu.live.user.provider;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.qiyu.live.user.constants.UserTagsEnum;
import org.qiyu.live.user.dto.UserDTO;
import org.qiyu.live.user.provider.service.IUserService;
import org.qiyu.live.user.provider.service.IUserTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.concurrent.CountDownLatch;

/**
 * 用户中台服务提供者
 */
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class UserProviderApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(UserProviderApplication.class);

    @Resource
    private IUserTagService userTagService;

    @Resource
    private IUserService userService;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(UserProviderApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Long userId = 1004L;
        UserDTO userDTO = userService.getByUserId(userId);
        userDTO.setNickName("test-nick_name");
//        userService.updateUserInfo(userDTO);

        System.out.println(userTagService.containTag(userId,UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.setTag(userId,UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.containTag(userId,UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.cancelTag(userId,UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.containTag(userId,UserTagsEnum.IS_OLD_USER));


//        Long userId = 1004L;
//        CountDownLatch count = new CountDownLatch(1);
//        for (int i = 0; i < 100; i++) {
//            Thread t1 = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        count.await();
//                        logger.info("result is " + userTagService.setTag(userId, UserTagsEnum.IS_VIP));
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//            t1.start();
//        }
//        count.countDown();
//        Thread.sleep(100000);


//        long userId = 1001L;
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_RICH));
//        System.out.println("当前用户是否拥有 is_rich 标签：" + userTagService.containTag(userId, UserTagsEnum.IS_RICH));
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println("当前用户是否拥有 is_vip 标签：" + userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_OLD_USER));
//        System.out.println("当前用户是否拥有 is_old_user 标签：" + userTagService.containTag(userId, UserTagsEnum.IS_OLD_USER));
    }
}
