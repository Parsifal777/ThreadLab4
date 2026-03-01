package code;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ReadWriteLockExample {
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Map<Integer, String> sharedData = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Задание 2: Демонстрация ReadWriteLock");

        // Инициализация данных
        for (int i = 1; i <= 5; i++) {
            sharedData.put(i, "Initial Data " + i);
        }

        // Запуск читающих потоков
        for (int i = 1; i <= 5; i++) {
            Thread reader = new Thread(new Reader(i), "Reader-" + i);
            reader.start();
        }

        // Запуск пишущих потоков
        for (int i = 1; i <= 2; i++) {
            Thread writer = new Thread(new Writer(i), "Writer-" + i);
            writer.start();
        }

        // Запуск потока для демонстрации одновременного чтения
        Thread simultaneousReader = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(3000);
                    demonstrateSimultaneousReading();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        simultaneousReader.start();

        Thread.sleep(15000);
        running = false;

        System.out.println("\nПрограмма завершена");
    }

    static class Reader implements Runnable {
        private final int id;

        public Reader(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (running) {
                rwLock.readLock().lock();
                try {
                    System.out.println("Читатель " + id + " начал чтение в " +
                            System.currentTimeMillis() % 10000);

                    int key = random.nextInt(5) + 1;
                    String value = sharedData.get(key);

                    Thread.sleep(random.nextInt(500) + 100);

                    System.out.println("Читатель " + id + " прочитал: ключ=" + key +
                            ", значение='" + value + "' в " +
                            System.currentTimeMillis() % 10000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.println("Читатель " + id + " завершил чтение");
                    rwLock.readLock().unlock();
                }

                try {
                    Thread.sleep(random.nextInt(1000) + 500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static class Writer implements Runnable {
        private final int id;
        private int writeCount = 0;

        public Writer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(random.nextInt(3000) + 2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("\nПисатель " + id + " пытается получить доступ на запись");

                rwLock.writeLock().lock();
                try {
                    writeCount++;
                    System.out.println("Писатель " + id + " НАЧАЛ запись #" + writeCount +
                            " в " + System.currentTimeMillis() % 10000);

                    int key = random.nextInt(5) + 1;
                    String newValue = "Updated Data " + writeCount + " by Writer " + id;
                    sharedData.put(key, newValue);

                    Thread.sleep(1500);

                    System.out.println("Писатель " + id + " ЗАВЕРШИЛ запись #" + writeCount +
                            " (ключ=" + key + ") в " +
                            System.currentTimeMillis() % 10000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.println("Писатель " + id + " освободил блокировку записи\n");
                    rwLock.writeLock().unlock();
                }
            }
        }
    }

    private static void demonstrateSimultaneousReading() {
        System.out.println("\nДЕМОНСТРАЦИЯ ОДНОВРЕМЕННОГО ЧТЕНИЯ");

        Thread[] readers = new Thread[3];
        for (int i = 0; i < readers.length; i++) {
            final int readerId = i + 10;
            readers[i] = new Thread(() -> {
                rwLock.readLock().lock();
                try {
                    System.out.println("  Reader-" + readerId + " начал одновременное чтение");
                    Thread.sleep(1000);
                    System.out.println("  Reader-" + readerId + " завершил чтение");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    rwLock.readLock().unlock();
                }
            });
            readers[i].start();
        }

        for (Thread reader : readers) {
            try {
                reader.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Все читатели завершили одновременное чтение\n");
    }
}
