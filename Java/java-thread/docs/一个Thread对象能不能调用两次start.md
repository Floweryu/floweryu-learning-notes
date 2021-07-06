执行下面这段程序：

```java
package main;

/**
 * @author Floweryu
 * @date 2021/6/17 10:21
 */
public class One {
    public static void main(String[] args) {
        PrintTime printTime = new PrintTime();
        printTime.start();
        printTime.start();
    }

    public static class PrintTime extends Thread {
        @Override
        public void run() {
            System.out.println(System.currentTimeMillis());
        }
    }
}

```

结果如下：

![image-20210617111607923](https://i.loli.net/2021/06/17/sZFExQ8XL2mICco.png)

发现只启动了一个线程，另一个线程抛出`IllegalThreadStateException`异常。这是怎么回事呢？

### 原因

Java的线程是不允许启动两次的，第二次调用必然会抛岀 `IllegalThreadStateEXception`，这是一种运行时异常，多次调用 `start `被认为是编程错误。在第二次调用 start()方法的时候，线程可能处于终止或者其他（非NEW）状态，但是不论如何，都是不可以再次启动的。

对于上述程序，查看`start`源码可以发现，在启动线程前会先对`threadStatus`进行判断，不为0就会抛出异常。

![image-20210617112616949](https://i.loli.net/2021/06/17/rVK1Fn4ykuxZw8C.png)

在源码中，通过下面方法将`threadStatus`转换为线程生命周期：

```java
public State getState() {
    // get current thread state
    return sun.misc.VM.toThreadState(threadStatus);
}

public static State toThreadState(int var0) {
    if ((var0 & 4) != 0) {
        return State.RUNNABLE;
    } else if ((var0 & 1024) != 0) {
        return State.BLOCKED;
    } else if ((var0 & 16) != 0) {
        return State.WAITING;
    } else if ((var0 & 32) != 0) {
        return State.TIMED_WAITING;
    } else if ((var0 & 2) != 0) {
        return State.TERMINATED;
    } else {
        return (var0 & 1) == 0 ? State.NEW : State.RUNNABLE;
    }
}
```

### 线程的生命周期

- 新建（NEW），表示线程被创建出来还没真正启动的状态，可以认为它是个Java内部状态。
- 就绪（ RUNNABLE），表示该线程已经在JVM中执行，当然由于执行需要计算资源，它可能是正在运行，也可能还在等待系统分配给它CPU片段，在就绪队列里面排队。
- 运行（Running）在其他一些分析中，会额外区分一种状态 RUNNING，但是从 Java aPi的角度，并不能表示出来。
- 阻塞（ BLOCKED），阻塞表示线程在等待 Monitor lock。比如，线程试图通过synchronized去获取某个锁，但是其他线程已经独占了，那么当前线程就会处于阻塞状态。
- 等待（WAITING），表示正在等待其他线程采取某些操作。一个常见的场景是类似生产者消费者模式，发现任务条件尚未满足，就让当前消费者线程等待（wait），另外的生产者线程去准备任务数据，然后通过类似notify等动作，通知消费线程可以继续工作了。Thread.join()也会令线程进入等待状态。
- 计时等待（ TIMED_WAIT），其进入条件和等待状态类似，但是调用的是存在超时条件的方法，比如wait或join等方法的指定超时版本
- 终止（ TERMINATED），不管是意外退出还是正常执行结束，线程已经完成使命，终止运行，也有人把这个状态叫作死亡。

![image-20210617112453376](https://i.loli.net/2021/06/17/6b4uiHcdthlBXGe.png)