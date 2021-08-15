# 1. 并发锁简介

## 1.1 乐观锁与悲观锁

乐观锁与悲观锁不是指具体的什么类型的锁，而是**处理并发同步的策略**。

- **乐观锁** - 乐观锁对于并发采取乐观的态度，认为：**不加锁的并发操作也没什么问题。对于同一个数据的并发操作，是不会发生修改的**。在更新数据的时候，会采用不断尝试更新的方式更新数据。**乐观锁适合读多写少的场景**。
- **悲观锁** - 悲观锁对于并发采取悲观的态度，认为：**不加锁的并发操作一定会出问题**。**悲观锁适合写操作频繁的场景**。

悲观锁与乐观锁在 Java 中的典型实现：

- 悲观锁在 Java 中的应用就是通过使用 `synchronized` 和 `Lock` 显示加锁来进行互斥同步，这是一种阻塞同步。
- 乐观锁在 Java 中的应用就是采用 `CAS` 机制（`CAS` 操作通过 `Unsafe` 类提供，但这个类不直接暴露为 API，所以都是间接使用，如各种原子类）或版本号机制（也可以根据实际情况选用其他能够标记数据版本的字段，如时间戳等。）。

**版本号机制**：版本号机制的基本思路是在数据中增加一个字段version，表示该数据的版本号，每当数据被修改，版本号加1。当某个线程查询数据时，将该数据的版本号一起查出来；当该线程更新数据时，判断当前版本号与之前读取的版本号是否一致，如果一致才进行操作。

考虑这样一种场景：游戏系统需要更新玩家的金币数，更新后的金币数依赖于当前状态(如金币数、等级等)，因此更新前需要先查询玩家当前状态。

下面的实现方式，没有进行任何线程安全方面的保护。如果有其他线程在query和update之间更新了玩家的信息，会导致玩家金币数的不准确。

```java
@Transactional
public void updateCoins(Integer playerId){
    //根据player_id查询玩家信息
    Player player = query("select coins, level from player where player_id = {0}", playerId);
    //根据玩家当前信息及其他信息，计算新的金币数
    Long newCoins = ……;
    //更新金币数
    update("update player set coins = {0} where player_id = {1}", newCoins, playerId);
}
```

为了避免这个问题，悲观锁通过加锁解决这个问题，代码如下所示。在查询玩家信息时，使用select …… for update进行查询；该查询语句会为该玩家数据加上排它锁，直到事务提交或回滚时才会释放排它锁；在此期间，如果其他线程试图更新该玩家信息或者执行select for update，会被阻塞。

```java
@Transactional
public void updateCoins(Integer playerId){
    //根据player_id查询玩家信息（加排它锁）
    Player player = queryForUpdate("select coins, level from player where player_id = {0} for update", playerId);
    //根据玩家当前信息及其他信息，计算新的金币数
    Long newCoins = ……;
    //更新金币数
    update("update player set coins = {0} where player_id = {1}", newCoins, playerId);
}
```

版本号机制则是另一种思路，它为玩家信息增加一个字段：version。在初次查询玩家信息时，同时查询出version信息；在执行update操作时，校验version是否发生了变化，如果version变化，则不进行更新。

```java
@Transactional
public void updateCoins(Integer playerId){
    //根据player_id查询玩家信息，包含version信息
    Player player = query("select coins, level, version from player where player_id = {0}", playerId);
    //根据玩家当前信息及其他信息，计算新的金币数
    Long newCoins = ……;
    //更新金币数，条件中增加对version的校验
    update("update player set coins = {0}, version = version + 1 where player_id = {1} and version = {2}", newCoins, playerId, player.version);
}
```

## 1.2 公平锁与非公平锁

- **公平锁** - 公平锁是指 **多线程按照申请锁的顺序来获取锁**。
- **非公平锁** - 非公平锁是指 **多线程不按照申请锁的顺序来获取锁** 。这就可能会出现优先级反转（后来者居上）或者饥饿现象（某线程总是抢不过别的线程，导致始终无法执行）。

公平锁为了保证线程申请顺序，势必要付出一定的性能代价，因此其吞吐量一般低于非公平锁。

公平锁与非公平锁 在 Java 中的典型实现：

- **`synchronized` 只支持非公平锁**。
- **`ReentrantLock` 、`ReentrantReadWriteLock`，默认是非公平锁，但支持公平锁**。

`ReentrantLock`提供了公平和非公平锁的实现：

- 公平锁：`ReentrantLock pairLock = new ReentrantLock(true);`
- 非公平锁：`ReentrantLock pairLock = new ReentrantLock(false);`若构造函数不传参数，默认是非公平锁。

