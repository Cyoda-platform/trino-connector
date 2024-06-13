package com.cyoda.presto.client.reporting;

import com.cyoda.presto.client.treenode.CyodaRSocketClient;

public class BaseRSocketReportsApiHandler {
    protected final CyodaRSocketClient rSocketClient;

    public BaseRSocketReportsApiHandler(CyodaRSocketClient rSocketClient) {
        this.rSocketClient = rSocketClient;
    }
}
