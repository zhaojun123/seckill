package com.seckill.service;

import com.seckill.dao.LogsDao;
import com.seckill.po.Logs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class LogService {

    @Autowired
    LogsDao logsDao;

    private LogHandle logHandle = new LogHandle(500);

    @PostConstruct
    private void init(){
        Thread logThread = new Thread(logHandle);
        logThread.start();

        //如果容器停止，则中断日志处理线程
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logThread.interrupt();
        }));
    }

    public void send(Long goodId,Long userId){
        Logs logs = new Logs();
        logs.setGoodId(goodId);
        logs.setUserId(userId);
        logHandle.logsQueue.offer(logs);
    }

    public void record(Long goodId,Long userId){
        Logs logs = new Logs();
        logs.setGoodId(goodId);
        logs.setUserId(userId);
        logsDao.save(logs);
    }

    private class LogHandle implements Runnable{

        //每100条插入一次数据库
        private  int batchSize = 100;
        private  List<Logs> logsList;
        private  BlockingQueue<Logs> logsQueue;

        public LogHandle(int batchSize){
            this.batchSize = batchSize;
            logsList = new ArrayList<Logs>(batchSize);
            logsQueue = new LinkedBlockingQueue<>();
        }

        private void batch(){
            if(!logsList.isEmpty()){
                logsDao.batchSave(logsList);
                logsList = new ArrayList<>(batchSize);
            }
        }

        @Override
        public void run() {
            //从阻塞队列里面拿取日志，批量插入数据库
            while(!Thread.currentThread().isInterrupted()){
                try {
                    Logs log = logsQueue.poll(5, TimeUnit.SECONDS);
                    //如果取不到数据 或者 数据量达到可以批处理的地步就执行批处理
                    if(log==null || logsList.size()>=batchSize){
                        batch();
                    }
                    if(log!=null){
                        logsList.add(log);
                    }
                } catch (InterruptedException e) {

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            System.out.println("LogHandle Thread stoped");
        }
    }

}
