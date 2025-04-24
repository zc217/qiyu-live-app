package org.qiyu.live.id.generate.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.id.generate.provider.dao.mapper.IdGenerateMapper;
import org.qiyu.live.id.generate.provider.dao.po.IdGeneratePO;
import org.qiyu.live.id.generate.provider.service.IdGenerateService;
import org.qiyu.live.id.generate.provider.service.bo.LocalSeqIdBo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class IdGenerateServiceImpl implements IdGenerateService, InitializingBean {

    @Resource
    private IdGenerateMapper idGenerateMapper;

    private static final Logger logger = LoggerFactory.getLogger(IdGenerateServiceImpl.class);

    private static Map<Integer, LocalSeqIdBo> localSeqIdBoMap = new ConcurrentHashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 16, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("id-generate-thread-" + ThreadLocalRandom.current().nextInt(1000));
                    return thread;
                }
            });
    private static final float UPDATE_RATE = 0.75f;

    private static Map<Integer, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    @Override
    public Long getSeqId(Integer id) {
        if (id == null) {
            logger.error("[getSeqId] id is null,id is {}", id);
            return null;
        }
        LocalSeqIdBo localSeqIdBo = localSeqIdBoMap.get(id);
        if (localSeqIdBo == null) {
            logger.error("[getSeqId] localSeqIdBo is null,id is {}", id);
            return null;
        }
        this.refreshLocalSeqId(localSeqIdBo);
        if (localSeqIdBo.getCurrentNum().get() > localSeqIdBo.getNextThreshold()){
            logger.error("[getSeqId] id is over limit,id is {}", id);
            return null;
        }
        long returnId = localSeqIdBo.getCurrentNum().getAndIncrement();
        return returnId;
    }

    private void refreshLocalSeqId(LocalSeqIdBo localSeqIdBo) {
        long step = localSeqIdBo.getNextThreshold() - localSeqIdBo.getCurrentStart();
        if (localSeqIdBo.getCurrentNum().get() - localSeqIdBo.getCurrentStart() > step * UPDATE_RATE) {
            Semaphore semaphore = semaphoreMap.get(localSeqIdBo.getId());
            if (semaphore == null) {
                logger.error("semaphore is null,id is {}", localSeqIdBo.getId());
                return;
            }
            boolean acquireStatus = semaphore.tryAcquire();
            if (acquireStatus) {
                logger.info("尝试开始进行本地id段的同步操作");
                //异步进行同步id段操作
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IdGeneratePO idGeneratePO = idGenerateMapper.selectById(localSeqIdBo.getId());
                            tryUpdateMySQLRecord(idGeneratePO);
                        } catch (Exception e) {
                            logger.error("[refreshLocalSeqId] error is ", e);
                        } finally {
                            semaphoreMap.get(localSeqIdBo.getId()).release();
                            logger.info("本地有序id段同步完成,id is {}", localSeqIdBo.getId());
                        }
                    }
                });
            }
        }
    }

    @Override
    public Long getUnSeqId(Integer id) {
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<IdGeneratePO> idGeneratePOList = idGenerateMapper.selectAll();
        for (IdGeneratePO idGeneratePO : idGeneratePOList) {
            tryUpdateMySQLRecord(idGeneratePO);
            semaphoreMap.put(idGeneratePO.getId(), new Semaphore(1));
        }
    }

    private void tryUpdateMySQLRecord(IdGeneratePO idGeneratePO) {
        long currentStart = idGeneratePO.getCurrentStart();
        long nextThreshold = idGeneratePO.getNextThreshold();
        long currentNum = currentStart;
        int updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
        if (updateResult > 0) {
            LocalSeqIdBo localSeqIdBo = new LocalSeqIdBo();
            AtomicLong atomicLong = new AtomicLong(currentNum);
            localSeqIdBo.setId(idGeneratePO.getId());
            localSeqIdBo.setCurrentNum(atomicLong);
            localSeqIdBo.setCurrentStart(currentNum);
            localSeqIdBo.setNextThreshold(nextThreshold);
            localSeqIdBoMap.put(localSeqIdBo.getId(), localSeqIdBo);
            return;
        }

        for (int i = 0; i < 3; i++) {
            idGeneratePO = idGenerateMapper.selectById(idGeneratePO.getId());
            updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
            if (updateResult > 0) {
                LocalSeqIdBo localSeqIdBo = new LocalSeqIdBo();
                AtomicLong atomicLong = new AtomicLong(idGeneratePO.getCurrentStart());
                localSeqIdBo.setId(idGeneratePO.getId());
                localSeqIdBo.setCurrentNum(atomicLong);
                localSeqIdBo.setCurrentStart(idGeneratePO.getCurrentStart());
                localSeqIdBo.setNextThreshold(idGeneratePO.getNextThreshold());
                localSeqIdBoMap.put(localSeqIdBo.getId(), localSeqIdBo);
                return;
            }
        }
        throw new RuntimeException("表id段占用失败，竞争过于激烈，id is " + idGeneratePO.getId());
    }
}
