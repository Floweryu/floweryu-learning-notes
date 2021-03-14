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

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}
```



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

## 4.1 `newFixedThreadPool`

创建一个指定工作线程数量的线程池。每当提交一个任务就创建一个工作线程，如果工作线程数量达到线程池初始的最大数，则将提交的任务存入到池队列中。阻塞队列长度为`Integer.MAX_VALUE`。

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}

public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(),
                                  threadFactory);
}
```

**特点**：

具有线程池提高程序效率和节省创建线程时所耗的开销的优点。

但是，在线程池空闲时，即线程池中没有可运行任务时，它不会释放工作线程，还会占用一定的系统资源。`keeyAliveTime=0`说明只要线程数比核心线程个数多并且当前空闲则回收。

【示例】

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class FixedThreadPollDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService poll = Executors.newFixedThreadPool(2);

        Thread t1 = new MyThread();
        Thread t2 = new MyThread();
        Thread t3 = new MyThread();
        Thread t4 = new MyThread();
        Thread t5 = new MyThread();
        
        poll.execute(t1);
        poll.execute(t2);
        poll.execute(t3);
        poll.execute(t4);
        poll.execute(t5);
        
        poll.shutdown();
    }
}

class MyThread extends Thread {
    @Override
    public void run () {
        System.out.println(Thread.currentThread().getName() + "正在执行");
    }
}

```

```bash
输出：可见只有两个线程在工作
pool-1-thread-2正在执行
pool-1-thread-1正在执行
pool-1-thread-2正在执行
pool-1-thread-1正在执行
pool-1-thread-2正在执行
```

## 4.2 `newSingleThreadExecutor`

创建一个单线程化的Executor，即**只创建唯一的工作者线程**来执行任务，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。如果这个线程异常结束，会有另一个取代它，保证顺序执行。单工作线程最大的特点是可保证顺序地执行各个任务，并且在任意给定的时间不会有多个线程是活动的。

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}

public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>(),
                                threadFactory));
}
```

【示例】

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SingleThreadPoolDemo {
    public static void main (String[] args) throws Exception{
        // 创建一个使用单个 worker 线程的 Executor，以无界队列方式来运行该线程。
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Runnable task1 = new SingleTasks();
        Runnable task2 = new SingleTasks();
        Runnable task3 = new SingleTasks();

        pool.execute(task1);
        pool.execute(task2);
        pool.execute(task3);

        pool.shutdown();
    }
}

class SingleTasks implements Runnable {
    @Override
    public void run () {
        System.out.println(Thread.currentThread().getName() + "正在执行");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName() + "执行完毕");
    }
}

```

```bash
pool-1-thread-1正在执行
pool-1-thread-1执行完毕
pool-1-thread-1正在执行
pool-1-thread-1执行完毕
pool-1-thread-1正在执行
pool-1-thread-1执行完毕
```

## 4.3 `newCachedThreadPool`

创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程。

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}

public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>(),
                                  threadFactory);
}
```

**特点**：

工作线程的创建数量几乎没有限制(其实也有限制的,数目为Interger. MAX_VALUE), 这样可灵活的往线程池中添加线程。

## 4.4 `newScheduleThreadPool`

创建一个定长的线程池，而且支持定时的以及周期性的任务执行，支持定时及周期性任务执行。

```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}

public static ScheduledExecutorService newScheduledThreadPool(
    int corePoolSize, ThreadFactory threadFactory) {
    return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
}
```

## 4.5 `newSingleThreadScheduledExecutor`

创建一个单线程执行程序，它可安排在给定延迟后运行命令或者定期地执行。线程池中最多执行1个线程，之后提交的线程活动将会排在队列中以此执行并且可定时或者延迟执行线程活动。

```java
public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
    return new DelegatedScheduledExecutorService
        (new ScheduledThreadPoolExecutor(1, threadFactory));
}

public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
    return new DelegatedScheduledExecutorService
        (new ScheduledThreadPoolExecutor(1));
}
```

## 4.6 使用`ThreadPoolExecutor`自定义线程池

【示例】

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

public class ThreadPoolDemo {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 200, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(5));

        for (int i = 0; i < 15; i++) {
            MyTask myTask = new MyTask(i);
            executor.execute(myTask);
            System.out.println("线程池中线程数目：" + executor.getPoolSize() + "，队列中等待执行的任务数目：" + executor.getQueue().size()
                    + "，已执行完别的任务数目：" + executor.getCompletedTaskCount());
        }
        executor.shutdown();
    }
}

class MyTask implements Runnable {
    private int taskNum;

