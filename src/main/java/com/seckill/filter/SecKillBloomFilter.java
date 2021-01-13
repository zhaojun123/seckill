package com.seckill.filter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.openjdk.jol.info.ClassLayout;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

/**
 * 布隆过滤器 过滤非法的秒杀请求
 */
public class SecKillBloomFilter implements Filter {

    private byte[] falseBytes = "false".getBytes();

    private BloomFilter bloomFilter;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String goodId = request.getParameter("goodId");
        //请求的商品id不存在，则是非法请求
        if(goodId == null || !bloomFilter.mightContain(goodId)){
            response.getOutputStream().write(falseBytes);
            return;
        }
        chain.doFilter(request,response);
    }

    /**
     * 初始化布隆过滤器，为了精确度和最优的内存占比 需要在初始化的时候预估数据量
     * 这里预计有100万的商品
     */
    @Override
    public  void init(FilterConfig filterConfig){
        //100万数据量 0.1%的错误率
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()),1000000,0.001);
        for(int i=1;i<=1000000;i++){
            bloomFilter.put(String.valueOf(i));
        }
    }
}
