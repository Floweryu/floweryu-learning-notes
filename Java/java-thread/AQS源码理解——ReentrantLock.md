## 公平锁策略

**ReentrantLock**类中 **FairSync** 类源码解读：

```java
static final class FairSync extends Sync {
    private static final long serialVersionUID = -3000897897090466540L;

    // 公平锁入口
    // 不响应中断的加锁
    final void lock() {
        acquire(1);		// （1）
    }

    /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         * 抢占成功：返回true, 包含重入锁
         * 抢占失败：返回false
         */
    protected final boolean tryAcquire(int acquires) {
        // 获取当前线程
        final Thread current = Thread.currentThread();
        // 获取AQS中当前status状态
        int c = getState();

        // 条件成立: c == 0 表示  当前AQS处于无锁的状态
        if (c == 0) {
            // 条件一: hasQueuedPredecessors()
            // 因为FairSync是公平锁，任何时候获取锁都需要检查一下队列中在当前需要获取锁的线程前有无等待者
            // hasQueuedPredecessors()方法返回true表示当前线程前面有等待者, 当前线程需要入队等待
            // hasQueuedPredecessors()方法返回false表示当前线程前面无等待者, 直接可以获取锁

            // 条件二: compareAndSetState(0, acquires) CAS设置state值, 由于是从条件一进入的此处, 队列中没有线程, CAS的预期值是0
            // 成功: 说明当前线程抢占锁成功
            // 失败: 说明存在竞争, 且当前线程竞争锁失败
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                // 如果当前线程前面没有等待线程, 并且成功获取到锁
                // 设置当前线程为独占锁的线程
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 执行到此处的情况：c != 0 或 c > 0
        // 这种情况需要判断当前线程是不是独占锁的线程, 因为ReentrantLock是可重入锁
        else if (current == getExclusiveOwnerThread()) {
            // 可重入锁的逻辑
            // nextc 是更新AQS的state的值
            int nextc = c + acquires;
            // 越界判断, 当重入的深度很深时, 会导致nextc<0, state的最大值是int, int值达到最大之后 再+1...变负数..
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            // 设置state的值
            setState(nextc);
            return true;
        }

        // 执行到这里的情况
        // 1. c==0时, CAS失败, CAS修改state时未抢过其他线程
        // 2. c!=0或c>0时, 当前线程不是独占锁线程
        return false;
    }
}

public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    // 条件一: h!=t 成立, 说明队列中有节点, 既然有节点, 为什么会出现条件二的判断呢？
    // 条件二: h.next == null, 在向空队列中新插入节点时, 新插入的节点的前驱与前置节点(这里就是tail)建立了关联，但在head.next还未与新节点连接之前, b会出现这种情况
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

点击上述(1)处代码进入**AbstractQueuedSynchronizer**源码中的**acquire方法**

```java
public final void acquire(int arg) {
    // 条件一: tryAcquire(arg) 该方法在ReentrantLock类中重写, 尝试获取锁, 获取成功后返回true, 获取失败返回false
    // 条件二: acquireQueued(addWaiter(Node.EXCLUSIVE), arg)
    // 2.1: addWaiter(Node.EXCLUSIVE) 将当前线程封装成node添加到队列中
    // 
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        //再次设置中断标记为true
        //此处为什么要selfInterrupt()?，原因是，如果acquireQueued()返回了true，说明线程是被interrupt醒的，不是被unpark醒的，
        //如果是被interrupt醒的，由于acquireQueued里面判断线程到底是怎么醒的，用的是Thread.interrupted()，该方法判断后，还会清除interrupt标记，因此这里要重新加上中断标记为。具体见https://www.zhihu.com/question/399039232
        selfInterrupt();
}

/**
 * Creates and enqueues node for current thread and given mode.
 *
 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
 * @return the new node
 * 返回当前线程形成的node
 */
private Node addWaiter(Node mode) {
    // 构建node, 将当前线程封装到node中
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    // 将节点入队
    // 获取队尾节点, 保存到pred变量中
    Node pred = tail;
    // 条件成立: 当队列中已经有node了
    if (pred != null) {
        // 当前节点的prev指向前置节点, 因为此时pred是tail
        node.prev = pred;
        // CAS 如果成功, 说明node入队成功
        if (compareAndSetTail(pred, node)) {
            // 前置节点指向当前节点, 完成双向绑定
            pred.next = node;
            return node;
        }
    }
    // 什么时候执行这里?
    // 1. 当前队列是空队列 tail = null
    // 2. CAS竞争失败
    enq(node);
    return node;
}

/**
 * Inserts node into queue, initializing if necessary. See picture above.
 * @param node the node to insert
 * @return node's predecessor
 */
