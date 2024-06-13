package com.cyoda.connector.client.reporting;

import java.util.List;

public interface CachedReportsApi<K, T> {
    List<T> getByKey(K requestKey);
}
