package com.laozhao.springboot.common;

/**
 * Created by viruser on 2018/10/25.
 */

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Order(0)
@Component
@WebFilter(filterName = "requestFilter",urlPatterns = "/*")
public class RequestFilter  implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)servletRequest;
        System.out.println("---------------------"+req.getRequestURL().toString());
        Map<String,String[]>  param=req.getParameterMap();
        if(param!=null&&param.size()>0)
        for(Map.Entry<String,String[]> itm:param.entrySet()){
            String name=itm.getKey();
            String[] value=itm.getValue();
            System.out.println(name+":"+value[0]);
        }
        filterChain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy() {

    }
}
