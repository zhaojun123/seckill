package com.seckill.service;

import com.seckill.dao.GoodsDao;
import com.seckill.dao.OrdersDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 利用数据库实现秒杀优化
 */
@Service
public class DatabaseSecKill2Service implements SecKillService{

    @Autowired
    GoodsDao goodsDao;

    @Autowired
    OrderService orderService;

    @Autowired
    LogService logService;

    @Override
    public Boolean secKill(Long userId, Long goodId) {
        logService.send(goodId,userId);
        int result = goodsDao.reduceStockIfAllow(goodId);
        if(result<1){
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
        //TODO 这里存在分布式事务问题，可以通过消息队列解决，也可以catch错误 重新添加库存
        orderService.order(userId,goodId);
        return true;
    }
}
