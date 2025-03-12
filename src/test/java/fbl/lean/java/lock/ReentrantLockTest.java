package fbl.lean.java.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReentrantLockTest {
    @Test
    @DisplayName("可重入锁测试")
    void testReentrant() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        int[] sum = {0};
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Adder adder = new Adder(lock, sum);
            adder.start();
            threads.add(adder);
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    static class Adder extends Thread {
        private final ReentrantLock mutex;
        private final int[] n;

        Adder(ReentrantLock mutex, int[] n) {
            this.n = n;
            this.mutex = mutex;
        }

        public void run() {
            while (true) {
                try {
                    add();
                    Thread.sleep(RandomUtils.nextInt(10, 100));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void add() {
            mutex.lock();
            try {
                add1();
                add1();
            } finally {
                mutex.unlock();
            }
            log.info("end");
        }

        private void add1() {
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

    public static class ReentrantLock {
        private final Sync sync = new Sync();

        public void lock() {
            sync.acquire(1);
        }

        public void unlock() {
            sync.release(1);
        }

        // Our internal helper class
        private static class Sync extends AQS {

            @Override
            protected boolean tryAcquire(int acquires) {
                final Thread current = Thread.currentThread();
                int c = getState();
                if (c == 0) {
                    //!hasQueuedPredecessors() &&
                    if (compareAndSetState(0, acquires)) {
                        setExclusiveOwnerThread(current);
                        return true;
                    }
                } else if (current == getExclusiveOwnerThread()) {
                    int nextc = c + acquires;
                    if (nextc < 0)
                        throw new Error("Maximum lock count exceeded");
                    setState(nextc);
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int releases) {
                int c = getState() - releases;
                if (Thread.currentThread() != getExclusiveOwnerThread())
                    throw new IllegalMonitorStateException();
                boolean free = false;
                if (c == 0) {
                    free = true;
                    setExclusiveOwnerThread(null);
                }
                setState(c);
                return free;
            }
        }
    }
}
