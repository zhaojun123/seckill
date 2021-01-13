package com.seckill.service;

import com.seckill.dao.GoodsDao;
import com.seckill.dao.OrdersDao;
import com.seckill.po.Goods;
import com.seckill.po.Orders;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 利用数据库实现秒杀
 */
@Service
public class DatabaseSecKillService implements SecKillService{


    @Autowired
    GoodsDao goodsDao;

    @Autowired
    OrderService orderService;

    @Autowired
    LogService logService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public Boolean secKill(Long userId, Long goodId) {
        //记录抢单日志
        logService.record(goodId,userId);
        //锁定这一行商品
        Goods goods = goodsDao.getForUpdate(goodId);
        if(goods == null){
            return false;
        }
        //如果库存为空 抢单失败
        if(goods.getStock()<=0){
            return false;
        }
        //TODO 这里模仿访问订单服务有2秒的延迟
        if(2 == goodId){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //开始下单
        orderService.order(userId,goodId);
        //减库存
        goodsDao.reduceStock(goodId);
        return true;
    }


}
