package com.example.rx;

/**
 * Служебный объект для отправки событий из источника в подписчика.
 *
 * @param <T> тип элементов потока
 */
public interface Emitter<T> extends Disposable {
    /**
     * Привязывает внутреннюю подписку к текущему источнику,
     * чтобы ее можно было отменить через общий Disposable
     *
     * @param disposable внутренняя подписка
     */
    void setDisposable(Disposable disposable);

    /**
     * Передает очередной элемент подписчику.
     *
     * @param item элемент потока
     */
    void onNext(T item);

    /**
     * Завершает поток с ошибкой.
     *
     * @param throwable ошибка
     */
    void onError(Throwable throwable);

    /**
     * Успешно завершает поток.
     */
    void onComplete();
}
