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

package de.codecentric.boot.admin.server.config;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.entities.SnapshottingInstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.eventstore.InMemoryEventStore;
import de.codecentric.boot.admin.server.eventstore.InstanceEventStore;
import de.codecentric.boot.admin.server.services.EndpointDetectionTrigger;
import de.codecentric.boot.admin.server.services.EndpointDetector;
import de.codecentric.boot.admin.server.services.HashingInstanceUrlIdGenerator;
import de.codecentric.boot.admin.server.services.InfoUpdateTrigger;
import de.codecentric.boot.admin.server.services.InfoUpdater;
import de.codecentric.boot.admin.server.services.InstanceIdGenerator;
import de.codecentric.boot.admin.server.services.InstanceRegistry;
import de.codecentric.boot.admin.server.services.StatusUpdateTrigger;
import de.codecentric.boot.admin.server.services.StatusUpdater;
import de.codecentric.boot.admin.server.services.endpoints.ChainingStrategy;
import de.codecentric.boot.admin.server.services.endpoints.ProbeEndpointsStrategy;
import de.codecentric.boot.admin.server.services.endpoints.QueryIndexEndpointStrategy;
import de.codecentric.boot.admin.server.web.client.BasicAuthHttpHeaderProvider;
import de.codecentric.boot.admin.server.web.client.CompositeHttpHeadersProvider;
import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import de.codecentric.boot.admin.server.web.client.InstanceExchangeFilterFunction;
import de.codecentric.boot.admin.server.web.client.InstanceExchangeFilterFunctions;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnBean(AdminServerMarkerConfiguration.Marker.class)
@EnableConfigurationProperties(AdminServerProperties.class)
@Import({AdminServerWebConfiguration.class})
public class AdminServerAutoConfiguration {
    private final AdminServerProperties adminServerProperties;

    public AdminServerAutoConfiguration(AdminServerProperties adminServerProperties) {
        this.adminServerProperties = adminServerProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public InstanceRegistry instanceRegistry(InstanceRepository instanceRepository,
                                             InstanceIdGenerator instanceIdGenerator) {
        return new InstanceRegistry(instanceRepository, instanceIdGenerator);
    }

    @Bean
    @ConditionalOnMissingBean 
    public InstanceIdGenerator instanceIdGenerator() {
        return new HashingInstanceUrlIdGenerator();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public CompositeHttpHeadersProvider httpHeadersProvider(Collection<HttpHeadersProvider> delegates) {
        if(delegates!=null&&delegates.size()>0) {
        	for(HttpHeadersProvider tt:delegates)
        		System.out.println("CompositeHttpHeadersProvider--->"+tt.getClass().getName());
        }
    	return new CompositeHttpHeadersProvider(delegates);
    }

    @Bean
    @Order(0)
    @ConditionalOnMissingBean
    public BasicAuthHttpHeaderProvider basicAuthHttpHeadersProvider() {
        return new BasicAuthHttpHeaderProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public StatusUpdater statusUpdater(InstanceRepository instanceRepository, InstanceWebClient instanceWebClient) {
        return new StatusUpdater(instanceRepository, instanceWebClient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public StatusUpdateTrigger statusUpdateTrigger(StatusUpdater statusUpdater, Publisher<InstanceEvent> events) {
        StatusUpdateTrigger trigger = new StatusUpdateTrigger(statusUpdater, events);
        trigger.setUpdateInterval(adminServerProperties.getMonitor().getPeriod());
        trigger.setStatusLifetime(adminServerProperties.getMonitor().getStatusLifetime());
        return trigger;
    }

    
    //请求 在这个里面添加
    @Bean
    @ConditionalOnMissingBean
    public EndpointDetector endpointDetector(InstanceRepository instanceRepository,
                                             InstanceWebClient instanceWebClient) {
        ChainingStrategy strategy = new ChainingStrategy(new QueryIndexEndpointStrategy(instanceWebClient),
                
        		//adminServerProperties.getProbedEndpoints()  这个返回所有可能的已经定义好的接口 url
        		new ProbeEndpointsStrategy(instanceWebClient, adminServerProperties.getProbedEndpoints()));
        return new EndpointDetector(instanceRepository, strategy);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    // 看这个类貌似 这里还自己定义了事件分发 以及自动的触发机制
    public EndpointDetectionTrigger endpointDetectionTrigger(EndpointDetector endpointDetector,
                                                             Publisher<InstanceEvent> events) {
        return new EndpointDetectionTrigger(endpointDetector, events);
    }

    @Bean
    @ConditionalOnMissingBean
    public InfoUpdater infoUpdater(InstanceRepository instanceRepository, InstanceWebClient instanceWebClient) {
        return new InfoUpdater(instanceRepository, instanceWebClient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    //同样的事件触发机制
    public InfoUpdateTrigger infoUpdateTrigger(InfoUpdater infoUpdater, Publisher<InstanceEvent> events) {
        return new InfoUpdateTrigger(infoUpdater, events);
    }

    @Bean
    @ConditionalOnMissingBean(InstanceEventStore.class)
    // 事件存储 
    public InMemoryEventStore eventStore() {
        return new InMemoryEventStore();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean(InstanceRepository.class)
    public SnapshottingInstanceRepository instanceRepository(InstanceEventStore eventStore) {
        return new SnapshottingInstanceRepository(eventStore);
    }

    @Bean
    @ConditionalOnMissingBean
    // 这个是实际请求服务的 运行参数 的http工具类
    public InstanceWebClient instanceWebClient(HttpHeadersProvider httpHeadersProvider,
                                               ObjectProvider<List<InstanceExchangeFilterFunction>> filtersProvider) {
        List<InstanceExchangeFilterFunction> filters = filtersProvider.getIfAvailable(Collections::emptyList);
        WebClientCustomizer customizer = (webClient) -> filters.forEach(instanceFilter -> webClient.filter(
            InstanceExchangeFilterFunctions.toExchangeFilterFunction(instanceFilter)));
          System.out.println("InstanceWebClient-->"+customizer.toString());
        return new InstanceWebClient(httpHeadersProvider, adminServerProperties.getMonitor().getConnectTimeout(),
            adminServerProperties.getMonitor().getReadTimeout(), customizer);
    }

}
