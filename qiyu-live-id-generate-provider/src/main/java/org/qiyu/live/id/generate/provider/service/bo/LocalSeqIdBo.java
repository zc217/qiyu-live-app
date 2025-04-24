package org.qiyu.live.id.generate.provider.service.bo;

import java.util.concurrent.atomic.AtomicLong;

public class LocalSeqIdBo {

    private int id;

    private AtomicLong currentNum;

    private Long currentStart;

    private Long nextThreshold;

    public Long getCurrentStart() {
        return currentStart;
    }

    public void setCurrentStart(Long currentStart) {
        this.currentStart = currentStart;
    }

    public Long getNextThreshold() {
        return nextThreshold;
    }

    public void setNextThreshold(Long nextThreshold) {
        this.nextThreshold = nextThreshold;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AtomicLong getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(AtomicLong currentNum) {
        this.currentNum = currentNum;
    }
}
