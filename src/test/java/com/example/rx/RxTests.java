package com.example.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RxTests {
    public static void main(String[] args) throws Exception {
        RxTests tests = new RxTests();
        tests.testCreateAndSubscribe();
        tests.testMap();
        tests.testFilter();
        tests.testFlatMap();
        tests.testErrorHandling();
        tests.testDisposableStopsFlow();
        tests.testSubscribeOnUsesAnotherThread();
        tests.testObserveOnUsesTargetScheduler();
        System.out.println("Все тесты пройдены");
    }

    private void testCreateAndSubscribe() {
        List<Integer> items = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger();

        Observable.just(1, 2, 3).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError("Ошибка T_T", throwable);
            }

            @Override
            public void onComplete() {
                completed.incrementAndGet();
            }
        });

        assertEquals(List.of(1, 2, 3), items, "Observable.just должен передать все элементы");
        assertEquals(1, completed.get(), "Поток должен завершиться один раз");
    }

    private void testMap() {
        List<Integer> items = new ArrayList<>();

        Observable.just(1, 2, 3)
                .map(value -> value * 2)
                .subscribe(new CollectingObserver<>(items));

        assertEquals(List.of(2, 4, 6), items, "map должен преобразовать каждый элемент");
    }

    private void testFilter() {
        List<Integer> items = new ArrayList<>();

        Observable.just(1, 2, 3, 4, 5)
                .filter(value -> value % 2 == 0)
                .subscribe(new CollectingObserver<>(items));

        assertEquals(List.of(2, 4), items, "filter должен оставлять только подходящие элементы");
    }

    private void testFlatMap() {
        List<Integer> items = new CopyOnWriteArrayList<>();

        Observable.just(1, 2, 3)
                .flatMap(value -> Observable.just(value, value * 10))
                .subscribe(new CollectingObserver<>(items));

        assertEquals(List.of(1, 10, 2, 20, 3, 30), items, "flatMap должен объединять внутренние потоки");
    }

    private void testErrorHandling() {
        AtomicReference<String> errorMessage = new AtomicReference<>();

        Observable.just(1, 2, 3)
                .map(value -> {
                    if (value == 2) {
                        throw new IllegalStateException("ошибка");
                    }
                    return value;
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        errorMessage.set(throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertEquals("ошибка", errorMessage.get(), "Ошибка");
    }

    private void testDisposableStopsFlow() throws Exception {
        List<Integer> items = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        Disposable disposable = Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 10; i++) {
                        if (emitter.isDisposed()) {
                            break;
                        }
                        emitter.onNext(i);
                        Thread.sleep(30);
                    }
                    emitter.onComplete();
                })
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        items.add(item);
                        if (item == 3) {
                            disposableRef.get().dispose();
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throw new AssertionError("Ошибка T_T", throwable);
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        disposableRef.set(disposable);
        await(latch, "Тест не завершился вовремя");
        Thread.sleep(150);

        assertEquals(List.of(1, 2, 3), items, "Отмена, должна остановить дальнейшую отправку элементов");
    }

    private void testSubscribeOnUsesAnotherThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
                    threadName.set(Thread.currentThread().getName());
                    emitter.onNext(1);
                    emitter.onComplete();
                })
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throw new AssertionError("Ошибка T_T", throwable);
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        await(latch, "Тест subscribeOn не завершился вовремя");
        assertTrue(threadName.get() != null && threadName.get().startsWith("rx-io"),
                "subscribeOn должен запускать источник в потоке IO-планировщика");
    }

    private void testObserveOnUsesTargetScheduler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        Observable.just(1)
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        threadName.set(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throw new AssertionError("Ошибка T_T", throwable);
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        await(latch, "Тест observeOn не завершился вовремя");
        assertTrue(threadName.get() != null && threadName.get().startsWith("поток"),
                "observeOn должен передавать элементы в потоке указанного планировщика");
    }

    private void await(CountDownLatch latch, String message) throws Exception {
        if (!latch.await(3, TimeUnit.SECONDS)) {
            throw new AssertionError(message);
        }
    }

    private void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ". Ожидалось: " + expected + ", получено: " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class CollectingObserver<T> implements Observer<T> {
        private final List<T> items;

        private CollectingObserver(List<T> items) {
            this.items = items;
        }

        @Override
        public void onNext(T item) {
            items.add(item);
        }

        @Override
        public void onError(Throwable throwable) {
            throw new AssertionError("Ошибка T_T", throwable);
        }

        @Override
        public void onComplete() {
        }
    }
}
