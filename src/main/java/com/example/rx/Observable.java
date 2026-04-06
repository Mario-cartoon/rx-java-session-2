package com.example.rx;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Упрощенная реализация реактивного источника данных.
 *
 * @param <T> тип элементов потока
 */
public class Observable<T> {
    /**
     * Функциональный интерфейс для создания логики источника.
     *
     * @param <T> тип элементов потока
     */
    @FunctionalInterface
    public interface OnSubscribe<T> {
        /**
         * Описывает, как источник отправляет события подписчику.
         *
         * @param emitter объект для отправки событий
         * @throws Exception любая ошибка в процессе генерации данных
         */
        void subscribe(Emitter<T> emitter) throws Exception;
    }

    private final OnSubscribe<T> source;

    private Observable(OnSubscribe<T> source) {
        this.source = source;
    }

    /**
     * Создает новый {@link Observable} с пользовательской логикой эмиссии.
     *
     * @param source логика создания событий
     * @param <T> тип элементов потока
     * @return новый поток
     */
    public static <T> Observable<T> create(OnSubscribe<T> source) {
        Objects.requireNonNull(source, "Источник не должен быть пустым");
        return new Observable<>(source);
    }

    /**
     * Создает поток из набора готовых значений.
     *
     * @param items элементы, которые будут отправлены подписчику
     * @param <T> тип элементов потока
     * @return новый поток
     */
    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create(emitter -> {
            for (T item : items) {
                if (emitter.isDisposed()) {
                    return;
                }
                emitter.onNext(item);
            }
            emitter.onComplete();
        });
    }

    /**
     * Подписывает наблюдателя на текущий поток.
     *
     * @param observer получатель событий
     * @return объект для отмены подписки
     */
    public Disposable subscribe(Observer<? super T> observer) {
        Objects.requireNonNull(observer, "Наблюдатель не должен быть пустым");
        SafeEmitter<T> emitter = new SafeEmitter<>(observer);
        try {
            source.subscribe(emitter);
        } catch (Throwable throwable) {
            emitter.onError(throwable);
        }
        return emitter;
    }

    /**
     * Преобразует каждый элемент потока.
     *
     * @param mapper функция преобразования
     * @param <R> тип новых элементов
     * @return новый поток с преобразованными значениями
     */
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "Функция преобразования не должна быть пустой");
        return create(emitter -> emitter.setDisposable(Observable.this.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (emitter.isDisposed()) {
                    return;
                }
                try {
                    emitter.onNext(mapper.apply(item));
                } catch (Throwable throwable) {
                    emitter.onError(throwable);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        })));
    }

    /**
     * Оставляет только те элементы, которые подходят под условие.
     *
     * @param predicate условие фильтрации
     * @return новый поток с отфильтрованными значениями
     */
    public Observable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "Условие фильтрации не должно быть пустым");
        return create(emitter -> emitter.setDisposable(Observable.this.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (emitter.isDisposed()) {
                    return;
                }
                try {
                    if (predicate.test(item)) {
                        emitter.onNext(item);
                    }
                } catch (Throwable throwable) {
                    emitter.onError(throwable);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        })));
    }

    /**
     * Для каждого элемента создает внутренний поток и объединяет результаты.
     *
     * @param mapper функция преобразования элемента во внутренний поток
     * @param <R> тип элементов внутреннего потока
     * @return новый поток с объединенными значениями
     */
    public <R> Observable<R> flatMap(Function<? super T, Observable<R>> mapper) {
        Objects.requireNonNull(mapper, "Функция преобразования не должна быть пустой");
        return create(emitter -> {
            CompositeDisposable composite = new CompositeDisposable();
            emitter.setDisposable(composite);
            AtomicInteger activeSources = new AtomicInteger(1);
            AtomicBoolean terminated = new AtomicBoolean(false);

            Disposable upstream = Observable.this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (emitter.isDisposed() || terminated.get()) {
                        return;
                    }

                    Observable<R> innerObservable;
                    try {
                        innerObservable = Objects.requireNonNull(mapper.apply(item), "Функция flatMap вернула пустой поток");
                    } catch (Throwable throwable) {
                        onError(throwable);
                        return;
                    }

                    activeSources.incrementAndGet();
                    Disposable innerDisposable = innerObservable.subscribe(new Observer<R>() {
                        @Override
                        public void onNext(R innerItem) {
                            if (!emitter.isDisposed() && !terminated.get()) {
                                emitter.onNext(innerItem);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (terminated.compareAndSet(false, true)) {
                                composite.dispose();
                                emitter.onError(throwable);
                            }
                        }

                        @Override
                        public void onComplete() {
                            if (activeSources.decrementAndGet() == 0 && terminated.compareAndSet(false, true)) {
                                emitter.onComplete();
                            }
                        }
                    });
                    composite.add(innerDisposable);
                }

                @Override
                public void onError(Throwable throwable) {
                    if (terminated.compareAndSet(false, true)) {
                        composite.dispose();
                        emitter.onError(throwable);
                    }
                }

                @Override
                public void onComplete() {
                    if (activeSources.decrementAndGet() == 0 && terminated.compareAndSet(false, true)) {
                        emitter.onComplete();
                    }
                }
            });

            composite.add(upstream);
        });
    }

    /**
     * Переносит запуск источника в указанный планировщик.
     *
     * @param scheduler планировщик выполнения
     * @return новый поток
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "Планировщик не должен быть пустым");
        return create(emitter -> {
            BooleanDisposable taskDisposable = new BooleanDisposable();
            emitter.setDisposable(taskDisposable);

            scheduler.execute(() -> {
                if (emitter.isDisposed() || taskDisposable.isDisposed()) {
                    return;
                }

                Disposable upstream = Observable.this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        emitter.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                });

                emitter.setDisposable(upstream);
            });
        });
    }

    /**
     * Переносит обработку событий подписчиком в указанный планировщик.
     *
     * @param scheduler планировщик выполнения
     * @return новый поток
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "Планировщик не должен быть пустым");
        return create(emitter -> emitter.setDisposable(Observable.this.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                scheduler.execute(() -> emitter.onNext(item));
            }

            @Override
            public void onError(Throwable throwable) {
                scheduler.execute(() -> emitter.onError(throwable));
            }

            @Override
            public void onComplete() {
                scheduler.execute(emitter::onComplete);
            }
        })));
    }

    /**
     * Безопасная обертка над observer, которая не допускает повторного
     * завершения потока и умеет отменять связанные подписки.
     *
     * @param <T> тип элементов потока
     */
    private static final class SafeEmitter<T> implements Emitter<T> {
        private final Observer<? super T> observer;
        private final CompositeDisposable compositeDisposable = new CompositeDisposable();
        private final AtomicBoolean disposed = new AtomicBoolean(false);
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        private SafeEmitter(Observer<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(T item) {
            if (disposed.get() || terminated.get()) {
                return;
            }
            observer.onNext(item);
        }

        @Override
        public void setDisposable(Disposable disposable) {
            compositeDisposable.add(disposable);
        }

        @Override
        public void onError(Throwable throwable) {
            if (disposed.get() || !terminated.compareAndSet(false, true)) {
                return;
            }
            observer.onError(throwable);
            dispose();
        }

        @Override
        public void onComplete() {
            if (disposed.get() || !terminated.compareAndSet(false, true)) {
                return;
            }
            observer.onComplete();
            dispose();
        }

        @Override
        public void dispose() {
            if (disposed.compareAndSet(false, true)) {
                compositeDisposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}