```java
public ReentrantLock() {
    sync = new NonfairSync();
}

/**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

## 1.3 独享锁与共享锁

独享锁与共享锁是一种广义上的说法，从实际用途上来看，也常被称为互斥锁与读写锁。

- **独享锁** - 独享锁是指 **锁一次只能被一个线程所持有**。
- **共享锁** - 共享锁是指 **锁可被多个线程所持有**。

独享锁是一种悲观锁，由于每次访问数据都先加上互斥锁，限制了并发性，因为读操作并不会影响数据的一直性。

共享锁是一种乐观锁，放款了加锁条件，允许多个线程同时进行读操作。

独享锁与共享锁在 Java 中的典型实现：

- **`synchronized` 、`ReentrantLock` 只支持独享锁**。
- **`ReentrantReadWriteLock` 其写锁是独享锁，其读锁是共享锁**。读锁是共享锁使得并发读是非常高效的，读写，写读 ，写写的过程是互斥的。

## 1.4 可重入锁

**可重入锁，顾名思义，指的是线程可以重复获取同一把锁**。即同一个线程在外层方法获取了锁，在进入内层方法会自动获取锁。

可重入锁的**原理**是在内部维护一个线程标示，用来标示该锁目前被哪个线程占用，然后关联一个计数器。一开始计数器值为0，说明该锁没有被任何线程占用。当一个线程获取了该锁时，计数器的值会变成1，这时其它线程再来获取该锁时会发现锁的所有者不是自己而被阻塞挂起。

但是当获取了该锁的线程再次获取锁时发现拥有者是自己，就会把计数器值加1，当释放锁后计数器值减1。当计数器值为0时，锁里面的线程标示被重置为null，这时候被阻塞的线程会被唤醒来竞争获取该锁。

**可重入锁可以在一定程度上避免死锁**。

- **`ReentrantLock` 、`ReentrantReadWriteLock` 是可重入锁**。这点，从其命名也不难看出。
- **`synchronized` 也是一个可重入锁**。

【示例】`synchronized` 的可重入示例

```java
synchronized void setA() throws Exception{
    Thread.sleep(1000);
    setB();
}

synchronized void setB() throws Exception{
    Thread.sleep(1000);
}
```

上面的代码就是一个典型场景：如果使用的锁不是可重入锁的话，`setB` 可能不会被当前线程执行，从而造成死锁。

【示例】`ReentrantLock` 的可重入示例

```java
class Task {

    private int value;
    private final Lock lock = new ReentrantLock();

    public Task() {
        this.value = 0;
    }

    public int get() {
        // 获取锁
        lock.lock();
        try {
            return value;
        } finally {
            // 保证锁能释放
            lock.unlock();
        }
    }

