package com.cyoda.connector.client.reporting.stats;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.logging.SupplierLogger;

import jakarta.inject.Inject;
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

    public void registerApiCall(String queryId, Date callTime, String requestUrl, Map<String, Object> params, String apiHandlerName){
        if (!config.getLogApiCallStats()) return;
//        String strParams = Joiner.on(";").withKeyValueSeparator("=").join(params);
        String response = tempResponseHolder.remove(requestUrl);
        Map<String, String> strParams = params.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        registerApiCall(queryId,callTime,requestUrl, apiHandlerName, strParams, response);
    }
    public void registerApiCall(String queryId, Date callTime, String requestUrl, String apiHandlerName, Map<String, String> request, Object response){

        add(
                new ApiRequestStats(
                        queryId,
                        callTime,
                        requestUrl,
                        apiHandlerName,
                        System.currentTimeMillis() - callTime.getTime(),
                        request,
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
