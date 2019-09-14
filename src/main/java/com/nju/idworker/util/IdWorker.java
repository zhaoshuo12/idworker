package com.nju.idworker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by XXT on 2019/9/14.
 */
public class IdWorker {

    private static final Logger logger = LoggerFactory.getLogger(IdWorker.class);
    /**
     * 起始的时间戳
     */
    private final static long START_STMP = 1480166465631L;

    /**
     * 每一部分占用的位数
     */
    private final static long SEQUENCE_BIT = 12; //序列号占用的位数
    private final static long MACHINE_BIT = 5;  //机器标识占用的位数
    private final static long DATACENTER_BIT = 5;//数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移位数
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    @Value("${IdWorker.datacenterId}")
    private long datacenterId;  //数据中心
    @Value("${IdWorker.machineId}")
    private long machineId;    //机器标识
    private long sequence = 0L; //序列号
    private long lastStmp = -1L;//上一次时间戳

    public IdWorker () {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
    }

    public IdWorker (long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 生成下一个唯一ID
     * @return
     */
    public long nextId(){
        return nextId(getNewstmp());
    }

    /**
     * 生成指定时间的下一个Id
     * @param currStmp
     * @return
     */
    private synchronized long nextId(long currStmp){
        if(currStmp < lastStmp){
            //发生时钟回退
            logger.warn("发生时钟回退：当前时间" + currStmp + "上一时间" + lastStmp);
            currStmp = lastStmp;
        }

        if(currStmp == lastStmp){
            //相同毫秒内，序列号自增
            sequence = ++sequence & MAX_SEQUENCE;
            if (sequence == 0) {
                //同一毫秒生成的序列数已满
                logger.warn(currStmp + "时间同一毫秒生成的序列数已满，重新获取时间戳！");
                return nextId(getNewstmp());
            }
        }else{
            //更新上一时间
            lastStmp = currStmp;
            //重置序列号
            sequence = 0;
        }

        return(currStmp - START_STMP) << TIMESTMP_LEFT //时间戳部分
                | datacenterId << DATACENTER_LEFT      //数据中心部分
                | machineId << MACHINE_LEFT            //机器标识部分
                | sequence;                            //序列号部分
    }

    private long getNewstmp(){
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        IdWorker idWorker = new IdWorker(2, 3);
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            System.out.println(idWorker.nextId());
        }
        System.out.println("生成10000个全局唯一Id耗时" + (System.nanoTime() - startTime) / 1000000 + "ms");

    }
}
