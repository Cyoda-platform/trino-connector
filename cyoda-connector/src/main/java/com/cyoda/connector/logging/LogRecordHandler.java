package com.cyoda.connector.logging;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class LogRecordHandler extends Handler {
    private static final int MAX_SIZE = 1000;
    private final LinkedBlockingQueue<LogRecord> logRecords = new LinkedBlockingQueue<>(MAX_SIZE);
    private static volatile LogRecordHandler instance;

    private LogRecordHandler() {
    }

    public static LogRecordHandler getInstance() {
        if (instance == null) {
            synchronized (LogRecordHandler.class) {
                if (instance == null) {
                    instance = new LogRecordHandler();
                }
            }
        }
        return instance;
    }

    @Override
    public void publish(LogRecord record) {
        // Try to add to the queue, removing the oldest if at capacity
        while (!logRecords.offer(record)) {
            logRecords.poll();
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        logRecords.clear();
    }

    public List<LogRecord> getLogRecords() {
        return logRecords.stream().collect(Collectors.toList());
    }
}
