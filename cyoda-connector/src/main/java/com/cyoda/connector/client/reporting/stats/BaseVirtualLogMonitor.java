package com.cyoda.connector.client.reporting.stats;

import com.cyoda.connector.CyodaConfig;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class BaseVirtualLogMonitor<T> {

    private final ConcurrentLinkedDeque<T> recordsDeque = new ConcurrentLinkedDeque<>();
    private final AtomicLong currentQueueSize = new AtomicLong();
    private final CyodaConfig config;


    protected abstract long getMaxRecords(CyodaConfig config);

    public BaseVirtualLogMonitor(CyodaConfig config){
        this.config = config;
    };


    public void add(T logRecord){
        recordsDeque.addFirst(logRecord);
        long newSize = currentQueueSize.incrementAndGet();
        while (newSize > config.getApiCallStatsMaxRecords()){
            recordsDeque.removeLast();
            newSize = currentQueueSize.decrementAndGet();
        }
    }

    public void truncate(){
        recordsDeque.clear();
    }

    public Iterable<T> getIterable(){
        return recordsDeque;
    }
}
