#  `join()`方法简介

`wait()、notify()/notifyAll()`是`Object`类中的方法，`join()`方法是`Thread`类直接提供的。

**作用**：在当前线程A中调用另外一个线程B的`join()`方法后，会让当前线程A阻塞，直到线程B的逻辑执行完成，A线程才会解阻塞，然后继续执行自己的业务逻辑。

##  `join()`原理分析

`join()`有三个重载的方法，通过`Object`类中的`wait()、notify()/notifyAll()`方法实现的。

```java
public final synchronized void join(long millis)
    throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }
	// 当millis为0时，表示不限时长地等待
    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {	// 当millis不为0时，就需要进行超时时间的计算，然后让线程等待指定的时间
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}

public final synchronized void join(long millis, int nanos)
    throws InterruptedException {

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
            "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    join(millis);
}

public final void join() throws InterruptedException {
    join(0);
}
```

当调用`join()`方法中，会直接调用`join(0)`方法，`参数传0表示不限时长地等待`。

`join(long millis)`方法因为使用了`synchronized`关键字修饰，所以是一个同步方法，它锁的对象是`this`，也就是实例对象本身。在`join(long millis)`方法中调用了实例对象的`wait()`方法，而`notifyAll()`方法是在`jvm`中调用的。

# `sleep()`方法简介

当线程调用了`sleep`方法后，调用线程会暂时让出指定时间的执行权，即在这期间不参与CPU的调度，但是线程所拥有的监视器资源，比如锁还是持有不让出的。指定的时间到了后该函数会正常返回，线程就处于就绪状态，然后参与CPU的调度，获取到CPU资源后就可以继续运行了。

```java
public static void sleep(long millis, int nanos)
    throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException(
            "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    sleep(millis);
}

public static native void sleep(long millis) throws InterruptedException;
```

#  `yield()`方法

当一个线程调用`yield`方法时，当前线程会让出CPU使用权，然后就处于就绪状态，线程调度器会从线程就绪队列里面获取一个线程优先级最高的线程，也有可能会调度到刚刚让出CPU的那个线程来获取CPU执行权。

# `sleep()`和`yield()`的区别

当线程调用`sleep`方法时调用线程会被阻塞挂起指定的时间，在这期间线程调度器不会去调度该线程。

调用`yield()`方法时，线程只是让出自己剩余的时间片，并没有被阻塞挂起，而是处于就绪状态，线程调度器下一次调度就有可能调度当前线程执行。

# `sleep()`和`wait()`方法区别

`wait()`是用于线程间通信的，而`sleep()`是用于短时间暂停当前线程。

当一个线程调用`wait()`方法的时候，会释放它锁持有的对象的管程和锁，但是调用`sleep()`方法的时候，不会释放他所持有的管程。

- `wait`只能在同步（synchronize）环境中被调用，而`sleep`不需要
- 进入`wait`状态的线程能够被`notify`和`notifyAll`线程唤醒，但是进入`sleeping`状态的线程不能被`notify`方法唤醒。
- `wait`通常有条件地执行，线程会一直处于`wait`状态，直到某个条件变为真。但是`sleep`仅仅让你的线程进入睡眠状态。
- wait方法在进入wait状态的时候会释放对象的锁，但是sleep方法不会。