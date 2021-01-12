package com.seckill.service;


import com.seckill.dao.GoodsDao;
import com.seckill.po.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 利用jvm内存实现秒杀
 */
@Service
public class RamSecKillService implements SecKillService{

    @Autowired
    GoodsDao goodsDao;

    @Autowired
    OrderService orderService;

    @Autowired
    LogService logService;
    /**
     * 商品信息
     */
    private final ConcurrentHashMap<Long, GoodsInfo> goodsMap = new ConcurrentHashMap();



    /**
     * 初始化热门商品信息
     */
    @PostConstruct
    private void init(){
        List<Goods> goodsList = goodsDao.hostList();
        for(Goods goods:goodsList){
            GoodsInfo goodsInfo = new GoodsInfo();
            goodsInfo.goodsId = goods.getGoodId();
            goodsInfo.stock = new AtomicInteger(goods.getStock());
            goodsInfo.initialized = true;
            goodsMap.put(goodsInfo.goodsId,goodsInfo);
        }
    }

    /**
     * 在高并发下，该段代码会访问多次数据库
     * @param goodId
     * @return
     */
    /*private GoodsInfo getGoodsInfo(String goodId){
        GoodsInfo goodsInfo = goodsMap.get(goodId);
        //缓存内没有，则从数据库来加载
        if(goodsInfo == null){
            Goods goods = goodsDao.get(goodId);
            //如果数据库没有数据说明是非法请求
            if(goods == null){
                return null;
            }
            goodsInfo = new GoodsInfo();
            goodsInfo.goodsId = goods.getGoodId();
            goodsInfo.atomicInteger = new AtomicInteger(goods.getStock());
            GoodsInfo old = goodsMap.putIfAbsent(goodsInfo.goodsId,goodsInfo);
            if(old != null){
                goodsInfo = old;
            }
        }
        return goodsInfo;
    }*/

    /**
     * 获取缓存对象GoodsInfo
     * 优化数据库查询问题，该方法只会有一个线程查询数据库
     * @param goodId
     * @return
     */
    private GoodsInfo getGoodsInfo(Long goodId){
        GoodsInfo goodsInfo = goodsMap.get(goodId);
        //缓存内没有，
        if(goodsInfo == null){
            goodsInfo = new GoodsInfo();
            GoodsInfo old = goodsMap.putIfAbsent(goodId,goodsInfo);
            //当old为空，则表明该线程是第一个初始化GoodsInfo的线程
            if(old == null){
                //这个时候开始查询数据库
                synchronized (goodsInfo){
                    Goods goods = goodsDao.get(goodId);
                    //如果商品不存在，设置为无效
                    if(goods == null){
                        old.effective = false;
                    }else{
                        old.goodsId = goods.getGoodId();
                        old.stock = new AtomicInteger(goods.getStock());
                    }
                    //设置初始化完成
                    old.initialized = true;
                    //唤醒其他阻塞的线程
                    goodsInfo.notifyAll();
                }
            }else{
                goodsInfo = old;
            }
        }
        //说明别的线程已经开始初始化GoodsInfo，此时做等待
        if(!goodsInfo.initialized){
            synchronized(goodsInfo){
                while(!goodsInfo.initialized){
                    try {
                        goodsInfo.wait();
                    } catch (InterruptedException e) {

                    }
                }
            }
        }
        //开始校验 goodsInfo是否合法
        if(!goodsInfo.effective){
            return null;
        }
        return goodsInfo;
    }


    @Override
    public Boolean secKill(Long userId, Long goodId) {
        GoodsInfo goodsInfo = getGoodsInfo(goodId);
        if(goodsInfo == null){
            return false;
        }
        logService.send(goodId,userId);
        //开始检查库存
        AtomicInteger stock = goodsInfo.stock;
        if(stock.get()<=0){
            return false;
        }
        //减库存成功
        if(stock.decrementAndGet()>=0){
            //减库存
            goodsDao.reduceStock(goodId);
            //下订单
            orderService.order(userId,goodId);
            return true;
        }
        return false;
    }

    /**
     * 这里需要注意 因为是多线程环境需要保证可见性 和 禁止指令重排，这里的属性都要加volatile
     * TODO  如果不加volatile 怎么处理
     */
    private class GoodsInfo{

        //是否已经初始化
        private volatile boolean initialized = false;

        private volatile Long goodsId;

        private volatile AtomicInteger stock;

        //是否是有效的，如果数据库找不到说明无效
        private volatile boolean effective = true;
    }



}
