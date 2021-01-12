package com.seckill.dao;


import com.seckill.po.Logs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LogsDao {

    @Insert("insert into logs(log_id,good_id,user_id,create_date) values (#{logId},#{goodId},#{userId},now())")
    public int save(Logs logs);

    @Insert( "<script>"  +
            "insert into logs (log_id,good_id,user_id,create_date)  VALUES " +
                "<foreach collection='list' item='item' index='index' separator=','> " +
                    "(#{item.logId},#{item.goodId},#{item.userId},now()) " +
                "</foreach>" +
            "</script>")
    public int batchSave(@Param("list") List<Logs> logsList);

}
