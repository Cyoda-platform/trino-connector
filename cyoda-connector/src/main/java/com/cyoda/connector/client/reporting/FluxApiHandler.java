package com.cyoda.connector.client.reporting;

import com.cyoda.connector.SizeListener;
import reactor.core.publisher.Flux;

public interface FluxApiHandler<K, T> {
    Flux<T> asFlux(K requestKey, SizeListener listener);
}
