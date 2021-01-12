package com.seckill.dao;

import com.seckill.po.Orders;
import org.apache.ibatis.annotations.Insert;

public interface OrdersDao {


    @Insert("insert into orders(order_id,good_id,user_id) values (#{orderId},#{goodId},#{userId})")
    public int save(Orders orders);

}
