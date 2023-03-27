package com.cyoda.presto.client.reporting.stats;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class ContentIdLoadingCache<K, V> implements LoadingCache<K, V> {

    private final LoadingCache<K, V> nested;
    private final Map<K, UUID> keyMap = new HashMap<>();
    private final Map<UUID, K> contendIdMap = new HashMap<>();

    private Consumer<UUID> newContentIdListener;
    private Consumer<UUID> removeContentIdListener;

    public ContentIdLoadingCache(LoadingCache<K, V> nested) {
        this.nested = nested;
    }

    private void ensureKeyMapped(K key){
        if (!keyMap.containsKey(key)){
            UUID generated = UUID.randomUUID();
            keyMap.put(key, generated);
            contendIdMap.put(generated, key);
            newContentIdListener.accept(generated);
        }
    }

    private void removeCacheKey(Object key){
        UUID removed = keyMap.remove(key);
        contendIdMap.remove(removed);
        removeContentIdListener.accept(removed);
    }

    private void ensureKeysMapped(@NonNull Iterable<? extends @NonNull K> keys){
        for (K key : keys){
            ensureKeyMapped(key);
        }
    }
    public UUID getContentId(K key){
        return keyMap.get(key);
    }
    public K getCacheKeyByContentId(UUID contentId){
        return contendIdMap.get(contentId);
    }

    public void setNewContentIdListener(Consumer<UUID> newContentIdListener) {
        this.newContentIdListener = newContentIdListener;
    }

    public void setRemoveContentIdListener(Consumer<UUID> removeContentIdListener) {
        this.removeContentIdListener = removeContentIdListener;
    }

    @Override
    public @Nullable V get(@NonNull K key) {
        ensureKeyMapped(key);
        return nested.get(key);
    }

    @Override
    public @NonNull Map<@NonNull K, @NonNull V> getAll(@NonNull Iterable<? extends @NonNull K> keys) {
        ensureKeysMapped(keys);
        return nested.getAll(keys);
    }

    @Override
    public void refresh(@NonNull K key) {
        ensureKeyMapped(key);
        nested.refresh(key);
    }

    @Override
    public @Nullable V getIfPresent(@NonNull Object key) {
        return nested.getIfPresent(key);
    }

    @Override
    public @Nullable V get(@NonNull K key, @NonNull Function<? super K, ? extends V> mappingFunction) {
        ensureKeyMapped(key);
        return nested.get(key, mappingFunction);
    }

    @Override
    public @NonNull Map<@NonNull K, @NonNull V> getAllPresent(@NonNull Iterable<@NonNull ?> keys) {
        return nested.getAllPresent(keys);
    }

    @Override
    public void put(@NonNull K key, @NonNull V value) {
        ensureKeyMapped(key);
        nested.put(key, value);
    }

    @Override
    public void putAll(@NonNull Map<? extends @NonNull K, ? extends @NonNull V> map) {
        ensureKeysMapped(map.keySet());
        nested.putAll(map);
    }

    @Override
    public void invalidate(@NonNull Object key) {
        nested.invalidate(key);
        removeCacheKey(key);
    }

    @Override
    public void invalidateAll(@NonNull Iterable<@NonNull ?> keys) {
        nested.invalidateAll(keys);
        for (Object key : keys){
            removeCacheKey(key);
        }
    }

    @Override
    public void invalidateAll() {
        nested.invalidateAll();
        contendIdMap.clear();
    }

    @Override
    public @NonNegative long estimatedSize() {
        return nested.estimatedSize();
    }

    @Override
    public @NonNull CacheStats stats() {
        return nested.stats();
    }

    @Override
    public @NonNull ConcurrentMap<@NonNull K, @NonNull V> asMap() {
        return nested.asMap();
    }

    @Override
    public void cleanUp() {
        Set<K> targetKeySet = nested.asMap().keySet();
        for (K key : keyMap.keySet()){
            if (!targetKeySet.contains(key)){
                removeCacheKey(key);
            }
        }
        nested.cleanUp();
    }

    @Override
    public @NonNull Policy<K, V> policy() {
        return nested.policy();
    }
}