    public void addOne() {
        // 获取锁
        lock.lock();
        try {
            // 注意：此处已经成功获取锁，进入 get 方法后，又尝试获取锁，
            // 如果锁不是可重入的，会导致死锁
            value = 1 + get();
        } finally {
            // 保证锁能释放
            lock.unlock();
        }
    }

}
```

## 1.5 自旋锁

**自旋锁则是**，当前线程再获取锁时，如果发现已经被其它线程占有，它不马上阻塞自己，在不放弃CPU使用权的情况下，多次尝试获取（默认次数是10.可以使用–XX:PreBlockSpinsh参数设置该值），很有可能在后面几次尝试中其它线程已经释放了锁。如果尝试指定次数后仍没有获取到锁则当前线程才会被阻塞挂起。

## 1.6  偏向锁、轻量级锁、重量级锁

所谓轻量级锁与重量级锁，指的是锁控制粒度的粗细。显然，控制粒度越细，阻塞开销越小，并发性也就越高。

Java 1.6 以前，重量级锁一般指的是 `synchronized` ，而轻量级锁指的是 `volatile`。

Java 1.6 以后，针对 `synchronized` 做了大量优化，引入 4 种锁状态： **无锁状态**、**偏向锁**、**轻量级锁**和**重量级锁**。**锁可以单向的从偏向锁升级到轻量级锁，再从轻量级锁升级到重量级锁** 。

- **偏向锁** - 偏向锁是指一段同步代码一直被一个线程所访问，那么该线程会自动获取锁。降低获取锁的代价。

- **轻量级锁** - 是指当锁是偏向锁的时候，被另一个线程所访问，偏向锁就会升级为轻量级锁，其他线程会通过自旋的形式尝试获取锁，不会阻塞，提高性能。（**也叫自旋锁）执行时间短、线程多用自旋锁，因为自旋锁占用CPU时间长。执行时间长用系统锁，也就是重量级锁。**
- **重量级锁** - 是指当锁为轻量级锁的时候，另一个线程虽然是自旋，但自旋不会一直持续下去，当自旋一定次数的时候，还没有获取到锁，就会进入阻塞，该锁膨胀为重量级锁。重量级锁会让其他申请的线程进入阻塞，性能降低。

## 1.7 分段锁

分段锁其实是一种锁的设计，并不是具体的一种锁。所谓分段锁，就是把锁的对象分成多段，每段独立控制，使得锁粒度更细，减少阻塞开销，从而提高并发性。

`Hashtable` 使用 `synchronized` 修饰方法来保证线程安全性，那么面对线程的访问，Hashtable 就会锁住整个对象，所有的其它线程只能等待，这种阻塞方式的吞吐量显然很低。

Java 1.7 以前的 `ConcurrentHashMap` 就是分段锁的典型案例。`ConcurrentHashMap` 维护了一个 `Segment` 数组，一般称为分段桶。

```java
final Segment<K,V>[] segments;
```

当有线程访问 `ConcurrentHashMap` 的数据时，`ConcurrentHashMap` 会先根据 hashCode 计算出数据在哪个桶（即哪个 Segment），然后锁住这个 `Segment`。

## 1.8 显示锁和内置锁

Java 1.5 之前，协调对共享对象的访问时可以使用的机制只有 `synchronized` 和 `volatile`。这两个都属于内置锁，即锁的申请和释放都是由 JVM 所控制。

Java 1.5 之后，增加了新的机制：`ReentrantLock`、`ReentrantReadWriteLock` ，这类锁的申请和释放都可以由程序所控制，所以常被称为显示锁。

> 💡 `synchronized` 的用法和原理可以参考：[Java 并发基础机制 - synchronized (opens new window)](https://github.com/dunwu/javacore/blob/master/docs/concurrent/Java并发核心机制.md#二synchronized)。
>
> 🔔 注意：如果不需要 `ReentrantLock`、`ReentrantReadWriteLock` 所提供的高级同步特性，**应该优先考虑使用 `synchronized`** 。理由如下：
>
> - Java 1.6 以后，`synchronized` 做了大量的优化，其性能已经与 `ReentrantLock`、`ReentrantReadWriteLock` 基本上持平。
> - 从趋势来看，Java 未来更可能会优化 `synchronized` ，而不是 `ReentrantLock`、`ReentrantReadWriteLock` ，因为 `synchronized` 是 JVM 内置属性，它能执行一些优化。
> - `ReentrantLock`、`ReentrantReadWriteLock` 申请和释放锁都是由程序控制，如果使用不当，可能造成死锁，这是很危险的。

以下对比一下显示锁和内置锁的差异：

- 主动获取锁和释放锁
  - `synchronized` 不能主动获取锁和释放锁。获取锁和释放锁都是 JVM 控制的。
  - `ReentrantLock` 可以主动获取锁和释放锁。（如果忘记释放锁，就可能产生死锁）。
- 响应中断
  - `synchronized` 不能响应中断。
  - `ReentrantLock` 可以响应中断。
- 超时机制
  - `synchronized` 没有超时机制。
  - `ReentrantLock` 有超时机制。`ReentrantLock` 可以设置超时时间，超时后自动释放锁，避免一直等待。
- 支持公平锁
  - `synchronized` 只支持非公平锁。
  - `ReentrantLock` 支持非公平锁和公平锁。
- 是否支持共享
  - 被 `synchronized` 修饰的方法或代码块，只能被一个线程访问（独享）。如果这个线程被阻塞，其他线程也只能等待
  - `ReentrantLock` 可以基于 `Condition` 灵活的控制同步条件。
- 是否支持读写分离
  - `synchronized` 不支持读写锁分离；
  - `ReentrantReadWriteLock` 支持读写锁，从而使阻塞读写的操作分开，有效提高并发性。

# 2. Lock 和 Condition

## 2.1. 为何引入 Lock 和 Condition

**Java SDK 并发包通过 Lock 和 Condition 两个接口来实现管程，其中 Lock 用于解决互斥问题，Condition 用于解决同步问题**

synchronized 无法通过**破坏不可抢占条件**来避免死锁。原因是 synchronized 申请资源的时候，如果申请不到，线程直接进入阻塞状态了，而线程进入阻塞状态，啥都干不了，也释放不了线程已经占有的资源。

与内置锁 `synchronized` 不同的是，**`Lock` 提供了一组无条件的、可轮询的、定时的以及可中断的锁操作**，所有获取锁、释放锁的操作都是显式的操作。

- **能够响应中断**。synchronized 的问题是，持有锁 A 后，如果尝试获取锁 B 失败，那么线程就进入阻塞状态，一旦发生死锁，就没有任何机会来唤醒阻塞的线程。但如果阻塞状态的线程能够响应中断信号，也就是说当我们给阻塞的线程发送中断信号的时候，能够唤醒它，那它就有机会释放曾经持有的锁 A。这样就破坏了不可抢占条件了。
- **支持超时**。如果线程在一段时间之内没有获取到锁，不是进入阻塞状态，而是返回一个错误，那这个线程也有机会释放曾经持有的锁。这样也能破坏不可抢占条件。
- **非阻塞地获取锁**。如果尝试获取锁失败，并不进入阻塞状态，而是直接返回，那这个线程也有机会释放曾经持有的锁。这样也能破坏不可抢占条件。

## 2.2. Lock 接口

`Lock` 的接口定义如下：

```java
public interface Lock {
    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();
    Condition newCondition();
}
```

- `lock()` - 获取锁。
- `unlock()` - 释放锁。
- `tryLock()` - 尝试获取锁，仅在调用时锁未被另一个线程持有的情况下，才获取该锁。
- `tryLock(long time, TimeUnit unit)` - 和 `tryLock()` 类似，区别仅在于限定时间，如果限定时间内未获取到锁，视为失败。
- `lockInterruptibly()` - 锁未被另一个线程持有，且线程没有被中断的情况下，才能获取锁。
- `newCondition()` - 返回一个绑定到 `Lock` 对象上的 `Condition` 实例。

## 2.3. Condition

**Condition 实现了管程模型里面的条件变量**。

前面中提过 `Lock` 接口中 有一个 `newCondition()` 方法用于返回一个绑定到 `Lock` 对象上的 `Condition` 实例。

在单线程中，一段代码的执行可能依赖于某个状态，如果不满足状态条件，代码就不会被执行（典型的场景，如：`if ... else ...`）。在并发环境中，当一个线程判断某个状态条件时，其状态可能是由于其他线程的操作而改变，这时就需要有一定的协调机制来确保在同一时刻，数据只能被一个线程锁修改，且修改的数据状态被所有线程所感知。

Java 1.5 之前，主要是利用 `Object` 类中的 `wait`、`notify`、`notifyAll` 配合 `synchronized` 来进行线程间通信（如果不了解其特性，可以参考：[wait、notify/notifyAll](https://floweryu.blog.csdn.net/article/details/110222425)）

`wait`、`notify`、`notifyAll` 需要配合 `synchronized` 使用，不适用于 `Lock`。而使用 `Lock` 的线程，彼此间通信应该使用 `Condition` 。这可以理解为，什么样的锁配什么样的钥匙。**内置锁（`synchronized`）配合内置条件队列（`wait`、`notify`、`notifyAll` ），显式锁（`Lock`）配合显式条件队列（`Condition` ）**。

### Condition 的特性

`Condition` 接口定义如下：

```java
public interface Condition {
    void await() throws InterruptedException;
    void awaitUninterruptibly();
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    boolean awaitUntil(Date deadline) throws InterruptedException;
    void signal();
    void signalAll();
}
```

其中，`await`、`signal`、`signalAll` 与 `wait`、`notify`、`notifyAll` 相对应，功能也相似。除此以外，`Condition` 相比内置条件队列（ `wait`、`notify`、`notifyAll` ），提供了更为丰富的功能：

- 每个锁（`Lock`）上可以存在多个 `Condition`，这意味着锁的状态条件可以有多个。
- 支持公平的或非公平的队列操作。
- 支持可中断的条件等待，相关方法：`awaitUninterruptibly()` 。
- 支持可定时的等待，相关方法：`awaitNanos(long)` 、`await(long, TimeUnit)`、`awaitUntil(Date)`。

# 3. `ReentrantLock`

`ReentrantLock` 类是 `Lock` 接口的具体实现，与内置锁 `synchronized` 相同的是，它是一个**可重入的独占锁**。

类图结构:

![image-20210301174230467](https://i.loli.net/2021/03/01/VrZvcSt69GQOa1d.png)

## 3.1. `ReentrantLock` 的特性

`ReentrantLock` 的特性如下：

- **`ReentrantLock` 提供了与 `synchronized` 相同的互斥性、内存可见性和可重入性**。
- `ReentrantLock` **支持公平锁和非公平锁**（默认）两种模式。
- `ReentrantLock`实现了`Lock`接口，支持了`synchronized`所不具备的灵活性。
  - `synchronized` 无法中断一个正在等待获取锁的线程
  - `synchronized` 无法在请求获取一个锁时无休止地等待

## 3.2. `ReentrantLock` 的用法及原理

### `ReentrantLock` 的构造方法

`ReentrantLock` 有两个构造方法：

```java
public ReentrantLock() {
	sync = new NonfairSync();
}

