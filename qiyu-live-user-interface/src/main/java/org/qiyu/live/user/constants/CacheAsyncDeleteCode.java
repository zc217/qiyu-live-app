package org.qiyu.live.user.constants;

public enum CacheAsyncDeleteCode {
    USER_INFO_DELETE(0,"用户基础信息删除"),
    USER_TAG_DELETE(1,"用户标签删除");

    int code;
    String desc;

    CacheAsyncDeleteCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
