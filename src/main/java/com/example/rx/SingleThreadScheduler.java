package com.example.rx;

import java.util.concurrent.Executors;

/**
 * Планировщик для последовательного выполнения задач в одном потоке.
 */
public class SingleThreadScheduler extends BaseScheduler {
    public SingleThreadScheduler() {
        super(Executors.newSingleThreadExecutor(daemonThreadFactory("поток")));
    }
}
