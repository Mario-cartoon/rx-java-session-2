package com.example.rx;

import java.util.concurrent.Executors;

/**
 * Планировщик для вычислительных задач.
 * Использует фиксированный пул потоков.
 */
public class ComputationScheduler extends BaseScheduler {
    public ComputationScheduler() {
        super(Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                daemonThreadFactory("rx-computation")
        ));
    }
}
