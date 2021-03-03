# 1. 线程池状态含义

- `RUNNING`: **运行状态**。接受新任务并且处理阻塞队列里面的任务.
- `SHUTDOWN`: **关闭状态**。拒绝新任务但是处理阻塞队列里的任务.
- `STOP`: **停止状态**。拒绝新任务并且抛弃阻塞队列里的任务, 同时会中断正在处理的任务.
- `TIDYING`: **整理状态**。所有任务都执行完(包含阻塞队列里面的任务)后当前线程池活动线程数为0, 将要调用`terminated`方法.
- `TERMINATED`: **终止状态**. `terminated`方法调用完成以后的状态.

# 2. 线程池状态转换列举如下:

- `RUNNING`—>`SHUTDOWN`：显示调用`shutdown()`方法，或隐式调用`finalize()`方法里面的`shutdowm()`方法.
- `RUNNING`或`SHUTDOWN`—>`STOP`：显示调用`shutdownNow()`方法时.
- `SHUTDOWN`—>`TIDYING`：当线程池和任务队列都为空时.
- `STOP`—>`TIDYING`：当线程池为空时.
- `TIDYING`—>`TERMINATED`：当`terminated()`方法执行完成时.



# 3. 线程池参数如下：

- `corePoolSize`：线程池核心线程数.

  - 如果运行的线程数少于 `corePoolSize`，则创建新线程来处理任务，即使线程池中的其他线程是空闲的。
  - 如果线程池中的线程数量大于等于 `corePoolSize` 且小于 `maximumPoolSize`，则只有当 `workQueue` 满时才创建新的线程去处理任务；
  - 如果设置的 `corePoolSize` 和 `maximumPoolSize` 相同，则创建的线程池的大小是固定的。这时如果有新任务提交，若 `workQueue` 未满，则将请求放入 `workQueue` 中，等待有空闲的线程去从 `workQueue` 中取任务并处理；
  - 如果运行的线程数量大于等于 `maximumPoolSize`，这时如果 `workQueue` 已经满了，则使用 `handler` 所指定的策略来处理任务；
  - 所以，任务提交时，判断的顺序为 `corePoolSize` => `workQueue` => `maximumPoolSize`。

- `workQueue`：用于保存等待执行的任务的阻塞队列。

  - `ArrayBlockingQueue`有界阻塞队列。

    - 此队列是**基于数组的先进先出队列（FIFO）**。
    - 此队列创建时必须指定大小。

  - `LinkedBlockingQueue` - **无界阻塞队列**

    - 此队列是**基于链表的先进先出队列（FIFO）**。

    - 如果创建时没有指定此队列大小，则默认为 `Integer.MAX_VALUE`。
    - 吞吐量通常要高于 `ArrayBlockingQueue`。
    - 使用 `LinkedBlockingQueue` 意味着： `maximumPoolSize` 将不起作用，线程池能创建的最大线程数为 `corePoolSize`，因为任务等待队列是无界队列。
    - `Executors.newFixedThreadPool` 使用了这个队列。

  - `SynchronousQueue`不会保存提交的任务，而是将直接新建一个线程来执行新来的任务。

    - 每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态。
    - 吞吐量通常要高于 `LinkedBlockingQueue`。
    - `Executors.newCachedThreadPool` 使用了这个队列。

  - `PriorityBlockingQueue` - **具有优先级的无界阻塞队列**。

- `maximumPoolSize`：线程池最大线程数量.

  - 如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。
  - 值得注意的是：如果使用了无界的任务队列这个参数就没什么效果。

- `threadFactory`：创建线程的工厂. 可以通过线程工厂给每个创建出来的线程设置更有意义的名字。

- `RejectedExecutionHandler`：饱和策略，当队列满并且线程个数达到`maximunPoolSize`后采取的策略，比如`AbortPolicy(抛出异常)`、`CallerRunsPolicy(使用调用者所在线程来运行任务)`、`DiscardOldestPolicy(调用poll丢弃一个任务，执行当前任务)`及`DiscardPolicy(默默丢弃，不抛出异常)`

- keepAliveTime：存活时间。如果当前线程池中的线程数量比核心线程数量多，并且是闲置状态，则这些闲置的线程能存活的最大时间.

  - 当线程池中的线程数量大于 `corePoolSize` 的时候，如果这时没有新的任务提交，核心线程外的线程不会立即销毁，而是会等待，直到等待的时间超过了 `keepAliveTime`。
  - 所以，如果任务很多，并且每个任务执行的时间比较短，可以调大这个时间，提高线程的利用率。

- `TimeUnit`：存活时间的时间单位.

- `handler` - **饱和策略**。它是 `RejectedExecutionHandler` 类型的变量。当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。线程池支持以下策略：

  - `AbortPolicy` - 丢弃任务并抛出异常。这也是默认策略。
  - `DiscardPolicy` - 丢弃任务，但不抛出异常。
  - `DiscardOldestPolicy` - 丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）。
  - `CallerRunsPolicy` - 直接调用 `run` 方法并且阻塞执行。
  - 如果以上策略都不能满足需要，也可以通过实现 `RejectedExecutionHandler` 接口来定制处理策略。如记录日志或持久化不能处理的任务。

# 4. 线程池类型如下

- `newFixedThreadPool`：创建一个核心线程个数和最大线程个数都为`nThreads`的线程池，并且阻塞队列长度为`Integer.MAX_VALUE`。`keeyAliveTime=0`说明只要线程数比核心线程个数多并且当前空闲则回收。
- `newSingleThreadExecutor`：创建一个核心线程个数和最大线程个数都为1的线程池，并且阻塞队列长度为`Integer.MAX_VALUE`. `keeyAliveTime=0`说明只要线程个数比核心线程个数多并且当前空闲则回收。
- `newCachedThreadPool`：创建一个按需创建线程的线程池，初始线程个数为0，最多线程个数为`Integer.MAX_VALUE`，并且阻塞队列为同步队列. `keeyAliveTime=60`说明只要当前线程在60s内空闲则回收。这个类型的特殊之处在于，加入同步队列的任务会被马上执行，同步队列里面最多只有一个任务。