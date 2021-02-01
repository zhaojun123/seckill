package com.seckill.filter;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.servlet.*;
import java.io.IOException;

public class RedisSecKillBloomFilter implements Filter {

    @Autowired
    private RedisTemplate redisTemplate;

    private byte[] falseBytes = "false".getBytes();

    private String key = "secKill:goods:bloom";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String goodId = request.getParameter("goodId");
        //请求的商品id不存在，则是非法请求
        if(goodId == null || !mightContain(goodId)){
            response.getOutputStream().write(falseBytes);
            return;
        }
        chain.doFilter(request,response);
    }

    /**
     * 预先插入 1到100万的goodsId 作为合法值
     * @param filterConfig
     */
    @Override
    public  void init(FilterConfig filterConfig){
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
    @HystrixCommand(fallbackMethod = "mightContainFallBack",groupKey = "accessRedis",commandKey = "mightContain")
    private boolean mightContain(String goodId){
        String lua = "local result_1 = redis.call('BF.EXISTS', '"+key+"','"+goodId+"')\n" +
                "return result_1";
        return (Boolean)redisTemplate.execute(new DefaultRedisScript(lua,Boolean.class),null);
    }

    /**
     * {@link RedisSecKillBloomFilter#mightContain(java.lang.String)} 熔断后的处理
     * @param goodId
     * @return
     */
    private boolean mightContainFallBack(String goodId){
        return false;
    }
}