    public MyTask(int num) {
        this.taskNum = num;
    }

    @Override
    public void run() {
        System.out.println("正在执行task " + taskNum);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("task " + taskNum + "执行完毕");
    }
}
```

```bash
// 输出如下：
正在执行task 0
线程池中线程数目：1，队列中等待执行的任务数目：0，已执行完别的任务数目：0
线程池中线程数目：2，队列中等待执行的任务数目：0，已执行完别的任务数目：0
正在执行task 1
正在执行task 2
线程池中线程数目：3，队列中等待执行的任务数目：0，已执行完别的任务数目：0
线程池中线程数目：4，队列中等待执行的任务数目：0，已执行完别的任务数目：0
正在执行task 3
线程池中线程数目：5，队列中等待执行的任务数目：0，已执行完别的任务数目：0
正在执行task 4
线程池中线程数目：5，队列中等待执行的任务数目：1，已执行完别的任务数目：0
线程池中线程数目：5，队列中等待执行的任务数目：2，已执行完别的任务数目：0
线程池中线程数目：5，队列中等待执行的任务数目：3，已执行完别的任务数目：0
线程池中线程数目：5，队列中等待执行的任务数目：4，已执行完别的任务数目：0
线程池中线程数目：5，队列中等待执行的任务数目：5，已执行完别的任务数目：0
线程池中线程数目：6，队列中等待执行的任务数目：5，已执行完别的任务数目：0
正在执行task 10
线程池中线程数目：7，队列中等待执行的任务数目：5，已执行完别的任务数目：0
正在执行task 11
线程池中线程数目：8，队列中等待执行的任务数目：5，已执行完别的任务数目：0
正在执行task 12
线程池中线程数目：9，队列中等待执行的任务数目：5，已执行完别的任务数目：0
正在执行task 13
线程池中线程数目：10，队列中等待执行的任务数目：5，已执行完别的任务数目：0
正在执行task 14
task 0执行完毕
正在执行task 5
task 1执行完毕
task 2执行完毕
正在执行task 6
正在执行task 7
task 14执行完毕
task 3执行完毕
正在执行task 9
task 10执行完毕
task 4执行完毕
task 11执行完毕
task 12执行完毕
task 13执行完毕
正在执行task 8
task 5执行完毕
task 6执行完毕
task 7执行完毕
task 9执行完毕
task 8执行完毕
```

从执行结果可以看出，当线程池中线程的数目大于5时，便将任务放入任务缓存队列里面，当任务缓存队列满了之后，便创建新的线程。

# 5. `execute`方法和`submit`方法区别

- `submit`属于`ExecutorService`接口，`execute`属于`Executor`接口。`ExecutorService`继承了`Executor`。
- `submit()`有返回值。
- `execute()`没有返回值。

## 5.1 源码

下面是`execute`源码和`submit`源码：

```java
public interface Executor {

	/**
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution
     * @throws NullPointerException if command is null
     */
    void execute(Runnable command);
}
```

```java
<T> Future<T> submit(Callable<T> task);

<T> Future<T> submit(Runnable task, T result);

Future<?> submit(Runnable task);
```

## 5.2 异常捕获

下面，执行一段代码：

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubmitExecuteDemo {
    
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        Runnable r = () -> System.out.println(1 / 0);
        pool.submit(r);			// 使用submit提交
        pool.shutdown();
    }
}
```

控制台输出为空，没有异常，也没有日志。

接着，将`pool.submit()`替换为`pool.execute()`：

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecuteDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        Runnable r = () -> System.out.println(1 / 0);
        pool.execute(r);
        pool.shutdown();
    }
}
```

可以看到输出了异常：

```bash
Exception in thread "pool-1-thread-1" java.lang.ArithmeticException: / by zero
        at ExecuteDemo.lambda$main$0(ExecuteDemo.java:8)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:834)
```

## 5.3 `submit`捕获异常

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SubmitExecuteDemo {
    
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);

        Runnable r = () -> System.out.println(1 / 0);
        Future<?> f = pool.submit(r);
        f.get();	// 使用Future.get()捕获异常
        pool.shutdown();
    }
}
```

```bash
Exception in thread "main" java.util.concurrent.ExecutionException: java.lang.ArithmeticException: / by zero
        at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122)
        at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191)
        at SubmitExecuteDemo.main(SubmitExecuteDemo.java:12)
Caused by: java.lang.ArithmeticException: / by zero
        at SubmitExecuteDemo.lambda$main$0(SubmitExecuteDemo.java:10)
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:834)
```

