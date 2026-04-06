package com.example.rx;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Объект, который хранит сразу несколько Disposable и умеет отменить их все одной командой.
 */
final class CompositeDisposable implements Disposable {
    private final CopyOnWriteArrayList<Disposable> disposables = new CopyOnWriteArrayList<>();
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    public void add(Disposable disposable) {
        if (disposable == null) {
            return;
        }
        if (disposed.get()) {
            disposable.dispose();
            return;
        }
        disposables.add(disposable);
        if (disposed.get()) {
            disposables.remove(disposable);
            disposable.dispose();
        }
    }

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            for (Disposable disposable : disposables) {
                disposable.dispose();
            }
            disposables.clear();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
}
