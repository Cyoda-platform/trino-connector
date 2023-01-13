package com.cyoda.presto.client.reporting.stats;

import com.cyoda.presto.logging.SupplierLogger;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class CyodaApiRequestStatsMonitor {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaApiRequestStatsMonitor.class);

    private static final long MAX_SIZE = 10; //TODO small for testing
    private final ConcurrentLinkedDeque<ApiRequestStats> requestStatsDeque = new ConcurrentLinkedDeque<>();
    private final AtomicLong currentQueueSize = new AtomicLong();

    @Inject
    public CyodaApiRequestStatsMonitor(){};

    public void add(ApiRequestStats requestStat){
        requestStatsDeque.addFirst(requestStat);
        long newSize = currentQueueSize.incrementAndGet();
        while (newSize > MAX_SIZE){
            requestStatsDeque.removeLast();
            newSize = currentQueueSize.decrementAndGet();
        }
    }

    public Iterable<ApiRequestStats> getIterable(){
        return requestStatsDeque;
    }
}