private Node enq(final Node node) {
    // 自旋入队, 只有当前节点入队成功后才会跳出循环
    for (;;) {
        Node t = tail;
        // 1. 当前队列是空队列 tail = null
        // 说明当前锁被占用, 且当前线程可能是第一个获取锁失败的线程（当前时刻可能存在一批获取锁失败的线程...）
        if (t == null) { // Must initialize
            // 作为当前持锁线程的第一个后继线程, 需要做什么事
            // 1. 因为当前持锁的线程, 在它获取锁时, 直接tryAcquire成功了, 没有向阻塞队列中添加任何node, 所以作为后继第一个进入阻塞队列的线程,需要额外处理一些事情
            // 2. 在自己这个节点之前追加一个线程为空的node
            // CAS成功, 说明当前节点成为head.next节点
            if (compareAndSetHead(new Node()))
                tail = head;
            // 注意这里执行完后并没有跳出for循环....
        } else {
            // 普通的入队方式, 在队列不为null时进入, 由于for是无限循环, 所以可以一直入队成功
            // 要入队的节点前驱和tail绑定
            node.prev = t;
            // CAS设置tail指针
            if (compareAndSetTail(t, node)) {
                // 当前节点和前置节点的后驱绑定
                t.next = node;
                return t;
            }
        }
    }
}

/**
 * Acquires in exclusive uninterruptible mode for thread already in
 * queue. Used by condition wait methods as well as acquire.
 *
 * @param node the node 当前线程包装出来的node，且当前时刻已经入队成功了
 * @param arg the acquire argument  当前线程抢占资源成功后，设置state值时会用到
 * @return {@code true} if interrupted while waiting
 * true:当前线程抢占成功，普通情况下，当前线程早晚会拿到锁
 * false:表示失败，需要执行出队的逻辑
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        // 当前线程是否被中断
        boolean interrupted = false;
        for (;;) {  
            // 为什么会执行到这里
            // 1. 进行for循环时, 在线程尚未park前会执行
            // 2. 线程park之后, 被唤醒后, 也会执行到这里

            // 获取当前节点的前置节点
            final Node p = node.predecessor();

            // 条件一成立：p==head, 说明当前节点为head.next节点, head.next节点在任何时候都有权力去争夺锁
            // 条件二成立: tryAcquire(arg) 说明head对应的线程已经释放锁了, head.next节点对应的线程正好获取到锁
            // 条件二不成立：head对应的线程还没有释放锁.. head.next仍然需要park
            if (p == head && tryAcquire(arg)) {
                // 拿到锁之后需要做什么？
                // 设置自己为head节点
                setHead(node);

                // 将上个线程对应的node节点的next引用指向null, 使之出队
                p.next = null; // help GC
                // 当前线程获取锁的过程没有异常
                failed = false;
                // 返回当前线程的中断标记
                return interrupted;
            }

            //shouldParkAfterFailedAcquire这个方法是干嘛的？ 当前线程获取锁资源失败后，是否需要挂起呢？
            //返回值：true ->当前线程需要挂起  false-> 不需要
            //parkAndCheckInterrupt()作用：挂起当前线程，并且唤醒之后返回当前线程的中断标记
            //唤醒：1：正常唤醒 其他线程unpark 2:其他线程给当前挂起的线程一个中断信号
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                //interruped=true表示当前node对应的线程是被中断信号唤醒的
                interrupted = true;
        }
    } finally { // 什么时候会走到这？感觉不会走，应该是保险措施
        if (failed)
            cancelAcquire(node);
    }
}

/**
 * 总结：
 * 1：当前节点的前置节点是取消状态，第一次来到这个方法时会越过取消状态的节点，第二次会返回true，然后park当前线程
 * 2：当前节点的前置节点状态是0，当前线程会设置前置节点的状态为-1，第二次自旋来到这个方法时会返回true，然后park当前线程
 * @param pred 当前线程node的前置节点
 * @param node 当前线程对应node
 * @return {@code true} if thread should block
 * 返回值：boolean true 表示当前线程需要挂起
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    // 获取前置节点的状态
    // waitStatus: 0 默认状态 -1 Signal状态,表示当前节点释放锁之后会唤醒它的第一个后继节点 >0 表示当前节点是CANCELED状态
    int ws = pred.waitStatus;

    // 条件成立：表示前置节点是个可以唤醒当前节点的节点, 返回 true  ==>parkAndCheckInterrupt() park当前线程
    if (ws == Node.SIGNAL)
        /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
        return true;
    //条件成立：>0,表示前置节点是CANCELED状态
    if (ws > 0) {
        /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
        // 寻找节点的waitStatus大于0的前置节点, 这些节点是取消的状态
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        // 找到waitStatus不是大于0的节点后, 将waitStatus > 0 的节点剔除队列
        pred.next = node;
    } else {
        /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
        // 当前节点的前置节点是0或<0的情况
        // 将当前线程node的前置node状态强制设置为SIGNAL，表示前置节点释放锁之后需要唤醒我...
        // 说白了就是，当某个线程的node想排队的时候，它会把它的前置node的status设置为-1
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}

/**
 * Convenience method to park and then check if interrupted
 * 将当前线程挂起，唤醒后返回当前线程是否为"中断信号"唤醒
 * @return {@code true} if interrupted
 */
private final boolean parkAndCheckInterrupt() {
    // 将当前线程挂起
    LockSupport.park(this);
    // 唤醒后执行下面操作
    return Thread.interrupted();
}

/**
 * Cancels an ongoing attempt to acquire.
 * 取消指定node参与竞争
 * @param node the node
 */
