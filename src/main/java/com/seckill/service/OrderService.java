package com.seckill.service;

import com.seckill.dao.OrdersDao;
import com.seckill.po.Orders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    OrdersDao ordersDao;

    public void order(Long userId,Long goodId){
        Orders orders = new Orders();
        orders.setGoodId(goodId);
        orders.setUserId(userId);
        ordersDao.save(orders);
    }

}
