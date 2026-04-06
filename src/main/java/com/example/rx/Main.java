package com.example.rx;

/**
 * Подобие мини RxJava.
 */
public class Main {
    /**
     * Запускает демонстрацию операторов и планировщиков.
     */
    public static void main(String[] args) throws InterruptedException {
        Scheduler io = new IOThreadScheduler();
        Scheduler single = new SingleThreadScheduler();

        Observable<Integer> observable = Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 5; i++) {
                emitter.onNext(i);
                Thread.sleep(100);
            }
            emitter.onComplete();
        });

        observable
                .map(number -> number * 10)
                .filter(number -> number >= 30)
                .flatMap(number -> Observable.just(number, number + 1))
                .subscribeOn(io)
                .observeOn(single)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println(Thread.currentThread().getName() + " -> Получено значение: " + item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Ошибка: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println(Thread.currentThread().getName() + " -> Поток завершен");
                    }
                });

        Thread.sleep(1500);
    }
}
