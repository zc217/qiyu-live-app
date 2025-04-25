package org.qiyu.live.user.utils;

public class TagInfoUtils {

    public static boolean isContain(Long tagInfo, Long matchTag) {
        return tagInfo != null && matchTag != null && matchTag > 0 && (tagInfo & matchTag) == matchTag;
    }


//    public static void main(String[] args) {
//        System.out.println(TagInfoUtils.isContain(3L, 2L));
//        System.out.println(TagInfoUtils.isContain(3L, 1L));
//        System.out.println(TagInfoUtils.isContain(3L, 4L));
//    }
}
