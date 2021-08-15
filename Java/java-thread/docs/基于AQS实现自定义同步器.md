下面是基于AQS实现的不可重入的独占锁：

**state**为**1**表示锁已经被某一个线程持有，由于是不可重入锁，所以不需要记录持有锁的线程获取锁的次数。

```java
public class NonReentrantLock implements Lock, java.io.Serializable {

    private static class Sync extends AbstractQueuedSynchronizer {
        // 锁是否被持有
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // 如果state为0 则尝试获取锁
        public boolean tryAcquire(int acquires) {
            assert acquires == 1;
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 尝试释放锁，设置state为0
        protected boolean tryRelease(int releases) {
            assert releases == 1;
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        Condition newCondition(){
            return new ConditionObject();
        }
    }

    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }
}

```

下面是模拟生产者-消费者的代码：

```java
public class Producer {

    final static NonReentrantLock lock = new NonReentrantLock();
    final static Condition notFull = lock.newCondition();
    final static Condition notEmpty = lock.newCondition();

    final static Queue<String> queue = new LinkedBlockingDeque<>();
    final static int queuesize = 10;

    public static void main(String[] args) {
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    // 如果队列满了，则等待
                    while (queue.size() == queuesize) {
                        notEmpty.await();
                    }

                    queue.add("ele");

                    System.out.println("添加完毕" + Thread.currentThread().getName());

                    // 唤醒消费线程
                    notFull.signalAll();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        Thread comsumer = new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    // 队列为空，消费线程等待
                    while (queue.size() == 0) {
                        notFull.await();
                    }
                    String ele = queue.poll();

                    System.out.println("消费完毕：" + ele + "   线程是  " + Thread.currentThread().getName());

                    // 唤醒生产线程
                    notEmpty.signalAll();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        producer.start();
        comsumer.start();
    }
}
```

