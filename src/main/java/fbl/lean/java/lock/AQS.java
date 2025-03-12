package fbl.lean.java.lock;

import fbl.lean.java.lock.util.UnsafeUtils;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class AQS extends AbstractOwnableSynchronizer {
    private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (AQS.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AQS.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AQS.class.getDeclaredField("tail"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private transient volatile Node head;
    private transient volatile Node tail;
    private volatile int state;

    protected boolean tryAcquire(int acquires) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int releases) {
        throw new UnsupportedOperationException();
    }

    public final void acquire(int acquires) {
        if (tryAcquire(acquires)) {
            log.info("tryAcquire success!");
            return;
        }
        printQueue("acquire");
        Node node = addWaiter();
        acquireQueued(node, acquires);
    }

    public final boolean release(int releases) {
        if (tryRelease(releases)) {
            Node h = head;
            if (h != null)
                unparkSuccessor(h);
            printQueue("release");
            return true;
        }
        return false;
    }

    private Node addWaiter() {
        Node node = new Node(Thread.currentThread());
        Node pred = tail;
        if (pred != null) {
            // 先快速操作一把，加到队列尾部
            if (appendToTail(node, pred)) {
                return node;
            }
        }
        // 尾部没有节点或者加入到队列尾部失败
        enq(node);
        return node;
    }

    private void enq(final Node node) {
        for (; ; ) {
            Node t = tail;
            if (t == null) {
                // 初始化头节点和尾节点
                if (compareAndSetHead(new Node())) {
                    tail = head;
                }
            } else {
                // 加到队列尾部
                if (appendToTail(node, t)) {
                    return;
                }
            }
        }
    }

    private boolean appendToTail(final Node node, Node tail) {
        node.prev = tail;
        if (compareAndSetTail(tail, node)) {
            tail.next = node;
            return true;
        }
        return false;
    }

    final void acquireQueued(final Node node, int acquires) {
        // 获取锁
        for (; ; ) {
            final Node p = node.predecessor();
            // 如果前驱是head，说明当前节点在第一个，去尝试加锁。
            if (p == head && tryAcquire(acquires)) {
                // 自己替换成新head
                setHead(node);
                // 旧的head释放前，需要将head的next设置为null
                p.next = null; // help GC
                return;
            }
            // 阻塞
            LockSupport.park(this);
        }
    }

    private void unparkSuccessor(Node node) {
        Node s = node.next;
        if (s == null) {
            // 可能enq中还没设置next
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev) {
                s = t;
            }
        }
        if (s != null) {
            log.info("unpark: {}", s.thread.getName());
            LockSupport.unpark(s.thread);
        }
    }

    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        this.state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(
            Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    public final boolean hasQueuedPredecessors() {
        // 获取队列的尾节点
        Node tailNode = tail;
        // 获取队列的头节点
        Node headNode = head;
        Node nextNode;
        // 判断队列是否存在等待的线程，并且队列中第一个等待的线程不是当前线程
        return headNode != tailNode &&
                ((nextNode = headNode.next) == null || nextNode.thread != Thread.currentThread());
    }

    // 打印链表队列内容
    private void printQueue(String action) {
        Node node = tail;
        List<String> msgs = new ArrayList<>();
        while (node != null) {
            msgs.add(node.thread == null ? "" : node.thread.getName());
            node = node.prev;
        }
        if (!msgs.isEmpty()) {
            Collections.reverse(msgs);
            log.info("{} head: {}\nqueue: {}", action, head, String.join(",", msgs));
        }
    }

    static final class Node {

        volatile Node prev;

        volatile Node next;

        volatile Thread thread;

        Node() {
        }

        Node(Thread thread) {
            this.thread = thread;
        }

        Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null) {
                throw new NullPointerException();
            } else {
                return p;
            }
        }

        @Override
        public String toString() {
            return "prev:" + (prev == null ? "null" : prev.thread) + ", next:" + (next == null ? "null" : next.thread) + ", thread:" + thread;
        }
    }
}
