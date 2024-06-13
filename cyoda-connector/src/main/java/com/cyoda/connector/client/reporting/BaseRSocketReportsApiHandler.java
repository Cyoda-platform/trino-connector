package com.cyoda.connector.client.reporting;

import com.cyoda.connector.client.treenode.CyodaRSocketClient;

public class BaseRSocketReportsApiHandler {
    protected final CyodaRSocketClient rSocketClient;

    public BaseRSocketReportsApiHandler(CyodaRSocketClient rSocketClient) {
        this.rSocketClient = rSocketClient;
    }
}