public ReentrantLock(boolean fair) {
	sync = fair ? new FairSync() : new NonfairSync();
}
```

- `ReentrantLock()` - 默认构造方法会初始化一个**非公平锁（NonfairSync）**；
- `ReentrantLock(boolean)` - `new ReentrantLock(true)` 会初始化一个**公平锁（FairSync）**。

### `void lock()` 方法

- `lock()` - **无条件获取锁**。如果锁当前没有被其它线程占用并且当前线程之前没有获取过锁，则当前线程会获取到锁，然后设置当前锁的拥有者为当前线程，并设置AQS的状态值为1，然后直接返回。如果当前线程之前已经获取过该锁，则这次只是简单地把AQS状态值加1后返回。如果该锁已经被其它线程持有，则调用该方法的线程会被放入AQS队列后阻塞挂起。
- `unlock()` - 用于**释放锁**。

> 🔔 注意：请务必牢记，获取锁操作 **`lock()` 必须在 `try catch` 块中进行，并且将释放锁操作 `unlock()` 放在 `finally` 块中进行，以保证锁一定被被释放，防止死锁的发生**。

```java
public void lock() {
	sync.lock();
}
```

`ReentrantLock`的`Lock()`委托给了`sync`类, 根据创建`ReentrantLock`构造函数选择`sync`的实现是`NofairSync`还是`FairSync`,这个锁是一个非公平锁或公平锁.

#### `NofairSync`非公平锁情况

```java
final void lock() {
    // (1)CAS设置状态值
    if (compareAndSetState(0, 1))
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);	//调用AQS的acquire方法
}

protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}
```

在(1)中,因为默认的AQS状态值为0,所以第一个调用`Lock`的线程会通过CAS设置状态值为1, CAS成功则表示当前线程获取到了锁,然后`setExclusiveOwnerThread`设置该锁持有者是当前线程.

如果这时有其它线程调用`Lock`方法企图获取该锁, CAS会失败, 然后会调用AQS的`acquire`方法. 注意, 传递参数为1, 下面是AQS的`acquire`核心代码:

```java
public final void acquire(long arg) {
    // (3)调用ReentrantLock重写的tryAcquire
    if (!tryAcquire(arg) &&
        // tryAcquire返回false会把当前线程放入AQS阻塞队列
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

AQS并没有提供可用的`tryAcquire`方法,`tryAcquire`方法需要子类自己定制化,所以(3)处会调用`ReentrantLock`重写的`tryAcquire`方法.

```java
protected final boolean tryAcquire(int acquires) {
	return nonfairTryAcquire(acquires);
}

final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    // (4)当前AQS状态值为0
    if (c == 0) {
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }	// (5)当前线程是该锁的持有者
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

首先代码(4)会查看当前的状态值是否为0, 为0说明当前**锁空闲**, 那么就尝试CAS获取当前锁, 将AQS的状态值从0设置为1, 并设置当前锁的持有者为当前线程, 然后返回true. 

如果当前**状态值不为0**, 则说明该锁已经被某个线程持有, 所以代码(5)查看当前线程是否为该锁的持有者, 如果当前线程是该锁的持有者, 则状态值加1, 然后返回true. 需要注意的是: `nextc<0`说明可重入次数溢出了. 

如果当前线程不是该锁的持有者, 返回`false`, 然后会被放入AQS阻塞队列.

**非公平锁的体现**:

假设线程A调用`lock()`方法时执行到`nofairTryAcquire`的代码(4), 发现当前状态值不为0, 所以执行代码(5), 发现当前线程不是线程持有者, 则执行代码(6)返回`false`, 然后当前线程进入AQS阻塞队列.

这时线程B也调用了`lock()`方法执行到`nonfairTryAcquire`的代码(4), 发现当前状态值为0(假设占有该锁的其它线程释放了该锁), 所以通过CAS设置获取了该锁. 按道理应该是A先获取锁, 这就是非公平性的体现.

#### `FairSync`公平锁情况

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    // (7)当前状态值为0
    if (c == 0) {
        // (8)公平性策略
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // (9) 当前线程是该锁的持有者
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

公平的`tryAcquire`与非公平的类似, 不同之处在于, 代码(8)在设置CAS前添加了`hasQueuedPredecessors`方法, 该方法是实现公平性的核心代码(在AQS源码中):

```java
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

在上述代码中, 如果当前线程节点有前驱节点则返回`true`, 否则如果当前AQS队列为空或者当前线程节点是AQS的第一个节点则返回`false`.其中如果`h == t`则说明当前队列为空, 直接返回`false`, 

如果`h != t`并且`s == null`则说明有一个元素将要作为AQS的第一个节点入队列, 那么返回`true`, 如果`h != t`并且`s != null`和`s.thread != Thread.currentThread())`则说明队列里面第一个元素不是当前线程, 那么返回`true`

### `void lockInterruptibly()方法`

该方法与`lock()`方法类似, 不同之处在于, 它能中断进行响应, 就是当前线程在调用该方法时, 如果其它线程调用了当前线程的`interrupt()`方法, 则当前线程会抛出`InterruptedException`异常. 源码在AQS包中.

- `lockInterruptibly()` - **可中断获取锁**。可中断获取锁可以在获得锁的同时保持对中断的响应。可中断获取锁比其它获取锁的方式稍微复杂一些，需要两个 `try-catch` 块（如果在获取锁的操作中抛出了`InterruptedException` ，那么可以使用标准的 `try-finally` 加锁模式）。
  - 举例来说：假设有两个线程同时通过 `lock.lockInterruptibly()` 获取某个锁时，若线程 A 获取到了锁，则线程 B 只能等待。若此时对线程 B 调用 `threadB.interrupt()` 方法能够中断线程 B 的等待过程。由于 `lockInterruptibly()` 的声明中抛出了异常，所以 `lock.lockInterruptibly()` 必须放在 `try` 块中或者在调用 `lockInterruptibly()` 的方法外声明抛出 `InterruptedException`。

> 🔔 注意：当一个线程获取了锁之后，是不会被 `interrupt()` 方法中断的。单独调用 `interrupt()` 方法不能中断正在运行状态中的线程，只能中断阻塞状态中的线程。因此当通过 `lockInterruptibly()` 方法获取某个锁时，如果未获取到锁，只有在等待的状态下，才可以响应中断。

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}

public final void acquireInterruptibly(long arg)
    throws InterruptedException {
    // 如果当前线程被中断, 则直接抛出异常
    if (Thread.interrupted())
        throw new InterruptedException();
    // 尝试获取资源
    if (!tryAcquire(arg))
        // 调用AQS可被中断的方法
        doAcquireInterruptibly(arg);
}
```

### `boolean tryLock()方法`

尝试获取锁, 如果当前该锁没有被其它线程持有, 则当前线程获取该锁并返回`true`, 否则返回`false`. 该方法不会引起当前线程阻塞.

- `tryLock()` - **可轮询获取锁**。如果成功，则返回 true；如果失败，则返回 false。也就是说，这个方法**无论成败都会立即返回**，获取不到锁（锁已被其他线程获取）时不会一直等待。
- `tryLock(long, TimeUnit)` - **可定时获取锁**。和 `tryLock()` 类似，区别仅在于这个方法在**获取不到锁时会等待一定的时间**，在时间期限之内如果还获取不到锁，就返回 false。如果如果一开始拿到锁或者在等待期间内拿到了锁，则返回 true。

```java
public boolean tryLock() {
    return sync.nonfairTryAcquire(1);
}

final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}

