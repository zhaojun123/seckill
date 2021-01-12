package com.seckill.dao;

import com.seckill.po.Goods;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface GoodsDao {

    @Select("select * from goods where good_id = #{VALUE} for update")
    public Goods getForUpdate(Long goodsId);

    @Update("update goods set stock = stock -1 where good_id = #{VALUE}")
    public int reduceStock(Long goodsId);

    @Update("update goods set stock = stock -1 where good_id = #{VALUE} and stock>0")
    public int reduceStockIfAllow(Long goodsId);

    @Select("select * from goods where good_id = 1")
    public List<Goods> hostList();

    @Select("select * from goods where good_id = #{VALUE}")
    public Goods get(Long goodsId);
}
