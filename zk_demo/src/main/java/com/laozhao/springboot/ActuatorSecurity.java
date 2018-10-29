package com.laozhao.springboot;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
                //.loginPage("/login").permitAll() //登录的路径
                    .failureForwardUrl("/?error=true")
                     
                .and()
                .logout()
                .logoutUrl("/logout").permitAll()
                .logoutSuccessUrl("/?logout=true")
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

    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
    }
}