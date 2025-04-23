package org.qiyu.live.user.provider.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "qiyu.rmq.consumer")
@Configuration
public class RocketMQConsumerProperties {

    private String nameSer;

    private String groupName;

    @Override
    public String toString() {
        return "RocketMQConsumerProperties{" +
                "nameSer='" + nameSer + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }

    public String getNameSer() {
        return nameSer;
    }

    public void setNameSer(String nameSer) {
        this.nameSer = nameSer;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
