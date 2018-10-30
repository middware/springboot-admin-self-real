/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.boot.admin.server.cloud.config;

import de.codecentric.boot.admin.server.cloud.discovery.DefaultServiceInstanceConverter;
import de.codecentric.boot.admin.server.cloud.discovery.EurekaServiceInstanceConverter;
import de.codecentric.boot.admin.server.cloud.discovery.InstanceDiscoveryListener;
import de.codecentric.boot.admin.server.cloud.discovery.ServiceInstanceConverter;
import de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration;
import de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.services.InstanceRegistry;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.netflix.discovery.EurekaClient;
/**
 * 
 * @author viruser
 *  主要是配置 从而实现 与注册中心的链接  
 *  监听服务的注册与注销
 */
@Configuration
@ConditionalOnSingleCandidate(DiscoveryClient.class)
@ConditionalOnBean(AdminServerMarkerConfiguration.Marker.class)
//这个在 该包下的 META-INF/additional-spring-configuration-metadata.json的文件中声明  并带有默认值
//@ConditionalOnProperty 可以通过设置该属性来决定是否启用 通过注册中心来发现服务
@ConditionalOnProperty(prefix = "spring.boot.admin.discovery", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(value = AdminServerAutoConfiguration.class, name = {
    "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
    "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration"})
public class AdminServerDiscoveryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "spring.boot.admin.discovery")
    public InstanceDiscoveryListener instanceDiscoveryListener(ServiceInstanceConverter serviceInstanceConverter,
                                                               DiscoveryClient discoveryClient,
                                                               InstanceRegistry registry,
                                                               InstanceRepository repository) {
        InstanceDiscoveryListener listener = new InstanceDiscoveryListener(discoveryClient, registry, repository);
        listener.setConverter(serviceInstanceConverter);
        return listener;
    }

    // 这里转换器加载 是单例模式  如果使用的是Eureka 作为注册中心  使用EurekaServiceInstanceConverter
    // 否则 使用 DefaultServiceInstanceConverter
    // @ConditionalOnMissingBean 该标签标表示 上下文不存在该类实例时 触发家标记的方法
    @Configuration
    @ConditionalOnMissingBean({ServiceInstanceConverter.class})
    @ConditionalOnBean(EurekaClient.class)
    public static class EurekaConverterConfiguration {
        @Bean
        @ConfigurationProperties(prefix = "spring.boot.admin.discovery.converter")
        public EurekaServiceInstanceConverter serviceInstanceConverter() {
            return new EurekaServiceInstanceConverter();
        }
    }

    @Bean
    @ConditionalOnMissingBean({ServiceInstanceConverter.class})
    @ConfigurationProperties(prefix = "spring.boot.admin.discovery.converter")
    public DefaultServiceInstanceConverter serviceInstanceConverter() {
        return new DefaultServiceInstanceConverter();
    }
}
