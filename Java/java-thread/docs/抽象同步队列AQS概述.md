# 1. 抽象同步队列AQS概述

> `AbstractQueuedSynchronizer`（简称 **AQS**）是**队列同步器**，顾名思义，其主要作用是处理同步。它是并发锁和很多同步工具类的实现基石（如 `ReentrantLock`、`ReentrantReadWriteLock`、`CountDownLatch`、`Semaphore`、`FutureTask` 等）。

## 1. AQS的数据结构

AQS 继承自 `AbstractOwnableSynchronize`，是一个FIFO的双向队列。

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** 等待队列的队头，懒加载。只能通过 setHead 方法修改。 */
    private transient volatile Node head;
    /** 等待队列的队尾，懒加载。只能通过 enq 方法添加新的等待节点。*/
    private transient volatile Node tail;
    /** 同步状态 */
    private volatile int state;
}
```

- `state` - AQS 使用一个整型的 `volatile` 变量来 **维护同步状态**。
  - 这个整数状态的意义由子类来赋予，如`ReentrantLock` 中该状态值表示所有者线程已经重复获取该锁的次数，`Semaphore` 中该状态值表示剩余的许可数量。
- `head` 和 `tail` - AQS **维护了一个 `Node` 类型（AQS 的内部类）的双链表来完成同步状态的管理**。这个双链表是一个双向的 FIFO 队列，通过 `head` 和 `tail` 指针进行访问。当 **有线程获取锁失败后，就被添加到队列末尾**。

再来看一下 `Node` 的源码

```java
static final class Node {
    /** 用来标记线程是获取共享资源时被阻塞挂起后放入AQS队列的 */
    static final Node SHARED = new Node();
    /** 用来标记线程是获取独占资源时被挂起后放入AQS队列的 */
    static final Node EXCLUSIVE = null;

    /** 线程等待状态，状态值有: 0、1、-1、-2、-3 */
    volatile int waitStatus;
    static final int CANCELLED =  1;	// 线程被取消
    static final int SIGNAL    = -1;	// 线程需要被唤醒
    static final int CONDITION = -2;	// 线程在条件队列里面等待
    static final int PROPAGATE = -3;	// 释放共享资源时需要通知其它节点

    /** 前驱节点 */
    volatile Node prev;
    /** 后继节点 */
    volatile Node next;
    /** 等待锁的线程 */
    volatile Thread thread;

