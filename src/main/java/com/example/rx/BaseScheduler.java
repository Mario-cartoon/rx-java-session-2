package com.example.rx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Базовая реализация планировщика  
 */
abstract class BaseScheduler implements Scheduler {
    private final ExecutorService executorService;

    protected BaseScheduler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Создает фабрику потоков с понятным префиксом имени
     */
    protected static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return task -> {
            Thread thread = new Thread(task, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void execute(Runnable task) {
        executorService.submit(task);
    }
}
