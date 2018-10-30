package com.laozhao.springboot.common.aop;

import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;

@Component
@Order(0)
@Aspect
public class ControllerAopLog {
 
	@Pointcut("execution(public * de.codecentric.boot.admin.server.web.ApplicationsController..*(..))")
	public void pointCut() {}
	
	@Pointcut("execution(public * de.codecentric.boot.admin.server.web.InstancesController..*(..))")
	public void pointCut2() {}
	
	@AfterReturning(pointcut="pointCut()",returning="rvt")
	public void returnInfo(JoinPoint jp,Object rvt) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		String url=request.getRequestURI();
		//if(url.indexOf(".")!=-1)return;
		System.out.println(request.getRequestURI()+"-->"+JSONObject.toJSONString(rvt));
	}
	
	@AfterReturning(pointcut="pointCut2()",returning="rvt")
	public void returnInfo2(JoinPoint jp,Object rvt) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		
		System.out.println(request.getRequestURI()+"-->"+JSONObject.toJSONString(rvt));
	}
}
