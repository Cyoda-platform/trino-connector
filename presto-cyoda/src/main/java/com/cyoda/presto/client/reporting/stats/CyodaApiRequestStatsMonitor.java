package com.cyoda.presto.client.reporting.stats;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.logging.SupplierLogger;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class CyodaApiRequestStatsMonitor {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaApiRequestStatsMonitor.class);
    private static final int MAX_RESPONSE_HOLDER_SIZE = 10;
    private final ConcurrentLinkedDeque<ApiRequestStats> requestStatsDeque = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<String, String> tempResponseHolder = new ConcurrentHashMap<>();
    private final AtomicLong currentQueueSize = new AtomicLong();
    private final CyodaConfig config;

    @Inject
    public CyodaApiRequestStatsMonitor(CyodaConfig config){
        this.config = config;
    };

    public void registerApiCall(String queryId, Date callTime, String requestUrl, Map<String, Object> params){
        if (!config.getLogApiCallStats()) return;

        String response = tempResponseHolder.remove(requestUrl);
        add(
                new ApiRequestStats(
                        queryId,
                        callTime,
                        requestUrl,
                        this.getClass().getSimpleName(),
                        params,
                        System.currentTimeMillis() - callTime.getTime(),
                        response));
    }
    public void add(ApiRequestStats requestStat){
        requestStatsDeque.addFirst(requestStat);
        long newSize = currentQueueSize.incrementAndGet();
        while (newSize > config.getApiCallStatsMaxRecords()){
            requestStatsDeque.removeLast();
            newSize = currentQueueSize.decrementAndGet();
        }
        if (config.getLogApiCallResponse() && tempResponseHolder.size() > MAX_RESPONSE_HOLDER_SIZE) {
            LOG.error("Response holder overflow - flushing data:\n" + tempResponseHolder.entrySet()
                    .stream()
                    .map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n")));
            tempResponseHolder.clear();
        }
    }

    public void addResponse(String uri, String response){
        tempResponseHolder.put(uri, response);
    }

    public void truncate(){
        requestStatsDeque.clear();
    }

    public Iterable<ApiRequestStats> getIterable(){
        return requestStatsDeque;
    }
}
