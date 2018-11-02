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

package de.codecentric.boot.admin.server.services.endpoints;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.Endpoint;
import de.codecentric.boot.admin.server.domain.values.Endpoints;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;
import lombok.Data;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.http.MediaType;

public class QueryIndexEndpointStrategy implements EndpointDetectionStrategy {
    private final InstanceWebClient instanceWebClient;
    private static final MediaType actuatorMediaType = MediaType.parseMediaType(ActuatorMediaType.V2_JSON);

    public QueryIndexEndpointStrategy(InstanceWebClient instanceWebClient) {
        this.instanceWebClient = instanceWebClient;
    }

    @Override
    // 这里面定义了返回 
    public Mono<Endpoints> detectEndpoints(Instance instance) {
        Registration registration = instance.getRegistration();
        if (Objects.equals(registration.getServiceUrl(), registration.getManagementUrl())) {
            return Mono.empty();
        }

        return instanceWebClient.instance(instance)
                                .get()
                                .uri(instance.getRegistration().getManagementUrl())
                                //这个是直接去访问 请求 （类似 http://127.0.0.1:8186/actuator）然后返回数据
                                /**
                                 *  数据的格式为 参数名称都是一致的
                                 *   private Map<String, EndpointRef> _links;
							         @Data
							         static class EndpointRef {
							             private String href;
							             private boolean templated;
							         }
                                 */
                                .exchange()
                                .flatMap(response -> {
                                    if (response.statusCode().is2xxSuccessful() &&
                                        response.headers()
                                                .contentType()
                                                .map(actuatorMediaType::isCompatibleWith)
                                                .orElse(false)) {
                                        return response.bodyToMono(Response.class);
                                    } else {
                                        return response.bodyToMono(Void.class).then(Mono.empty());
                                    }
                                })
                                .flatMap(this::convert);
    }
    // 这里完成数据的处理 直接在线访问 
    private Mono<Endpoints> convert(Response response) {
        List<Endpoint> endpoints = response.get_links() 
                                           .entrySet()
                                           .stream()
                                           .filter(e -> !e.getKey().equals("self") && !e.getValue().isTemplated())
                                           .map(e -> Endpoint.of(e.getKey(), e.getValue().getHref()))
                                           .collect(Collectors.toList());
        if (endpoints.isEmpty()) {
            return Mono.empty();
        } else {
            return Mono.just(Endpoints.of(endpoints));
        }
    }

    @Data
    static class Response {
        private Map<String, EndpointRef> _links;

        @Data
        static class EndpointRef {
            private String href;
            private boolean templated;
        }
    }
}
/*   http://127.0.0.1:8186/actuator 
 * 返回的数据
{
    "_links": {
        "self": {
            "href": "http://127.0.0.1:8186/actuator",
            "templated": false
        },
        "archaius": {
            "href": "http://127.0.0.1:8186/actuator/archaius",
            "templated": false
        },
        "auditevents": {
            "href": "http://127.0.0.1:8186/actuator/auditevents",
            "templated": false
        },
        "beans": {
            "href": "http://127.0.0.1:8186/actuator/beans",
            "templated": false
        },
        "health": {
            "href": "http://127.0.0.1:8186/actuator/health",
            "templated": false
        },
        "conditions": {
            "href": "http://127.0.0.1:8186/actuator/conditions",
            "templated": false
        },
        "configprops": {
            "href": "http://127.0.0.1:8186/actuator/configprops",
            "templated": false
        },
        "env": {
            "href": "http://127.0.0.1:8186/actuator/env",
            "templated": false
        },
        "env-toMatch": {
            "href": "http://127.0.0.1:8186/actuator/env/{toMatch}",
            "templated": true
        },
        "info": {
            "href": "http://127.0.0.1:8186/actuator/info",
            "templated": false
        },
        "loggers": {
            "href": "http://127.0.0.1:8186/actuator/loggers",
            "templated": false
        },
        "loggers-name": {
            "href": "http://127.0.0.1:8186/actuator/loggers/{name}",
            "templated": true
        },
        "heapdump": {
            "href": "http://127.0.0.1:8186/actuator/heapdump",
            "templated": false
        },
        "threaddump": {
            "href": "http://127.0.0.1:8186/actuator/threaddump",
            "templated": false
        },
        "metrics": {
            "href": "http://127.0.0.1:8186/actuator/metrics",
            "templated": false
        },
        "metrics-requiredMetricName": {
            "href": "http://127.0.0.1:8186/actuator/metrics/{requiredMetricName}",
            "templated": true
        },
        "scheduledtasks": {
            "href": "http://127.0.0.1:8186/actuator/scheduledtasks",
            "templated": false
        },
        "httptrace": {
            "href": "http://127.0.0.1:8186/actuator/httptrace",
            "templated": false
        },
        "mappings": {
            "href": "http://127.0.0.1:8186/actuator/mappings",
            "templated": false
        },
        "refresh": {
            "href": "http://127.0.0.1:8186/actuator/refresh",
            "templated": false
        },
        "features": {
            "href": "http://127.0.0.1:8186/actuator/features",
            "templated": false
        },
        "service-registry": {
            "href": "http://127.0.0.1:8186/actuator/service-registry",
            "templated": false
        }
    }
}

*/