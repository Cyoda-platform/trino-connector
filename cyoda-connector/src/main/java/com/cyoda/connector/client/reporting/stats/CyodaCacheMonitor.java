package com.cyoda.connector.client.reporting.stats;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class CyodaCacheMonitor {

    private final Map<String, CacheHandle> cacheMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> knownContentIds = new ConcurrentHashMap<>();

    @Inject
    public CyodaCacheMonitor(){
    }

    public <K, T> void register(String name, ContentIdLoadingCache<K, T> cache,
                                Function<K, String> keySerializer,
                                Function<T, Integer> valueSizeExtractor) {
        cacheMap.put(name, new CacheHandle<>(name, cache, keySerializer, valueSizeExtractor));
        cache.setNewContentIdListener(uuid -> knownContentIds.put(uuid, name));
        cache.setRemoveContentIdListener(knownContentIds::remove);
    }

    public Map<String, CacheStats> getStats(){
        return remap(cacheMap, cacheHandle -> cacheHandle.cache.stats());
    }

    public List<CacheContent> getContent(){
        return cacheMap.values().stream().flatMap(CacheHandle::getContent).toList();
    }

    public void removeContent(UUID contentId){
        String cacheName = knownContentIds.get(contentId);
        if (cacheName == null) return;
        cacheMap.get(cacheName).removeByContentId(contentId);
    }

    private record CacheHandle<K, T>(String cacheName,
                                     ContentIdLoadingCache<K, T> cache,
                                     Function<K, String> keySerializer,
                                     Function<T, Integer> valueSizeExtractor) {

        public Stream<CacheContent> getContent(){
            return cache.asMap().entrySet().stream().map(entry -> new CacheContent(
                    cache.getContentId(entry.getKey()),
                    cacheName,
                    keySerializer.apply(entry.getKey()),
                    valueSizeExtractor().apply(entry.getValue())
            ));
        }
        public void removeByContentId(UUID contentId){
            K removeKey = cache.getCacheKeyByContentId(contentId);
            cache.invalidate(removeKey);
        }
    }

    public record CacheContent(UUID contentId, String cacheName, String key, long size){};


    private <K, T1, T2> Map<K, T2> remap(Map<K, T1> source, Function<T1, T2> mapper){
        Map<K, T2> res = new HashMap<>();
        source.forEach((key, value) -> res.put(key, mapper.apply(value)));
        return res;
    }
}