public boolean tryLock(long timeout, TimeUnit unit)
    throws InterruptedException {
    // 调用AQS包中的tryAcquireNanos方法
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
```

源码的实现与非公平锁的`tryAcquire()`方法类似, 所以`tryLock()`使用的是非公平锁.

### `void unlock()`方法

尝试释放当前锁, 如果当前线程持有该锁, 则调用该方法会让该线程对该线程持有的AQS状态值减1, 如果减去1后当前状态值为0, 则当前线程会释放该锁, 否则仅仅减1而已. 如果当前线程没有持有该锁而调用了该方法则会抛出`IllegalMonitorStateException`异常.

```java
public void unlock() {
    sync.release(1);
}

protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    // (11) 如果不是锁持有则调用unlock则会抛出异常
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    // (12) 如果当前可重入次数为0, 则清空持有该锁的线程
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    // (13) 设置可重入次数为原始值-1
    setState(c);
    return free;
}
```

# 4. `ReentrantReadWriteLock`

`ReadWriteLock` 适用于**读多写少的场景**。

`ReentrantReadWriteLock` 类是 `ReadWriteLock` 接口的具体实现，它是一个可重入的读写锁。`ReentrantReadWriteLock` 维护了一对读写锁，将读写锁分开，有利于提高并发效率。

读写锁，并不是 Java 语言特有的，而是一个广为使用的通用技术，所有的读写锁都遵守以下三条基本原则：

- 允许多个线程同时读共享变量；
- 只允许一个线程写共享变量；
- 如果一个写线程正在执行写操作，此时禁止读线程读共享变量。

读写锁与互斥锁的一个重要区别就是**读写锁允许多个线程同时读共享变量**，而互斥锁是不允许的，这是读写锁在读多写少场景下性能优于互斥锁的关键。但**读写锁的写操作是互斥的**，当一个线程在写共享变量的时候，是不允许其他线程执行写操作和读操作。

## 4.1 `ReentrantReadWriteLock`的特性

`ReentrantReadWriteLock` 的特性如下：

- **`ReentrantReadWriteLock` 适用于读多写少的场景**。如果是写多读少的场景，由于 `ReentrantReadWriteLock` 其内部实现比 `ReentrantLock` 复杂，性能可能反而要差一些。如果存在这样的问题，需要具体问题具体分析。由于 `ReentrantReadWriteLock` 的读写锁（`ReadLock`、`WriteLock`）都实现了 `Lock` 接口，所以要替换为 `ReentrantLock` 也较为容易。

- `ReentrantReadWriteLock` 实现了 `ReadWriteLock` 接口，支持了 `ReentrantLock` 所不具备的读写锁分离。`ReentrantReadWriteLock` 维护了一对读写锁（`ReadLock`、`WriteLock`）。将读写锁分开，有利于提高并发效率。`ReentrantReadWriteLock` 的加锁策略是：**允许多个读操作并发执行，但每次只允许一个写操作**。
- `ReentrantReadWriteLock` 为读写锁都提供了可重入的加锁语义。
- `ReentrantReadWriteLock` 支持公平锁和非公平锁（默认）两种模式。

`ReadWriteLock` 接口定义如下：

```java
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
```

- `readLock` - 返回用于读操作的锁（`ReadLock`）。
- `writeLock` - 返回用于写操作的锁（`WriteLock`）。

在读写锁和写入锁之间的交互可以采用多种实现方式，`ReadWriteLock` 的一些可选实现包括：

- **释放优先** - 当一个写入操作释放写锁，并且队列中同时存在读线程和写线程，那么应该优先选择读线程、写线程，还是最先发出请求的线程？
- **读线程插队** - 如果锁是由读线程持有，但有写线程正在等待，那么新到达的读线程能否立即获得访问权，还是应该在写线程后面等待？如果允许读线程插队到写线程之前，那么将提高并发性，但可能造成线程饥饿问题。
- **重入性** - 读锁和写锁是否是可重入的？
- **降级** - 如果一个线程持有写入锁，那么它能否在不释放该锁的情况下获得读锁？这可能会使得写锁被降级为读锁，同时不允许其他写线程修改被保护的资源。
- **升级** - 读锁能否优先于其他正在等待的读线程和写线程而升级为一个写锁？在大多数的读写锁实现中并不支持升级，因为如果没有显式的升级操作，那么很容易造成死锁。

## 4.2. `ReentrantReadWriteLock` 的用法及原理

在AQS中只维护了一个`state`状态, 而`ReentrantReadWriteLock`则需要维护读状态和写状态.它巧妙的用`state`的高16位表示读状态, 也就是获取到读锁的次数, 用低16位表示获取到写锁的线程的可重入次数.

```java
static final int SHARED_SHIFT   = 16;
// 共享锁(读锁)状态的单位值65536
static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
// 共享锁线程最大个数65535
static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
// 排他锁(写锁)掩码, 二进制, 15个1
static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
// 返回读线程数
static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
// 返回写锁可重入个数
static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
```



### `ReentrantReadWriteLock` 的构造方法

`ReentrantReadWriteLock` 和 `ReentrantLock` 一样，也有两个构造方法，且用法相似。

```java
public ReentrantReadWriteLock() {
    this(false);
}

public ReentrantReadWriteLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
    readerLock = new ReadLock(this);
    writerLock = new WriteLock(this);
}
```

- `ReentrantReadWriteLock()` - 默认构造方法会初始化一个**非公平锁（NonfairSync）**。在非公平的锁中，线程获得锁的顺序是不确定的。写线程降级为读线程是可以的，但读线程升级为写线程是不可以的（这样会导致死锁）。
- `ReentrantReadWriteLock(boolean)` - `new ReentrantLock(true)` 会初始化一个**公平锁（FairSync）**。对于公平锁，等待时间最长的线程将优先获得锁。如果这个锁是读线程持有，则另一个线程请求写锁，那么其他读线程都不能获得读锁，直到写线程释放写锁。

### 写锁的获取与释放

#### `void lock()`方法

```java
public void lock() {
    sync.acquire(1);
}

