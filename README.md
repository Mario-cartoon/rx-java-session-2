# RxJava

Реализация RxJava.

## Структура проекта

- `src/main/java/com/example/rx` — реализация библиотеки и пример запуска.
- `src/test/java/com/example/rx` — тесты. 

## Что реализовано

- `Observer<T>`
- `Observable<T>`
- `Disposable`
- `Scheduler`
- `IOThreadScheduler`
- `ComputationScheduler`
- `SingleThreadScheduler`
- `map`
- `filter`
- `flatMap`
- `subscribeOn`
- `observeOn`
- обработка ошибок

## Сборка и запуск 

```bash
mkdir -p out
javac -d out $(find src/main/java src/test/java -name "*.java")
```

### Запуск примера

```bash
java -cp out com.example.rx.Main
```

### Запуск тестов

```bash
java -cp out com.example.rx.RxTests
```




# Отчет по проекту RxJava

## 1. Архитектура

Проект реализует подобие RxJava на основе паттерна Observer.

Основные компоненты:

- `Observer<T>` — получает элементы, ошибки и сигнал завершения.
- `Observable<T>` — источник данных и операторы для преобразования потока.
- `Emitter<T>` — объект, через который `Observable.create(...)` отправляет события подписчику.
- `Disposable` — позволяет отменить подписку.
- `Scheduler` — отвечает за выполнение задач в нужном потоке.

Логика построена так:

1. Создается `Observable` через `Observable.create(...)`.
2. Внутри `subscribe(...)` создается безопасный `SafeEmitter`, который не дает отправлять события после завершения или отмены.
3. Операторы (`map`, `filter`, `flatMap`, `subscribeOn`, `observeOn`) создают новый `Observable`, который оборачивает предыдущий.
4. Подписчик получает события через методы `onNext`, `onError`, `onComplete`.

## 2. Реализованные операторы

### `map`
Преобразует каждый элемент потока.

Пример:

```java
Observable.just(1, 2, 3)
    .map(x -> x * 10);
```

### `filter`
Пропускает только те элементы, которые удовлетворяют условию.

Пример:

```java
Observable.just(1, 2, 3, 4)
    .filter(x -> x % 2 == 0);
```

### `flatMap`
Для каждого элемента создает новый `Observable` и объединяет их в один поток.

Пример:

```java
Observable.just(1, 2)
    .flatMap(x -> Observable.just(x, x * 10));
```

## 3. Планировщики

В проекте реализованы три планировщика задач:

- `IOThreadScheduler` — использует `CachedThreadPool`, подходит для задач ввода-вывода.
- `ComputationScheduler` — использует `FixedThreadPool`, подходит для вычислений.
- `SingleThreadScheduler` — использует один поток, подходит для последовательной обработки.

### `subscribeOn(Scheduler scheduler)`
Определяет, в каком потоке будет запускаться источник данных.

### `observeOn(Scheduler scheduler)`
Определяет, в каком потоке будут обрабатываться события у подписчика.

## 4. Обработка ошибок

Ошибки могут появиться:

- внутри `create(...)`;
- внутри `map`, `filter`, `flatMap`;
- в пользовательской логике.

Во всех этих случаях ошибка передается в `onError(...)`, а поток завершается.

## 5. Управление подпиской

Интерфейс `Disposable` позволяет отменять подписку.
После вызова `dispose()` новые элементы больше не передаются подписчику.

## 6. Тестирование

В проекте есть файл `RxTests.java`, который проверяет:

- базовую подписку;
- работу `map`;
- работу `filter`;
- работу `flatMap`;
- передачу ошибок в `onError`;
- остановку потока через `Disposable`;
- работу `subscribeOn`;
- работу `observeOn`.

Тесты написаны без внешних библиотек, чтобы проект можно было проверить обычным `javac` и `java`.

## 7. Пример использования

В проекте показан полный пример использования библиотеки. Ниже собраны отдельные сценарии, которые удобно брать как шаблоны.

### 7.1. Простой поток из готовых значений

```java
Observable.just(1, 2, 3)
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Получено: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Ошибка: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Готово");
            }
        });
```

### 7.2. Цепочка операторов `map`, `filter`, `flatMap`

```java
Observable.just(1, 2, 3, 4, 5)
        .map(value -> value * 10)
        .filter(value -> value >= 30)
        .flatMap(value -> Observable.just(value, value + 1))
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Значение: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Ошибка: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Поток завершен");
            }
        });
```

### 7.3. Создание собственного источника через `create`

```java
Observable.<String>create(emitter -> {
            emitter.onNext("A");
            emitter.onNext("B");
            emitter.onNext("C");
            emitter.onComplete();
        })
        .subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                System.out.println("Элемент: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Ошибка: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Завершено");
            }
        });
```

### 7.4. Переключение потоков с `subscribeOn` и `observeOn`

```java
Scheduler io = new IOThreadScheduler();
Scheduler single = new SingleThreadScheduler();

Observable.just(1, 2, 3)
        .subscribeOn(io)
        .observeOn(single)
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println(Thread.currentThread().getName() + " -> " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Ошибка: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println(Thread.currentThread().getName() + " -> Готово");
            }
        });
```

### 7.5. Отмена подписки через `Disposable`

```java
Disposable disposable = Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 10; i++) {
                if (emitter.isDisposed()) {
                    return;
                }
                emitter.onNext(i);
            }
            emitter.onComplete();
        })
        .subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Получено: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Ошибка: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Завершено");
            }
        });

disposable.dispose();
``` 

## 8. Вывод

Реализация RxJava, которая показывает:

- как работает паттерн Observer;
- как строятся цепочки операторов;
- как переключаются потоки выполнения;
- как обрабатываются ошибки;
- как отменяется подписка.

## Реализация

```text
поток-1 -> Получено значение: 30
поток-1 -> Получено значение: 31
поток-1 -> Получено значение: 40
поток-1 -> Получено значение: 41
поток-1 -> Получено значение: 50
поток-1 -> Получено значение: 51
поток-1 -> Поток завершен
```
## Тестирование
Тестирование проводилось с помощью набора юнит‑тестов в файле `src/test/java/com/example/rx/RxTests.java.`
Проверяемые сценарии:

-Базовая подписка
Проверяется, что `Observable.just(...)` передает все элементы и корректно завершает поток один раз.

-Оператор `map`
Проверяется, что каждый элемент преобразуется по заданной функции.

-Оператор `filter`
Проверяется, что в результат попадают только элементы, удовлетворяющие условию.

-Оператор `flatMap`
Проверяется, что внутренние потоки объединяются, а все элементы передаются подписчику.

-Обработка ошибок
Проверяется, что исключение в операторе map передается в `onError`, а поток корректно завершается.

-Отмена подписки (`Disposable`)
Проверяется, что после вызова `dispose`() дальнейшие элементы не передаются.

-`subscribeOn`
Проверяется, что источник действительно запускается в потоке IO‑планировщика.

-`observeOn`
Проверяется, что обработка элементов происходит в потоке указанного планировщика.
