package com.example.rx;

/**
 * Получатель событий реактивного потока.
 *
 * @param <T> тип элементов в потоке
 */
public interface Observer<T> {
    /**
     * Вызывается при получении очередного элемента.
     *
     * @param item элемент потока
     */
    void onNext(T item);

    /**
     * Вызывается при возникновении ошибки.
     *
     * @param throwable ошибка в потоке
     */
    void onError(Throwable throwable);

    /**
     * Вызывается после успешного завершения потока.
     */
    void onComplete();
}