public final void acquire(long arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

调用了AQS的`acquire`方法, 其中`tryAcquire`是`ReentrantReadWriteLock`内部`sync`类重写: 

```java
protected final boolean tryAcquire(int acquires) {
    Thread current = Thread.currentThread();
    int c = getState();
    int w = exclusiveCount(c);	// c与16位1相与
    // (1) c != 0说明读锁或者写锁已经被某线程获取
    if (c != 0) {
        // (2) w = 0 说明已经有线程获取了读锁, w != 0并且当前线程不是写锁拥有者, 则返回false
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        // (3) 说明当前线程获取了写锁, 判断可重入次数
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        // (4)设置可重入次数
        setState(c + acquires);
        return true;
    }
    // (5)第一个写线程获取写锁
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```

在代码(1)中, 如果当前AQS状态值不为0则说明当前已经有线程获取到了读锁或者写锁. 在(2)中, 如果`w == 0`则说明状态值低16位位0, 而AQS状态值不为0, 则说明高16位不为0, 这暗示已经有线程获取了读锁, 所以直接返回`false`.

而如果`w != 0`则说明当前已经有线程获取到了该写锁, 再看当前线程是不是该写锁的持有者, 如果不是则返回`false`

执行到(3)说明当前线程之前已经获取到了该锁, 所以判断该线程的可重入次数是不是超过了最大值, 是则抛出异常, 都在执行(4)处代码增加当前线程可重入次数, 然后返回`true`

如果AQS的状态值等于0则说明目前没有线程获取到读锁和写锁, 所以执行代码(5). 其中, 对于`writeShouldBlock`方法, 非公平锁的实现为: 

```java
final boolean writerShouldBlock() {
    return false; // writers can always barge
}
```

如果代码对于非公平锁来说总是返回`false`, 则说明带啊吗(5)抢占式执行CAS尝试获取写锁, 获取成功则设置当前锁的持有者为当前线程并返回`true`, 否则返回`false`.

公平锁的实现为:

```java
final boolean writerShouldBlock() {
    return hasQueuedPredecessors();
}
```

还是使用`hasQueuedPredecessors`来判断当前线程节点是否有前驱节点, 如果有则当前线程放弃获取写锁的权限, 直接返回`false`

#### `void lockInterruptibly()`方法

与`ReentrantLock`中方法一致

该方法与`lock()`方法类似, 不同之处在于, 它能中断进行响应, 就是当前线程在调用该方法时, 如果其它线程调用了当前线程的`interrupt()`方法, 则当前线程会抛出`InterruptedException`异常. 源码在AQS包中.

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}
```

#### `boolean tryLock()`方法

尝试获取写锁. 用法和`ReentrantLock`中类似.

```java
public boolean tryLock( ) {
    return sync.tryWriteLock();
}

