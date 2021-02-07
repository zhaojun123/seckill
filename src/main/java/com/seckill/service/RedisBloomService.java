package com.seckill.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisBloomService {

    @Autowired
    private RedisTemplate redisTemplate;

    private String key = "secKill:goods:bloom";

    public void init(){
        long begin = System.currentTimeMillis();
        String lua ="redis.call('del','"+key+"')\n"+
                "redis.call('bf.reserve','"+key+"',0.001,1000000)\n"+
                "for i=1,1000000 do\n" +
                " redis.call('BF.ADD','"+key+"',i)\n" +
                "end\n";
        redisTemplate.execute(new DefaultRedisScript(lua),null);
        System.err.println("bloom 初始化 耗时"+(System.currentTimeMillis()-begin)/1000);
    }

    /**
     * 判断goodId 是否合法
     * @param goodId
     * @return
     */
    @HystrixCommand(fallbackMethod = "mightContainFallBack",commandKey = "mightContain")
    public boolean mightContain(String goodId){
        String lua = "local result_1 = redis.call('BF.EXISTS', '"+key+"','"+goodId+"')\n" +
                "return result_1";
        return (Boolean)redisTemplate.execute(new DefaultRedisScript(lua,Boolean.class),null);
    }

    /**
     * 处理 mightContain的 熔断业务
     * @param goodId
     * @param e
     * @return
     */
    public boolean mightContainFallBack(String goodId,Throwable e){
        //e.printStackTrace();
        return false;
    }
}
