package code;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockExample {
    private static final ReentrantLock lock = new ReentrantLock();
    private static int sharedCounter = 0;
    private static final int ITERATIONS = 100000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Задание 1: Демонстрация ReentrantLock");

        // Сравнение производительности
        comparePerformance();

        // Демонстрация правильного использования lock/unlock
        demonstrateLockUsage();

        // Демонстрация обработки исключений
        demonstrateExceptionHandling();
    }

    private static void comparePerformance() throws InterruptedException {
        System.out.println("\nСравнение производительности");

        // Тест с synchronized
        long syncTime = testSynchronized();
        System.out.println("Synchronized время: " + syncTime + " мс");

        // Тест с ReentrantLock
        long lockTime = testReentrantLock();
        System.out.println("ReentrantLock время: " + lockTime + " мс");

        System.out.println("Разница: " + Math.abs(syncTime - lockTime) + " мс");
    }

    private static long testSynchronized() throws InterruptedException {
        sharedCounter = 0;
        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    synchronized (ReentrantLockExample.class) {
                        sharedCounter++;
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return System.currentTimeMillis() - startTime;
    }

    private static long testReentrantLock() throws InterruptedException {
        sharedCounter = 0;
        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    lock.lock();
                    try {
                        sharedCounter++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return System.currentTimeMillis() - startTime;
    }

    private static void demonstrateLockUsage() {
        System.out.println("\nДемонстрация правильного использования lock/unlock");

        Thread thread1 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Поток 1 захватил блокировку");
                sharedCounter += 10;
                Thread.sleep(1000);
                System.out.println("Поток 1 завершил работу, counter = " + sharedCounter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("Поток 1 освободил блокировку");
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("Поток 2 пытается захватить блокировку");

                if (lock.tryLock()) {
                    try {
                        System.out.println("Поток 2 захватил блокировку");
                        sharedCounter += 5;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("Поток 2 не смог захватить блокировку сразу");

                    lock.lock();
                    try {
                        System.out.println("Поток 2 захватил блокировку после ожидания");
                        sharedCounter += 5;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Итоговое значение counter: " + sharedCounter);
    }

    private static void demonstrateExceptionHandling() {
        System.out.println("\nДемонстрация обработки исключений в блокировках");

        lock.lock();
        try {
            System.out.println("Блокировка захвачена");

            if (true) {
                throw new RuntimeException("Имитация исключения");
            }

            sharedCounter++;
        } catch (RuntimeException e) {
            System.out.println("Исключение перехвачено: " + e.getMessage());
        } finally {
            lock.unlock();
            System.out.println("Блокировка освобождена в finally блоке");
        }

        boolean isLocked = lock.tryLock();
        if (isLocked) {
            System.out.println("Блокировка успешно освобождена после исключения");
            lock.unlock();
        }
    }
}
