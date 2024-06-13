package com.cyoda.presto.client.reporting;

import com.cyoda.presto.SizeListener;
import reactor.core.publisher.Flux;

public interface FluxApiHandler<K, T> {
    Flux<T> asFlux(K requestKey, SizeListener listener);
}
