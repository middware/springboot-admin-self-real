package com.laozhao.springboot;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;

/**
 * Created by viruser on 2018/8/13.
 */
@Configuration
public class ActuatorSecurity extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().
        authorizeRequests().antMatchers("**").hasRole("USER")
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/loginPage").permitAll() //登录的路径
                //指定自定义form表单请求的路径
                .loginProcessingUrl("/authentication/form")    
                //.successForwardUrl("/")
                //.successHandler(new ForwardAuthenticationSuccessHandler("/?status=true"))
                .and()
                .logout()
                .logoutUrl("/logout").permitAll()
                .logoutSuccessUrl("/?logout=true")
                .and()
                .authorizeRequests().antMatchers("/assets/**").permitAll()
              
              
                ;
    	 //http.requestMatcher(EndpointRequest.toAnyEndpoint()).authorizeRequests()
         //.anyRequest().permitAll();
    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder())
                .withUser("zy")
                .password(new BCryptPasswordEncoder().encode("zy")).roles("USER")
                .and()
                .withUser("admin").password(new BCryptPasswordEncoder().encode("password")).roles("USER","ADMIN");
   }

    @Override
    public void configure(WebSecurity web) throws Exception {
        //解决静态资源被拦截的问题
        web.ignoring().antMatchers("/assets/**");
    }

}