final boolean tryWriteLock() {
    Thread current = Thread.currentThread();
    int c = getState();
    if (c != 0) {
        int w = exclusiveCount(c);
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
    }
    if (!compareAndSetState(c, c + 1))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```

与`tryAcquire`方法类似, 不同在于这里使用的是非公平锁策略.

还有一个`boolean tryLock(long timeout, TimeUnit unit)`方法. 尝试获取写锁失败则会把当前线程挂起指定时间, 待超时时间到后当前线程被激活, 如果还是没有获取到写锁则返回`false`. 另外, 该方法会对中断进行响应.

```java
public boolean tryLock(long timeout, TimeUnit unit)
    throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
```

#### `void unlock()`方法

和`ReentrantLokc`方法用法类似.

```java
public void unlock() {
    sync.release(1);	// AQS中的release
}

public final boolean release(long arg) {
    // 调用ReentrantReadWriteLock中sync实现的tryRelease方法
    if (tryRelease(arg)) {
        // 激活阻塞队列中的一个线程
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}

protected final boolean tryRelease(int releases) {
    // (6)看是否是写锁拥有者调用的unlock
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    // (7)获取可重入值,这里没有考虑高16位, 因为获取写锁时读锁状态值肯定为0
    int nextc = getState() - releases;
    boolean free = exclusiveCount(nextc) == 0;
    // (8)如果写锁可重入值为0则释放锁, 否则只是简单地更新状态值
    if (free)
        setExclusiveOwnerThread(null);
    setState(nextc);
    return free;
}
```

在上述代码中,`tryRelease`首先通过`isHeldExclusively`判断是否当前线程时该写锁的持有者, 如果不是则抛出异常, 否则执行代码(7), 这说明当前线程持有写锁, 持有写锁说明状态值的高16位为0, 所以这里`nextc`值就是当前线程写锁的剩余可重入次数. 代码(8)判断当前可重入次数是否为0, 如果`free`为`true`则说明可重入次数为0, 所以当前线程会释放写锁, 将当前锁的持有者设置为`null`.如果`free`为`false`则简单地更新可重入次数.

### 读锁的获取与释放

#### `void lock()`方法

获取读锁, 如果当前没有其它线程持有写锁, 则当前线程可以获取读锁, AQS的状态值`state`的高16位会加1, 然后方法返回. 否则如果有其它线程持有写锁, 则当前线程会被阻塞.

```java
public void lock() {
    sync.acquireShared(1);	// 调用AQS中的方法
}

public final void acquireShared(long arg) {
    // 调用ReentrantReadWriteLock中的sync的tryAcquireShared方法
    if (tryAcquireShared(arg) < 0)
        // 调用AQS中的方法
        doAcquireShared(arg);
}

// ReentrantReadWriteLock中的sync的tryAcquireShared方法
protected final int tryAcquireShared(int unused) {
    // (1)获取当前状态值
    Thread current = Thread.currentThread();
    int c = getState();
    
    // (2)判断是否写锁被占用: 低16位与16位1相与后是否不为0
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    // (3)获取读锁计数
    int r = sharedCount(c);
    // (4)尝试获取锁,多个读线程只有一个会成功,不成功的进入fullTryAcquireShared进行重试
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        // (5)第一个线程获取读锁
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        // (6)如果当前线程是第一个获取读锁的线程
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            // (7)记录最后一个获取读锁的线程或记录其它线程读锁的可重入数
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    // (8)类似tryAcquireShared, 但是是自旋获取
    return fullTryAcquireShared(current);
}
```

如上代码先获取当前AQS的状态值, 然后代码(2)查看是否有其它线程获取到写锁. 如果是则直接返回-1, 而后调用AQS的`doAcquireShared`方法把当前线程放入AQS阻塞队列.

如果**当前要获取读锁的进程已经持有了写锁, 则也可以获取读锁**. 但是要注意, 当一个进程先获取了写锁, 然后获取了读锁处理事情完毕后, 要记得把读锁和写锁都释放掉, 不能只释放写锁.

否则执行代码(3), 得到获取到的读锁的个数, 到这里说明目前没有线程获取到写锁, 但是可能有线程持有读锁, 然后执行代码(4).其中非公平锁的`readerShouldBlock`实现代码如下:

```java
final boolean readerShouldBlock() {
    return apparentlyFirstQueuedIsExclusive();
}

final boolean apparentlyFirstQueuedIsExclusive() {
    Node h, s;
    return (h = head) != null &&
        (s = h.next)  != null &&
        !s.isShared()         &&
        s.thread != null;
}
```

上面代码的作用是, 如果队列里面存在一个元素, 则判断第一个元素是不是正在尝试获取写锁, 如果不是, 则当前线程判断当前获取读锁的线程是否达到了最大值. 最后执行CAS操作将AQS状态值的高16位值增加1.

代码(5)(6)记录第一个获取读锁的线程并统计该线程获取读锁的可重入数.代码(7)使用`cachedHoldCounter`记录最后一个获取到读锁的线程和该线程获取读锁的可重入数, `readHolds`记录了当前线程获取读锁的可重入数.

如果`readerShouldBlock`返回`true`则说明有线程正在获取写锁, 所以执行(8)处代码,` fullTryAcquireShared`的代码与`tryAcquireShared`类似, 它们的不同之处在于, 前者通过循环自旋获取.

#### `void lockInterruptibly()`方法

与之前讲述的类似

#### `void tryLock()`方法

尝试获取读锁, 如果当前没有其它线程持有写锁, 则会成功获取读锁, 返回`true`. 否则返回`false`, 但当前线程不会被阻塞..

如果当前线程已经持有了该读锁, 则简单的增加AQS的状态值高16位,然后返回`true`

还有一个`boolean tryLock(long timeout, TimeUnit unit)`方法. 源码和前面代码类似.

#### `void unlock()`方法

```java
public void unlock() {
    sync.releaseShared(1);
}

public final boolean releaseShared(long arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}

protected final boolean tryReleaseShared(int unused) {
    Thread current = Thread.currentThread();
    if (firstReader == current) {
        // assert firstReaderHoldCount > 0;
        if (firstReaderHoldCount == 1)
            firstReader = null;
        else
            firstReaderHoldCount--;
    } else {
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current))
            rh = readHolds.get();
        int count = rh.count;
        if (count <= 1) {
            readHolds.remove();
            if (count <= 0)
                throw unmatchedUnlockException();
        }
        --rh.count;
    }
    // 循环直到自己的读计数-1, CAS更新成功
    for (;;) {
        int c = getState();
        int nextc = c - SHARED_UNIT;
        if (compareAndSetState(c, nextc))
            // Releasing the read lock has no effect on readers,
            // but it may allow waiting writers to proceed if
            // both read and write locks are now free.
            return nextc == 0;
    }
}
```

在无限循环里面, 首先获取当前AQS状态值并将其保存到变量c, 然后变量c被减去一个读计数单位后使用CAS操作更新AQS的状态值, 如果更新成功则查看当前AQS状态值是否位0, 为0则说明当前已经没有读线程占用读锁, 则`tryReleaseShared`返回`true`.然后调用`doReleaseShared`方法释放一个由于获取写锁而被阻塞的线程, 如果当前AQS状态值不为0, 则说明当前还有其它线程持有了读锁, 所以返回`false`. 如果`tryReleaseShared`中的CAS更新AQS状态值失败, 则自旋重试直到成功.

# 5. `StampedLock`

`ReadWriteLock` 支持两种模式：一种是读锁，一种是写锁。

而 `StampedLock` 支持三种模式，分别是：**写锁**、**悲观读锁**和**乐观读**。其中，写锁、悲观读锁的语义和 `ReadWriteLock` 的写锁、读锁的语义非常类似，允许多个线程同时获取悲观读锁，但是只允许一个线程获取写锁，写锁和悲观读锁是互斥的。不同的是：`StampedLock` 里的写锁和悲观读锁加锁成功之后，都会返回一个 `stamp`；然后解锁的时候，需要传入这个 `stamp`。

> 注意这里，用的是“乐观读”这个词，而不是“乐观读锁”，是要提醒你，**乐观读这个操作是无锁的**，所以相比较 ReadWriteLock 的读锁，乐观读的性能更好一些。

`StampedLock` 的性能之所以比 `ReadWriteLock `还要好，其关键是**StampedLock 支持乐观读**的方式。

- `ReadWriteLock` 支持多个线程同时读，但是当多个线程同时读的时候，所有的写操作会被阻塞；
- 而 `StampedLock` 提供的乐观读，是允许一个线程获取写锁的，也就是说不是所有的写操作都被阻塞。

对于读多写少的场景 `StampedLock` 性能很好，简单的应用场景基本上可以替代 `ReadWriteLock`，但是**StampedLock 的功能仅仅是 ReadWriteLock 的子集**，在使用的时候，还是有几个地方需要注意一下。

- **StampedLock 不支持重入**
- `StampedLock` 的悲观读锁、写锁都不支持条件变量。
- 如果线程阻塞在`` StampedLock` 的 `readLock() `或者 `writeLock() `上时，此时调用该阻塞线程的 `interrupt()`` 方法，会导致 CPU 飙升。**使用 StampedLock 一定不要调用中断操作，如果需要支持中断功能，一定使用可中断的悲观读锁 readLockInterruptibly() 和写锁 writeLockInterruptibly()**。

# 6. 参考资料

- <<Java并发编程之美>>

- [深入理解JAVA并发锁](https://dunwu.github.io/javacore/concurrent/Java%E9%94%81.html#_8-%E5%8F%82%E8%80%83%E8%B5%84%E6%96%99)

  