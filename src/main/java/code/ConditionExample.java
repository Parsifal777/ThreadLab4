package code;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class ConditionExample {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition notEmpty = lock.newCondition();
    private static final Condition notFull = lock.newCondition();

    private static final Queue<Integer> buffer = new LinkedList<>();
    private static final int MAX_CAPACITY = 5;
    private static final Random random = new Random();
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Задание 3: Демонстрация Condition");
        System.out.println("Буфер максимальной емкости: " + MAX_CAPACITY);

        // Запуск производителей
        Thread producer1 = new Thread(new Producer(1), "Producer-1");
        Thread producer2 = new Thread(new Producer(2), "Producer-2");

        // Запуск потребителей
        Thread consumer1 = new Thread(new Consumer(1), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer(2), "Consumer-2");

        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();

        // Демонстрация преимущества Condition перед wait/notify
        demonstrateConditionAdvantages();

        Thread.sleep(15000);
        running = false;

        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();

        System.out.println("\nПрограмма завершена");
        System.out.println("Остаток в буфере: " + buffer.size() + " элементов");
    }

    static class Producer implements Runnable {
        private final int id;
        private int producedCount = 0;

        public Producer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (running) {
                lock.lock();
                try {
                    while (buffer.size() == MAX_CAPACITY && running) {
                        System.out.println("Производитель " + id + " ждет (буфер полон)");
                        notFull.await();
                    }

                    if (!running) break;

                    int item = random.nextInt(100);
                    buffer.offer(item);
                    producedCount++;

                    System.out.println("Производитель " + id + " создал: " + item +
                            " (размер буфера: " + buffer.size() +
                            ", всего произведено: " + producedCount + ")");

                    notEmpty.signalAll();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }

                try {
                    Thread.sleep(random.nextInt(800) + 200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static class Consumer implements Runnable {
        private final int id;
        private int consumedCount = 0;

        public Consumer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (running) {
                lock.lock();
                try {
                    while (buffer.isEmpty() && running) {
                        System.out.println("Потребитель " + id + " ждет (буфер пуст)");
                        notEmpty.await();
                    }

                    if (!running) break;
                    int item = buffer.poll();
                    consumedCount++;

                    System.out.println("Потребитель " + id + " забрал: " + item +
                            " (размер буфера: " + buffer.size() +
                            ", всего потреблено: " + consumedCount + ")");

                    notFull.signalAll();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }

                try {
                    Thread.sleep(random.nextInt(1000) + 100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void demonstrateConditionAdvantages() throws InterruptedException {
        System.out.println("\nДЕМОНСТРАЦИЯ ПРЕИМУЩЕСТВ CONDITION");

        // Создаем отдельный буфер для демонстрации
        Queue<Integer> demoBuffer = new LinkedList<>();

        lock.lock();
        try {
            System.out.println("\nПрактическая демонстрация");

            for (int i = 0; i < MAX_CAPACITY; i++) {
                buffer.offer(i);
            }
            System.out.println("Буфер заполнен: " + buffer.size() + " элементов");

            // Создаем поток, который будет ждать на условии notFull
            Thread waitingProducer = new Thread(() -> {
                lock.lock();
                try {
                    System.out.println("Производитель пытается добавить элемент...");
                    System.out.println("Производитель будет ждать на условии notFull");
                    notFull.await();
                    System.out.println("Производитель получил сигнал и продолжил работу");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            });

            waitingProducer.start();

            Thread.sleep(100);

            System.out.println("Потребитель забирает элемент...");
            buffer.poll();
            notFull.signal();

            System.out.println("(Потребители не получили сигнал, так как использовано notFull.signal())");

            waitingProducer.join();

        } finally {
            lock.unlock();
        }
    }
}
