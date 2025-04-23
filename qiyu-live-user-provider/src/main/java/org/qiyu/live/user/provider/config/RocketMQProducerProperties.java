package org.qiyu.live.user.provider.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "qiyu.rmq.producer")
@Configuration
public class RocketMQProducerProperties {

    private String nameSer;

    private String groupName;

    private int retryTimes;

    private int sendTimeOut;

    @Override
    public String toString() {
        return "RocketMQProducerProperties{" +
                "nameSer='" + nameSer + '\'' +
                ", groupName='" + groupName + '\'' +
                ", retryTimes=" + retryTimes +
                ", sendTimeOut=" + sendTimeOut +
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

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getSendTimeOut() {
        return sendTimeOut;
    }

    public void setSendTimeOut(int sendTimeOut) {
        this.sendTimeOut = sendTimeOut;
    }
}
