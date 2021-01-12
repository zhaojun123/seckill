package com.seckill.controller;

import com.seckill.service.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secKill")
public class SecKillController implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    DatabaseSecKillService databaseSecKillService;

    @Autowired
    DatabaseSecKill2Service databaseSecKill2Service;

    @Autowired
    RamSecKillService ramSecKillService;

    @Autowired
    RedisSecKillService redisSecKillService;

    /**
     * 通过数据库解决秒杀
     * @param userId
     * @param goodId
     * @return
     */
    @RequestMapping("/databaseSecKill")
    public String databaseSecKill(Long userId,Long goodId){
        Boolean result = databaseSecKillService.secKill(userId,goodId);
        return result.toString();
    }


    /**
     * 通过数据库解决秒杀(优化)
     * @param userId
     * @param goodId
     * @return
     */
    @RequestMapping("/databaseSecKill2")
    public String databaseSecKill2(Long userId,Long goodId){
        Boolean result = databaseSecKill2Service.secKill(userId,goodId);
        return result.toString();
    }

    /**
     * 通过本地缓存实现秒杀
     * @param userId
     * @param goodId
     * @return
     */
    @RequestMapping("/ramSecKill")
    public String ramSecKill(Long userId,Long goodId){
        Boolean result = ramSecKillService.secKill(userId,goodId);
        return result.toString();
    }

    /**
     * 基于redis缓存实现秒杀
     * @param userId
     * @param goodId
     * @return
     */
    @RequestMapping("/redisSecKill")
    public String redisSecKillService(Long userId,Long goodId){
        Boolean result = redisSecKillService.secKill(userId,goodId);
        return result.toString();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
