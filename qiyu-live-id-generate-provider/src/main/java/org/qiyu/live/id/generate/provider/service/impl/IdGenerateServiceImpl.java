package org.qiyu.live.id.generate.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.id.generate.provider.dao.mapper.IdGenerateMapper;
import org.qiyu.live.id.generate.provider.dao.po.IdGeneratePO;
import org.qiyu.live.id.generate.provider.service.IdGenerateService;
import org.qiyu.live.id.generate.provider.service.bo.LocalSeqIdBo;
import org.qiyu.live.id.generate.provider.service.bo.LocalUnSeqIdBo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class IdGenerateServiceImpl implements IdGenerateService, InitializingBean {

    @Resource
    private IdGenerateMapper idGenerateMapper;

    private static final Logger logger = LoggerFactory.getLogger(IdGenerateServiceImpl.class);

    private static Map<Integer, LocalSeqIdBo> localSeqIdBoMap = new ConcurrentHashMap<>();

    private static Map<Integer, LocalUnSeqIdBo> localUnSeqIdBoMap = new ConcurrentHashMap<>();

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

    private static final int SEQ_ID = 1;

    private static Map<Integer, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    @Override
    public Long getUnSeqId(Integer id) {
        if (id == null) {
            logger.error("[getUnSeqId] id is null,id is {}", id);
            return null;
        }
        LocalUnSeqIdBo localUnSeqIdBo = localUnSeqIdBoMap.get(id);
        if (localUnSeqIdBo == null) {
            logger.error("[getUnSeqId] localUnSeqIdBo is null,id is {}", id);
            return null;
        }
        Long returnId = localUnSeqIdBo.getIdQueue().poll();
        if (returnId == null) {
            logger.error("[getUnSeqId] returnId is null,id is {}", id);
            return null;
        }
        this.refreshLocalUnSeqId(localUnSeqIdBo);
        return returnId;
    }


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
        long returnId = localSeqIdBo.getCurrentNum().incrementAndGet();
        if (returnId > localSeqIdBo.getNextThreshold()) {
            logger.error("[getSeqId] id is over limit,id is {}", id);
            return null;
        }
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

    private void refreshLocalUnSeqId(LocalUnSeqIdBo localUnSeqIdBo) {
        long begin = localUnSeqIdBo.getCurrentStart();
        long end = localUnSeqIdBo.getNextThreshold();
        long remainSize = localUnSeqIdBo.getIdQueue().size();
        if ((end - begin) * 0.25 > remainSize) {
            Semaphore semaphore = semaphoreMap.get(localUnSeqIdBo.getId());
            if (semaphore == null) {
                logger.error("semaphore is null,id is {}", localUnSeqIdBo.getId());
                return;
            }
            boolean acquireStatus = semaphore.tryAcquire();
            if (acquireStatus) {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IdGeneratePO idGeneratePO = idGenerateMapper.selectById(localUnSeqIdBo.getId());
                            tryUpdateMySQLRecord(idGeneratePO);
                        } catch (Exception e) {
                            logger.error("[refreshLocalUnSeqId] error is ", e);
                        } finally {
                            semaphoreMap.get(localUnSeqIdBo.getId()).release();
                            logger.info("本地无序id段同步完成,id is {}", localUnSeqIdBo.getId());
                        }
                    }
                });
            }
        }
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
        int updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
        if (updateResult > 0) {
            localIdBoHandler(idGeneratePO);
            return;
        }
        for (int i = 0; i < 3; i++) {
            idGeneratePO = idGenerateMapper.selectById(idGeneratePO.getId());
            updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
            if (updateResult > 0) {
                localIdBoHandler(idGeneratePO);
                return;
            }
        }
        throw new RuntimeException("表id段占用失败，竞争过于激烈，id is " + idGeneratePO.getId());
    }

    private void localIdBoHandler(IdGeneratePO idGeneratePO) {
        long currentStart = idGeneratePO.getCurrentStart();
        long nextThreshold = idGeneratePO.getNextThreshold();
        long currentNum = currentStart;
        if (idGeneratePO.getIsSeq() == SEQ_ID) {
            LocalSeqIdBo localSeqIdBo = new LocalSeqIdBo();
            AtomicLong atomicLong = new AtomicLong(currentNum);
            localSeqIdBo.setId(idGeneratePO.getId());
            localSeqIdBo.setCurrentNum(atomicLong);
            localSeqIdBo.setCurrentStart(currentNum);
            localSeqIdBo.setNextThreshold(nextThreshold);
            localSeqIdBoMap.put(localSeqIdBo.getId(), localSeqIdBo);
        } else {
            LocalUnSeqIdBo localUnSeqIdBo = new LocalUnSeqIdBo();
            localUnSeqIdBo.setCurrentStart(currentStart);
            localUnSeqIdBo.setNextThreshold(nextThreshold);
            localUnSeqIdBo.setId(idGeneratePO.getId());
            Long begin = localUnSeqIdBo.getCurrentStart();
            Long end = localUnSeqIdBo.getNextThreshold();
            List<Long> idList = new ArrayList<>();
            for (long i = begin; i < end; i++) {
                idList.add(i);
            }
            Collections.shuffle(idList);
            ConcurrentLinkedQueue<Long> idQueue = new ConcurrentLinkedQueue<>();
            idQueue.addAll(idList);
            localUnSeqIdBo.setIdQueue(idQueue);
            localUnSeqIdBoMap.put(localUnSeqIdBo.getId(), localUnSeqIdBo);
        }
    }
}
