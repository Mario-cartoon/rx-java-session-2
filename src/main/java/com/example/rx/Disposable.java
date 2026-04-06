package com.example.rx;

/**
 * Представляет подписку, которую можно отменить.
 */
public interface Disposable {
    /**
     * Отменяет подписку и останавливает дальнейшую передачу событий.
     */
    void dispose();

    /**
     * Показывает, была ли подписка уже отменена.
     *
     * @return {@code true}, если подписка отменена
     */
    boolean isDisposed();
}
