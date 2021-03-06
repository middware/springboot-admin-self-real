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

package de.codecentric.boot.admin.server.services;

import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceRegisteredEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceRegistrationUpdatedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUpdateTrigger extends ResubscribingEventHandler<InstanceEvent> {
    private static final Logger log = LoggerFactory.getLogger(StatusUpdateTrigger.class);
    private final StatusUpdater statusUpdater;
    private Map<InstanceId, Instant> lastQueried = new HashMap<>();
    private Duration updateInterval = Duration.ofSeconds(60*1);
    private Duration statusLifetime = Duration.ofSeconds(60*1);
    private Disposable intervalSubscription;


    public StatusUpdateTrigger(StatusUpdater statusUpdater, Publisher<InstanceEvent> publisher) {
        super(publisher, InstanceEvent.class);
        this.statusUpdater = statusUpdater;
    }

    @Override
    public void start() {
        super.start();
        intervalSubscription = Flux.interval(updateInterval)
                                   .doOnSubscribe(
                                       subscription -> log.debug("Scheduled status update every {}", updateInterval))
                                   .log(log.getName(), Level.FINEST)
                                   .subscribeOn(Schedulers.newSingle("status-monitor"))
                                   .flatMap((i) -> this.updateStatusForAllInstances())
                                   .retryWhen(Retry.any()
                                                   .retryMax(Integer.MAX_VALUE)
                                                   .doOnRetry(ctx -> log.error("Resubscribing after uncaught error",
                                                       ctx.exception())))
                                   .subscribe();
    }

    @Override
    protected Publisher<?> handle(Flux<InstanceEvent> publisher) {
        return publisher.subscribeOn(Schedulers.newSingle("status-updater"))
                        .filter(event -> event instanceof InstanceRegisteredEvent ||
                                         event instanceof InstanceRegistrationUpdatedEvent)
                        .flatMap(event -> updateStatus(event));
    }

    @Override
    public void stop() {
        super.stop();
        if (intervalSubscription != null) {
            intervalSubscription.dispose();
        }
    }

    protected Mono<Void> updateStatusForAllInstances() {
        log.debug("Updating status for all instances");
        Instant expiryInstant = Instant.now().minus(statusLifetime);
        return Flux.fromIterable(lastQueried.entrySet())
                   .filter(e -> e.getValue().isBefore(expiryInstant))
                   .map(Map.Entry::getKey)
                   .flatMap(this::updateStatusId)
                   .then();
    }

    protected Mono<Void> updateStatus(InstanceEvent event) {
    	System.out.println("EndpointDetectionTrigger deal event--->hash："+event.hashCode()+" name:"+event.getClass().getSimpleName()+" time:"+System.nanoTime());
        return statusUpdater.updateStatus(event.getInstance()).doFinally((s) -> lastQueried.put(event.getInstance(), Instant.now()));
    }
    protected Mono<Void> updateStatusId(InstanceId instanceId) {
        return statusUpdater.updateStatus(instanceId).doFinally((s) -> lastQueried.put(instanceId, Instant.now()));
    }

    public void setUpdateInterval(Duration updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setStatusLifetime(Duration statusLifetime) {
        this.statusLifetime = statusLifetime;
    }
}