  	/** 和节点是否共享有关 */
    Node nextWaiter;
}
```

很显然，Node 是一个双链表结构。

- `waitStatus` - `Node` 使用一个整型的 `volatile` 变量来 维护 AQS 同步队列中线程节点的状态。`waitStatus` 有五个状态值：
  - `CANCELLED(1)` - 此状态表示：该节点的线程可能由于超时或被中断而 **处于被取消(作废)状态**，一旦处于这个状态，表示这个节点应该从等待队列中移除。
  - `SIGNAL(-1)` - 此状态表示：**后继节点会被挂起**，因此在当前节点释放锁或被取消之后，必须唤醒(`unparking`)其后继结点。
  - `CONDITION(-2)` - 此状态表示：该节点的线程 **处于等待条件状态**，不会被当作是同步队列上的节点，直到被唤醒(`signal`)，设置其值为 0，再重新进入阻塞状态。
  - `PROPAGATE(-3)` - 此状态表示：下一个 `acquireShared` 应无条件传播。
  - `0` - 非以上状态。

对于`ReentrantLock`来说，`state`可以表示当前线程获取锁的可重入次数；对于读写锁`ReentrantReadWriteLock`来说，`state`的高16位表示读状态，也就是获取该读锁的次数，低16位表示获取到写锁的线程的可重入次数；对于`semaphore`来说，`state`表示当前可用信号的个数；对于`CountDownlatch`来说，`state`用来表示计数器当前的值。

AQS有个内部类`ConditionObject`类，用来结合锁实现线程同步。`ConditionObject`可以直接访问AQS对象内部的变量，比如`state`状态值和AQS队列。`ConditionObject`是条件变量，每个条件变量对应一个条件队列（单向链表队列），其用来存放调用条件变量的`await`方法后被阻塞的线程。

## 1.2 独享方式和共享方式

**AQS 提供了对独享锁与共享锁的支持**。

### 独享锁 API

获取、释放独享锁的主要 API 如下：

```java
public final void acquire(int arg)
public final void acquireInterruptibly(int arg)
public final boolean tryAcquireNanos(int arg, long nanosTimeout)
public final boolean release(int arg)
```

- `acquire` - 获取独占锁。
- `acquireInterruptibly` - 获取可中断的独占锁。
- `tryAcquireNanos` - 尝试在指定时间内获取可中断的独占锁。在以下三种情况下回返回：
  - 在超时时间内，当前线程成功获取了锁；
  - 当前线程在超时时间内被中断；
  - 超时时间结束，仍未获得锁返回 false。
- `release` - 释放独占锁。

### 共享锁 API

获取、释放共享锁的主要 API 如下：

```java
public final void acquireShared(int arg)
public final void acquireSharedInterruptibly(int arg)
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
public final boolean releaseShared(int arg)
```

- `acquireShared` - 获取共享锁。
- `acquireSharedInterruptibly` - 获取可中断的共享锁。
- `tryAcquireSharedNanos` - 尝试在指定时间内获取可中断的共享锁。
- `release` - 释放共享锁

## 1.3. AQS 的原理

> ASQ 原理要点：
>
> - AQS 使用一个整型的 `volatile` 变量来 **维护同步状态**。状态的意义由子类赋予。
> - AQS 维护了一个 FIFO 的双链表，用来存储获取锁失败的线程。
>
> AQS 围绕同步状态提供两种基本操作“获取”和“释放”，并提供一系列判断和处理方法，简单说几点：
>
> - state 是独占的，还是共享的；
> - state 被获取后，其他线程需要等待；
> - state 被释放后，唤醒等待线程；
> - 线程等不及时，如何退出等待。
>
> 至于线程是否可以获得 state，如何释放 state，就不是 AQS 关心的了，要由子类具体实现。

### 独占锁的获取与释放

AQS类没有提供可用的`tryAcquire`和`tryRelease`方法, 它们需要由具体的子类来实现. 子类在实现`tryAcquire`和`tryRelease`时要根据具体场景使用CAS算法尝试修改`state`状态值. 子类还需要定义在调用`acquire`和`release`方法时`state`状态值的增减代表什么含义.

#### 获取独占锁

当一个线程调用`acquire(int arg)`方法获取独占资源时, 会首先使用`tryAcquire`方法尝试获取资源, 具体是设置状态变量`state`的值, 成功则直接返回, 失败则当前线程封装为类型为`Node.EXCLUSIVVE`的`Node`节点后插入到`AQS`阻塞队列的尾部, 并调用`LockSupport.park(this)`方法挂起自己.

```java
public final void acquire(long arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

#### 释放独占锁

当一个线程调用`release(int arg)`方法时会尝试用`tryRelease`操作释放资源, 这里是设置状态变量`state`的值, 然后调用`LockSupport.unpark(Thread)`方法激活AQS队列里面被阻塞的一个线程.被激活的线程则使用`tryAcquire`尝试, 看当前状态变量`state`的值是否能满足自己的需要, 满足则该线程被激活, 否则放入AQS队列挂起.

```java
public final boolean release(long arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

#### 获取可中断的独占锁

AQS 中使用 `acquireInterruptibly(int arg)` 方法获取可中断的独占锁。

`acquireInterruptibly(int arg)` 实现方式**相较于获取独占锁方法（ `acquire`）非常相似**，区别仅在于它会**通过 `Thread.interrupted` 检测当前线程是否被中断**，如果是，则立即抛出中断（`InterruptedException`）。

#### 获取超时等待式的独占锁

AQS 中使用 `tryAcquireNanos(int arg)` 方法获取超时等待的独占锁。

`doAcquireNanos` 的实现方式 **相较于获取独占锁方法（ `acquire`）非常相似**，区别在于它会根据超时时间和当前时间计算出截止时间。在获取锁的流程中，会不断判断是否超时，如果超时，直接返回 false；如果没超时，则用 `LockSupport.parkNanos` 来阻塞当前线程

### 共享锁的获取与释放

AQS类没有提供可用的`tryAcquireShared`和`tryReleaseShared`方法, 它们需要由具体的子类来实现. 子类在实现`tryAcquire`和`tryRelease`时要根据具体场景使用CAS算法尝试修改`state`状态值.

#### 获取共享锁

当线程调用`acquireShared(int arg)`获取共享资源时, 会首先使用`tryAcquireShared`尝试获取资源, 具体是设置状态变量`state`的值, 成功则直接返回, 失败则将当前线程封装为类型`Node.SHARED`的`Node`节点后插入到AQS阻塞队列尾部, 并调用`LockSupport.park(this)`方法挂起自己.

```JAVA
public final void acquireShared(long arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

#### 释放共享锁

当一个线程调用`releaseShared(int arg)`时会尝试使用`tryReleaseShared`操作释放资源, 这里是设置状态变量`state`的值, 然后使用`LockSupport.unpark(thread)`激活AQS队列里面的一个线程.被激活的线程则使用`tryReleaseShared`查看当前状态变量`state`的值是否满足自己的需要, 满足则该线程被激活,  否则放入AQS队列挂起.

```java
public final boolean releaseShared(long arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

#### 获取可中断的共享锁

AQS 中使用 `acquireSharedInterruptibly(int arg)` 方法获取可中断的共享锁。

`acquireSharedInterruptibly` 方法与 `acquireInterruptibly` 几乎一致，不再赘述。

#### 获取超时等待式的共享锁

AQS 中使用 `tryAcquireSharedNanos(int arg)` 方法获取超时等待式的共享锁。

`tryAcquireSharedNanos` 方法与 `tryAcquireNanos` 几乎一致，不再赘述。

## 1.4 AQS如何操作队列

**入队操作**：当一个线程获取锁失败后该线程会被转换为Node节点，然后就会使用`enq(final Node node)`方法将该节点插入到AQS队列。

```java
/**
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor
     */
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

