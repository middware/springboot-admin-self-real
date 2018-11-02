# springboot-admin-self-real
## spring-admin-server-cloud 
>通过微服务的注册中心 根据三个事件(这三个事件 是springboot框架的 要严格按照这个来写）来重注册中心 获取微服务的使用情况
这个模块在使用微服务的时候要添加  这里面获取信息最主要的事件是HeartbeatEvent   
另外 在获取完信息后，需要进行数据信息的转换 以及存储等操作
```
 @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
    	System.out.println("----------onApplicationReady----------");
    	discover();
    }

    @EventListener
    public void onInstanceRegistered(InstanceRegisteredEvent<?> event) {
        System.out.println("----------onInstanceRegistered----------");
    	discover();
    }

    @EventListener
    public void onParentHeartbeat(ParentHeartbeatEvent event) {
    	 System.out.println("----------onParentHeartbeat----------");
        discoverIfNeeded(event.getValue());
    }

    @EventListener
    public void onApplicationEvent(HeartbeatEvent event) {
    	 System.out.println("----------HeartbeatEvent----------");
        discoverIfNeeded(event.getValue());
    }
```
---
## spring-admin-server
>这个是主要的工作模块主要有以下的功能
* 1. 存储微服务信息（内存中 这个可以自己定义 需要进行扩展）
* 2. 定时更新微服务信息
* 3. 获取微服务所开放的监控指标
* 4. 暴露web接口 返回 前台信息

>这里有两个核心的配置类 AdminServerAutoConfiguration 这个类 定义了前三点所需的bean的 以及系统启动后的初始化方法
```
public class AdminServerAutoConfiguration {
    private final AdminServerProperties adminServerProperties;

   //这里定义了一些参数 包括连接超时 请求超时 以及一些端口的url等
    public AdminServerAutoConfiguration(AdminServerProperties adminServerProperties) {
        this.adminServerProperties = adminServerProperties;
    }

   
    @Bean
    @ConditionalOnMissingBean
    // 微服务信息获取后 使用该类 注册到该系统 其中
    //InstanceRepository这个实例 我们在这里使用的是下方一个子类SnapshottingInstanceRepository 我们获取的微服务信息 都是存到这这里
    //前端获取的服务信息 也要从这里取
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
    //这个是webclient 在向对应的微服务请求数据的时候 添加的头 用来做安全验证
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
   // 这个定时更新微服务中的信息 同时使用webflux的发布订阅模式 对微服务信息进行跟新
    public StatusUpdateTrigger statusUpdateTrigger(StatusUpdater statusUpdater, Publisher<InstanceEvent> events) {
    	System.out.println("StatusUpdateTrigger--->"+events.getClass().getName()+"---hash:"+events.hashCode());
    	StatusUpdateTrigger trigger = new StatusUpdateTrigger(statusUpdater, events);
        trigger.setUpdateInterval(adminServerProperties.getMonitor().getPeriod());
        trigger.setStatusLifetime(adminServerProperties.getMonitor().getStatusLifetime());
        return trigger;
    }

    
    //请求 在这个里面添加
    @Bean
    @ConditionalOnMissingBean
    // 开放 的端口探测  这里定义了两个 一个直接在线请求  一个单独请求  在线请求失败后 分别单独请求
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
    	System.out.println("EndpointDetectionTrigger--->"+events.getClass().getName()+"---hash:"+events.hashCode());
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
    	System.out.println("InfoUpdateTrigger--->"+events.getClass().getName()+"---hash:"+events.hashCode());
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

```
# server-admin-ui
>这个模块 没什么好说的 这里用的是vue 唯一要说的一点是 里面一个插件 的一个依赖的包需要下载新版的 旧版的下不到了 另外 该项目修改后 需要mvn package
因为 静态文件会node整理到dist文件夹中

以下是插件以及插件对应的依赖 测试部分被注释掉了 
```
 <dependency>
		    <groupId>org.codehaus.mojo</groupId>
		    <artifactId>exec-maven-plugin</artifactId>
		    <version>1.6.0</version>
            <exclusions>
                <exclusion>
                    <groupId>com.jcraft</groupId>
                    <artifactId>jsch</artifactId>
                </exclusion>
            </exclusions>
    </dependency>

        <dependency>
            <groupId>com.jcraft</groupId>
            <artifactId>jsch</artifactId>
            <version>0.1.53</version>
        </dependency>
        <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <executions>
                    <!-- <execution>
                        <id>npm-install</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>npm</executable>
                            <arguments>
                                <argument>install</argument>
                            </arguments>
                        </configuration>
                    </execution> -->
                    <execution>
                        <id>npm-build</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>npm</executable>
                            <arguments>
                                <argument>run</argument>
                                <argument>build</argument>
                            </arguments>
                            <environmentVariables>
                                <PROJECT_VERSION>${project.version}</PROJECT_VERSION>
                            </environmentVariables>
                        </configuration>
                    </execution>
                   <!--  <execution>
                        <id>npm-test</id>
                        <phase>test</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <skip>${skipTests}</skip>
                            <executable>npm</executable>
                            <arguments>
                                <argument>run</argument>
                                <argument>test</argument>
                            </arguments>
                        </configuration>
                    </execution> -->
                </executions>
            </plugin>
 ```
 ---      
  # 模块间总体的工作流程
  这个感觉用画图 会很清楚 先口头说一下
  以上三个模块是主要的工作模块（还有一个spring-admin-client 不过有zk后 估计也不会去用它进行服务注册了）
  首先 spring-admin-server-cloud 根据springboot自身微服务的工作机制 根据心跳 使用discoverClient来获取微服务的信息 在获取完信息后，进行数据的
  包装 转换为系统自己的domian 然后将数据提交到 admin-server
  admin-server处理domain里面的信息 然后根据实际情况生成对应的事件 之后进行分发（webflux的publisher subscribe）完成后续的数据的处理这里面有以下
  几个进行事件的处理
 * StatusUpdateTrigger 
  捕获事件后进行 数据信息的跟新 同时自身也运行一个定时任务 来定时刷新服务的信息
 * EndpointDetectionTrigger 
 在有新的服务信息进来后 该类 会探测该微服务所开放的可监控端口 他这里使用这样的策略 首先直接发送这个是直接去访问 
 请求 （类似 http://127.0.0.1:8186/actuator）如果有返回数据 那么直接使用该数据 如果没有这使用下一个进行探查（这里我们可以定义自己的）
 ```
 de.codecentric.boot.admin.server.services.endpoints.ChainingStrategy
 /*
 *QueryIndexEndpointStrategy  访问请求 （类似 http://127.0.0.1:8186/actuator）
 *ProbeEndpointsStrategy      单个请求
 */
 @Override
    public Mono<Endpoints> detectEndpoints(Instance instance) {
        Mono<Endpoints> result = Mono.empty();
        for (EndpointDetectionStrategy delegate : delegates) {
        	 System.out.println(delegate.getClass().getName());
        	 //按照打印的结果来看 先直接在线访问 如果有的话 就 不进行单独的请求 没有的话 switchIfEmpty 则继续访问  
             result = result.switchIfEmpty(delegate.detectEndpoints(instance));
        }
        return result.switchIfEmpty(Mono.just(Endpoints.empty()));
    }
  ```
 * InfoUpdateTrigger
 这个好像也没干什么事 就是更新信息吧
 
 最后admin-server还使用webflux暴露web接口 让admin-server-ui模块可以进行数据访问
 这里要说一点 admin-server本身不存储每个微服务的 线程啊 内存使用什么的 每次ui模块获取 都是根据其提供的参数信息 然后由admin-server使用webclient
 实际去请求对应的微服务然后返回的
