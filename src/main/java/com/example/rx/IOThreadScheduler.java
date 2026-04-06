package com.example.rx;

import java.util.concurrent.Executors;

/**
 * Планировщик для задач ввода-вывода.
 * Использует кешируемый пул потоков.
 */
public class IOThreadScheduler extends BaseScheduler {
    public IOThreadScheduler() {
        super(Executors.newCachedThreadPool(daemonThreadFactory("rx-io")));
    }
}
