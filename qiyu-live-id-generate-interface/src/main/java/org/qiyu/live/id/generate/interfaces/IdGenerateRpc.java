package org.qiyu.live.id.generate.interfaces;

public interface IdGenerateRpc {

    Long getSeqId(Integer id);

    Long getUnSeqId(Integer id);
}
