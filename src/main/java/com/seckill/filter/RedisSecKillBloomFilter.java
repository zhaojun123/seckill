package com.seckill.filter;

import com.seckill.service.RedisBloomService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import java.io.IOException;

public class RedisSecKillBloomFilter implements Filter {

    @Autowired
    RedisBloomService redisBloomService;

    private byte[] falseBytes = "false".getBytes();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String goodId = request.getParameter("goodId");
        //请求的商品id不存在，则是非法请求
        if(goodId == null || !redisBloomService.mightContain(goodId)){
            response.getOutputStream().write(falseBytes);
            return;
        }
        chain.doFilter(request,response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        redisBloomService.init();
    }

}
