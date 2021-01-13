package com.seckill.service;


import com.seckill.dao.GoodsDao;
import com.seckill.po.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 利用jvm内存实现秒杀
 */
@Service
public class RedisSecKillService implements SecKillService{

    @Resource
    private RedisTemplate redisTemplate;

    @Autowired
    GoodsDao goodsDao;

    @Autowired
    OrderService orderService;

    @Autowired
    LogService logService;

    private final String secKillGoodsPrefix = "secKill:goods:id:";

    private final String secKillLockPrefix = "secKill:lock:goods:id:";

    //设定缓存60秒过期
    private long timeOut = 60;

    //当该值为0时表明库存已抢光，后续请求不再请求redis
    private volatile int remainStockNum = -1;

    /**
     * 初始化热门商品信息
     */
    @PostConstruct
    private void init(){
        List<Goods> goodsList = goodsDao.hostList();
        for(Goods goods:goodsList){
            //加入缓存 设置60秒过期
            redisTemplate.opsForValue().set(secKillGoodsPrefix+goods.getGoodId(),String.valueOf(goods.getStock()),timeOut, TimeUnit.SECONDS);
        }
    }

    @Override
    public Boolean secKill(Long userId, Long goodId) {
        //记录抢单日志
        logService.send(goodId,userId);
        if(remainStockNum == 0){
            return false;
        }
        Long stockNum = getStockNum(goodId);
        //如果stockNum为null 说明缓存没有找到，执行锁并且初始化数据
        if(stockNum == null){
            String uuid = UUID.randomUUID().toString();
            //加锁 为了防止死锁10秒过期
            if(lock(goodId,uuid,10L)){
                try{
                    //初始化数据
                    Goods goods = goodsDao.get(goodId);
                    //加入缓存 设置60秒过期,极端情况下为了防止锁已经超时，这里用setIfAbsent
                    redisTemplate.opsForValue().setIfAbsent(secKillGoodsPrefix+goods.getGoodId(),String.valueOf(goods.getStock()),timeOut, TimeUnit.SECONDS);
                }finally {
                    release(goodId,uuid);
                }
            }
            //这里业务应该更复杂 需要等待初始化， 但是为了性能可以有一些业务瑕疵，所以抢购不是越快越好，运气很重要
            return false;
        }
        //库存不足
        if(stockNum<0){
            //后续请求不再访问redis
            remainStockNum = 0;
            return false;
        }
        //下单 减库存
        //减库存
        goodsDao.reduceStock(goodId);
        //下订单
        orderService.order(userId,goodId);
        return true;
    }

    /**
     * 通过redis获取库存
     * @param goodId
     * @return
     */
    //TODO这里可以加上断路器，如果熔断，将remainStockNum设置成0
    private Long getStockNum(Long goodId){
        //通过lua 判断key是否存在，如果不存在返回 null 存在则调用decrement  并返回结果
        String lua = "if redis.call('EXISTS', '"+secKillGoodsPrefix+goodId+"') == 1 then \n" +
                " redis.call('EXPIRE','"+secKillGoodsPrefix+goodId+"',"+timeOut+")\n"+
                " return redis.call('DECRBY', '"+secKillGoodsPrefix+goodId+"',1)\n "+
                " else \n"+
                " return nil \n" +
                "end";
        Long stockNum = (Long)redisTemplate.execute(new DefaultRedisScript(lua,Long.class),null);
        return stockNum;
    }

    /**
     * 利用redis获取分布式锁
     * @param goodId
     * @param requestId 唯一请求id，解锁的时候根据这个判断是否是自己的锁
     * @return
     */
    private boolean lock(Long goodId,String requestId,Long expire){
        return redisTemplate.opsForValue().setIfAbsent(secKillLockPrefix+goodId,requestId,expire,TimeUnit.SECONDS);
    }

    /**
     * 解锁
     * @param goodId
     * @param requestId
     * @return
     */
    private boolean release(Long goodId,String requestId){
        String lua = "if redis.call('get', '"+secKillLockPrefix+goodId+"') == '"+requestId+"' then return redis.call('del', '"+secKillLockPrefix+goodId+"') else return 0 end";
        Long result = (Long)redisTemplate.execute(new DefaultRedisScript(lua,Long.class),null);
        return 1 == result;
    }
}
