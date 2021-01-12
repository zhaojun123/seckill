package com.seckill.service;

/**
 * 秒杀服务接口
 */
public interface SecKillService {

    public Boolean secKill(Long userId,Long goodId);
}
