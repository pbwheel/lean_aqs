package fbl.lean.java.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MutexLockTest {
    @Test
    @DisplayName("多线程互斥锁测试")
    void testMutex() throws InterruptedException {
        MutexLock mutex = new MutexLock();
        int[] sum = {0};
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Adder adder = new Adder(mutex, sum);
            adder.start();
            threads.add(adder);
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    static class Adder extends Thread {
        private final MutexLock mutex;
        private final int[] n;

        Adder(MutexLock mutex, int[] n) {
            this.n = n;
            this.mutex = mutex;
        }

        public void run() {
            while (true) {
                add();
                try {
                    Thread.sleep(RandomUtils.nextInt(10, 100));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void add() {
            mutex.lock();
            try {
                n[0]++;
                log.info("get lock, {}", n[0]);
                Thread.sleep(RandomUtils.nextInt(10, 100));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutex.unlock();
            }
            log.info("end");
        }
    }

    public static class MutexLock {
        private final Sync sync = new Sync(true);

        public void lock() {
            sync.acquire(1);
        }

        public void unlock() {
            sync.release(1);
        }

        // Our internal helper class
        private static class Sync extends AQS {
            private final boolean fair;

            Sync(boolean fair) {
                this.fair = fair;
            }

            @Override
            protected boolean tryAcquire(int acquires) {
                if (fair && hasQueuedPredecessors()) {
                    log.info("hasQueuedPredecessors");
                    return false;
                }
                return compareAndSetState(0, 1);
            }

            @Override
            protected boolean tryRelease(int releases) {
                if (getState() == 0) {
                    throw new IllegalMonitorStateException();
                }

                setState(0);
                return true;
            }
        }
    }
}
