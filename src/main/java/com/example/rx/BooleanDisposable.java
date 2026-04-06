package com.example.rx;

import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Простая реализация Disposable, которая хранит только один факт: подписка отменена или нет.
 * По сути это флажок:
 * false — подписка еще активна
 * true — подписка уже отменена
 */
final class BooleanDisposable implements Disposable {
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    @Override
    public void dispose() {
        disposed.set(true);
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
}