private void cancelAcquire(Node node) {
    // Ignore if node doesn't exist
    if (node == null)
        return;

    // 因为已经取消排队了，所以node内部关联的当前线程置为null就好了
    node.thread = null;

    // Skip cancelled predecessors
    // 获取当前取消排队node的前驱
    Node pred = node.prev;
    // 跳过需要需要排队的节点
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    // predNext is the apparent node to unsplice. CASes below will
    // fail if not, in which case, we lost race vs another cancel
    // or signal, so no further action is necessary.
    // 拿到前驱的next节点, 有一下情况
    // 1.当前node
    Node predNext = pred.next;

    // Can use unconditional write instead of CAS here.
    // After this atomic step, other Nodes can skip past us.
    // Before, we are free of interference from other threads.
    // 将当前node状态设置为取消状态1
    node.waitStatus = Node.CANCELLED;

    // If we are the tail, remove ourselves.
    /**
         * 当前取消排队的node所在队列的位置不同，执行出队策略是不一样的，一共分为3种情况。
         * 1:当前node是队尾 tail -> node
         * 2:当前node不是head.next节点，也不是tail节点
         * 3:当前node是head.next节点。
         */

    // 第一种情况
    // 条件一: node=tail成立，当前node是队尾 tail -> node
    // 条件二:compareAndSetTail(node,pred)成功的话, 说明修改tail完成, 即将当前节点的前驱设置为tail
    if (node == tail && compareAndSetTail(node, pred)) {
        // 修改pred.next -> null,完成node出队
        compareAndSetNext(pred, predNext, null);
    } else {
        // If successor needs signal, try to set pred's next-link
        // so it will get one. Otherwise wake it up to propagate.
        // 保存节点状态
        int ws;

        // 第二种情况：当前node不是head.next节点，也不是tail
        // 条件一：pred!=head 说明当前node不是head.next节点，也不是tail节点
        // 条件二： ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL)))
        // 条件2.1 (ws = pred.waitStatus) == Node.SIGNAL 成立:说明node的前驱状态是Signal状态. 不成立：前驱状态可能是0，
        // 极端情况下：前驱也取消排队了
        // 条件2.2: (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))
        // 假设前驱状态 ws<=0 则设置前驱状态为Signal状态，表示要唤醒后继节点

        // if里面做的事情，就是让pred.next -> node.next, 所有需要保证pred节点状态为Signal状态
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {

            // 情况2：当前node不是head.next节点，也不是tail
            // 出队: pred.next -> node.next 节点后，当node.next节点被唤醒后，调用shouldParkAfterFailedAcquire会让node.next节点越过取消状态的节点，完成真正的出队
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
                compareAndSetNext(pred, predNext, next);
        } else {
            // 当前node是head.next节点
            // 类似情况2，后继节点唤醒后，会调用shouldParkAfterFailedAcquire会让node.next节点越过取消状态的节点
            // 队列的第三个节点会直接与head建立双重指向的关系，head.next->第三个node 中间就是被出队的head.next节点 第三个node.prev -> head
            unparkSuccessor(node);
        }

        node.next = node; // help GC
    }
}

/**
 * Wakes up node's successor, if one exists.
 *
 * @param node the node
 */
private void unparkSuccessor(Node node) {
    /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
    // 获取当前节点的状态
    int ws = node.waitStatus;
    if (ws < 0)
        //-1 Signal 改成0的原因：因为当前节点已经完成喊后续节点的任务了。
        compareAndSetWaitStatus(node, ws, 0);

    /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */

    // 条件一：
    // s什么时候等于null？
    // 1.当前节点就是tail节点时，s==null

    // 条件二： s.waitStatus>0 前提：s!=null
    // 成立：说明当前node节点的后续节点是取消状态... 需要找一个合适的可以被唤醒的节点。此处可以画图看看。
    /*
            head指向节点0，s指向节点1，t指向节点5。 则最终s指向节点3
             0       1      2       3       4      5
            node -> [1] -> [1] -> [-1] -> [-1] -> [0]
         */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        // 查找可以被唤醒的节点...
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
        // 上面循环，会找到一个离当前node最近的一个可以被唤醒的node，node可能找不到，node可能是null
    }
    // 如果找到合适的可以被唤醒的node，则唤醒..，找不到啥也不做
    if (s != null)
        LockSupport.unpark(s.thread);
}

```

**释放锁的源码**

```java
public final boolean release(int arg) {
    // 尝试释放锁，tryRelease返回true表示当前线程已经完全释放锁
    // 返回false，说明当前线程尚未完全释放锁
    if (tryRelease(arg)) {
        // head什么情况下会被创建出来？
        // 当持锁线程未释放线程时，且持锁期间有其他线程想要获取锁时，其他线程发现获取不了锁，而且队列是空队列，此时后续线程会为当前持锁中的线程构建出来一个head节点。后续线程会追加到head节点后面
        Node h = head;
        // 条件一成立：说明队列中head已经初始化过了，ReentrantLock在使用期间发生过多线程竞争了
        // 条件二成立：说明head后面一定插入过node节点
        if (h != null && h.waitStatus != 0)
            // 唤醒后继节点
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

