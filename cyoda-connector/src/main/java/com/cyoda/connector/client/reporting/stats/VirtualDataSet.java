package com.cyoda.connector.client.reporting.stats;

public interface VirtualDataSet<T> {
    Iterable<T> getIterable();
}
