package com.example.rx;

/**
 * Определяет, в каком потоке или пуле потоков будет выполнена задача.
 */
public interface Scheduler {
    /**
     * Планирует выполнение задачи.
     *
     * @param task задача для выполнения
     */
    void execute(Runnable task);
}
